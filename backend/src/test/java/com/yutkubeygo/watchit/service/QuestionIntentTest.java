package com.yutkubeygo.watchit.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

//QuestionIntent: soru tipini Türkçe ve İngilizce ifadelerden ayırır.
class QuestionIntentTest {

    @ParameterizedTest
    @ValueSource(strings = {"Filmin başında ne oluyor?", "Başta ne söylüyor?", "Videonun başlangıcında ne var?",
            "En başta kim konuşuyor?", "What happens at the beginning?", "How does the video start? What's in the opening"})
    void beginning(String question)
    {
        assertThat(QuestionIntent.of(question).type()).isEqualTo(QuestionIntent.Type.BEGINNING);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Filmin sonunda ne oluyor?", "En son ne diyor?", "Finalde kim kazanıyor?", "What happens at the end?", "How is the ending?"})
    void end(String question)
    {
        assertThat(QuestionIntent.of(question).type()).isEqualTo(QuestionIntent.Type.END);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "03:20'de ne oluyor?|200",
            "1:05:30'da ne diyor?|3930",
            "5. dakikada ne anlatıyor?|270",
            "5 dakikada ne oluyor?|270",
            "2'nci dakikada ne var?|90",
            "What happens at minute 3?|150",
            "dakika 4'te ne oluyor?|210"})
    void atTime(String question, double seconds)
    {
        QuestionIntent intent = QuestionIntent.of(question);
        assertThat(intent.type()).isEqualTo(QuestionIntent.Type.AT_TIME);
        assertThat(intent.seconds()).isEqualTo(seconds);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bu video ne anlatıyor?", "Videoyu özetler misin?", "Kısa bir özet ver", "Video ne hakkında?",
            "Konusu ne?", "Özetle", "What is this video about?", "Can you summarize it?", "TL;DR"})
    void summary(String question)
    {
        assertThat(QuestionIntent.of(question).type()).isEqualTo(QuestionIntent.Type.SUMMARY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Robotun eli neden sorun oluyor?", "Why are Celia and Tom arguing?", "Soğan konuyor mu?",
            "Başarılı olmak için ne yapmalı?", "Sonuç olarak ne öneriyor?", "İlk adım ne?"})
    void contentQuestionsStayContent(String question)
    {
        //"Başarılı" "baş" ile, "Sonuç" "son" ile başlıyor ama zaman sorusu değil
        assertThat(QuestionIntent.of(question).type()).isEqualTo(QuestionIntent.Type.CONTENT);
    }
}
