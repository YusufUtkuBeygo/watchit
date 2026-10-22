package com.yutkubeygo.watchit.repository;

import com.yutkubeygo.watchit.entity.Category;
import com.yutkubeygo.watchit.entity.Channel;
import com.yutkubeygo.watchit.entity.User;
import com.yutkubeygo.watchit.entity.Video;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

//VideoRepository.searchByTitle gerçek PostgreSQL'e karşı (unaccent, LIKE kaçırma, sıralama).
//@Transactional: her test sonunda eklenen kullanıcı/kanal/video geri alınır, veritabanında iz kalmaz.
@SpringBootTest
@Transactional
class VideoSearchQueryTest {

    @Autowired private VideoRepository videoRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private CategoryRepository categoryRepository;

    private Channel channel;
    private Category category;

    @BeforeEach
    void setUp()
    {
        User user = new User();
        user.setUsername("arama_testi_kullanici");
        user.setEmail("arama_testi@test.local");
        user.setPassword("x");
        userRepository.saveAndFlush(user);

        channel = new Channel();
        channel.setChannelName("Arama testi kanalı");
        channel.setHandle("arama_testi_kanali");
        channel.setOwner(user);
        channelRepository.saveAndFlush(channel);

        category = categoryRepository.findAll().getFirst();

        save("QQX İstanbul Gezisi");
        save("QQX ışık ve gölge");
        save("QQX %100 Kod");
        save("QQX a_b deneme");
        save("QQX axb deneme");
    }

    private void save(String title)
    {
        Video video = new Video();
        video.setTitle(title);
        video.setChannel(channel);
        video.setCategory(category);
        video.setDurationInSeconds(10);
        videoRepository.saveAndFlush(video);
    }

    private List<String> titles(String text)
    {
        return videoRepository.searchByTitle(text).stream().map(Video::getTitle).toList();
    }

    @Test
    void dottedCapitalIMatchesPlainI()
    {
        assertThat(titles("istanbul")).containsExactly("QQX İstanbul Gezisi");
        assertThat(titles("ISTANBUL")).containsExactly("QQX İstanbul Gezisi");
        assertThat(titles("İSTANBUL")).containsExactly("QQX İstanbul Gezisi");
    }

    @Test
    void turkishLettersMatchWithoutThem()
    {
        assertThat(titles("isik")).containsExactly("QQX ışık ve gölge");
        assertThat(titles("IŞIK")).containsExactly("QQX ışık ve gölge");
        assertThat(titles("golge")).containsExactly("QQX ışık ve gölge");
    }

    @Test
    void escapedWildcardsAreLiteral()
    {
        //Servis kaçırmayı yapar; burada kaçırılmış hâli verilir
        assertThat(titles("\\%100")).containsExactly("QQX %100 Kod");
        assertThat(titles("a\\_b")).containsExactly("QQX a_b deneme");
    }

    @Test
    void noMatchGivesEmptyList()
    {
        assertThat(titles("qqx-olmayan-kelime")).isEmpty();
    }
}
