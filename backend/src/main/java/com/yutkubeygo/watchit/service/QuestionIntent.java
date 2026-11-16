package com.yutkubeygo.watchit.service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Sorunun tipi. Vektör araması "anlam"a göre bulur; "başında", "sonunda", "5. dakikada" bir anlam değil, konumdur.
//"Video ne anlatıyor?" ise tek bir parçada değil bütün videoda. Bu tipleri basit kurallarla ayırıp
//parçaları farklı seçiyoruz (Gemini'ye fazladan istek yok). Türkçe ve İngilizce.
public record QuestionIntent(Type type, Double seconds) {

    public enum Type {
        BEGINNING,  //videonun başı
        END,        //videonun sonu
        AT_TIME,    //belli bir an ("03:20", "5. dakika")
        SUMMARY,    //bütün video ("özetle", "ne anlatıyor")
        CONTENT     //geri kalan her şey: anlama göre arama (RAG)
    }

    //Unicode: \b ve \w Türkçe harfleri de kelime harfi saysın (yoksa "özet"in başında kelime sınırı bulunmaz)
    private static final int U = Pattern.UNICODE_CHARACTER_CLASS;

    //"03:20", "1:05:30"
    private static final Pattern CLOCK = Pattern.compile("\\b(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2})\\b", U);
    //"5. dakika", "5 dakika", "5'inci dakika", "dakika 5", "minute 5", "5 min"
    private static final Pattern MINUTE = Pattern.compile(
            "\\b(\\d{1,3})\\s*(?:\\.|'?(?:inci|ıncı|uncu|üncü|nci|ncı|ncu|ncü))?\\s*(?:dakika|dk\\b|minute|min\\b)"
            + "|\\b(?:dakika|minute)\\s*(\\d{1,3})\\b", U);
    private static final Pattern BEGINNING = Pattern.compile(
            "\\b(?:başında|başta|başlangıçta|başlangıcında|en başta|ilk sahne|ilk dakika|açılışta|girişte|"
            + "beginning|start of|at the start|first scene|opening|intro)", U);
    private static final Pattern END = Pattern.compile(
            "\\b(?:sonunda|sonda|en sonda|en son|finalde|finali|bitişte|son sahne|son dakika|"
            + "ending|end of|at the end|final scene|last scene|finale)", U);
    private static final Pattern SUMMARY = Pattern.compile(
            "\\b(?:özet|özetle|özetler|ne anlatıyor|ne anlatılıyor|neyi anlatıyor|ne hakkında|konusu ne|ana fikri|genel olarak|"
            + "summary|summarize|summarise|what is (?:this|the) video about|what is it about|tl;?dr|overview)", U);

    public static QuestionIntent of(String question)
    {
        //Türkçe küçük harf: "İ" -> "i" doğru dönsün (Locale.ROOT'ta "i̇" olurdu)
        String q = question.toLowerCase(Locale.forLanguageTag("tr"));

        Matcher clock = CLOCK.matcher(q);
        if(clock.find())
        {
            int h = clock.group(1) == null ? 0 : Integer.parseInt(clock.group(1));
            return new QuestionIntent(Type.AT_TIME, h * 3600.0 + Integer.parseInt(clock.group(2)) * 60 + Integer.parseInt(clock.group(3)));
        }
        Matcher minute = MINUTE.matcher(q);
        if(minute.find())
        {
            String m = minute.group(1) != null ? minute.group(1) : minute.group(2);
            //"5. dakika" = 4:00-5:00 arası; ortasını alıyoruz
            return new QuestionIntent(Type.AT_TIME, Math.max(0, Integer.parseInt(m) - 0.5) * 60);
        }
        if(BEGINNING.matcher(q).find())
            return new QuestionIntent(Type.BEGINNING, null);
        if(END.matcher(q).find())
            return new QuestionIntent(Type.END, null);
        if(SUMMARY.matcher(q).find())
            return new QuestionIntent(Type.SUMMARY, null);
        return new QuestionIntent(Type.CONTENT, null);
    }
}
