package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.entity.Video;
import com.yutkubeygo.watchit.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;



//Paylaşım önizlemesi: WhatsApp, Twitter, Discord gibi uygulamaların link botları JavaScript çalıştırmaz; React sayfası
//onlara boş görünür ve linkin altında başlık/resim çıkmaz. nginx (frontend/nginx.conf) bot isteğini /watch/5'ten
//buraya yönlendirir; burası videonun başlığı, açıklaması ve kapağıyla küçük bir HTML döner (Open Graph etiketleri).
//İnsan buraya düşerse (bot sanıldıysa) sayfa onu hemen asıl izleme sayfasına gönderir.
@RestController
public class ShareController {

    private static final int MAX_DESCRIPTION = 200;

    private final VideoRepository videoRepository;
    private final String frontendUrl;

    public ShareController(VideoRepository videoRepository, @Value("${app.frontend-url}") String frontendUrl)
    {
        this.videoRepository = videoRepository;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping(value = "/og/watch/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> watchPreview(@PathVariable Long id)
    {
        Video video = videoRepository.findById(id).orElse(null);
        if(video == null)
            return ResponseEntity.notFound().build();

        String url = frontendUrl + "/watch/" + id;
        String title = video.getTitle() == null ? "WatchIt" : video.getTitle();
        String channel = video.getChannel() == null ? "" : video.getChannel().getChannelName();
        String description = shorten(video.getDescription() == null || video.getDescription().isBlank()
                ? channel + " kanalında izle" : video.getDescription());

        //Her değer htmlEscape'ten geçer: başlıkta "<script>" yazan biri bot sayfasına kod sokamasın
        StringBuilder html = new StringBuilder()
                .append("<!doctype html><html lang=\"tr\"><head><meta charset=\"utf-8\">")
                .append("<title>").append(htmlEscape(title)).append(" - WatchIt</title>")
                .append(meta("description", description))
                .append(og("og:site_name", "WatchIt"))
                .append(og("og:type", "video.other"))
                .append(og("og:title", title))
                .append(og("og:description", description))
                .append(og("og:url", url));
        if(video.getThumbnailUrl() != null && !video.getThumbnailUrl().isBlank())
        {
            html.append(og("og:image", video.getThumbnailUrl()))
                .append(meta("twitter:card", "summary_large_image"));
        }
        else
        {
            html.append(meta("twitter:card", "summary"));
        }
        html.append(meta("twitter:title", title))
            .append(meta("twitter:description", description))
            .append("<link rel=\"canonical\" href=\"").append(htmlEscape(url)).append("\">")
            .append("<meta http-equiv=\"refresh\" content=\"0;url=").append(htmlEscape(url)).append("\">")
            .append("</head><body><a href=\"").append(htmlEscape(url)).append("\">").append(htmlEscape(title)).append("</a></body></html>");

        //Botlar aynı linki tekrar tekrar sorar: 10 dakika önbellekte tutabilirler
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic()).body(html.toString());
    }

    //Yalnız HTML'de anlamı olan karakterler (<, >, &, ", ') kaçırılır; Türkçe harfler olduğu gibi kalır
    //(htmlEscape'in tek parametreli hâli "ç"yi &ccedil; yapıyordu)
    private static String htmlEscape(String text)
    {
        return org.springframework.web.util.HtmlUtils.htmlEscape(text, "UTF-8");
    }

    private static String meta(String name, String content)
    {
        return "<meta name=\"" + name + "\" content=\"" + htmlEscape(content) + "\">";
    }

    private static String og(String property, String content)
    {
        return "<meta property=\"" + property + "\" content=\"" + htmlEscape(content) + "\">";
    }

    private static String shorten(String text)
    {
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= MAX_DESCRIPTION ? oneLine : oneLine.substring(0, MAX_DESCRIPTION - 1) + "…";
    }
}
