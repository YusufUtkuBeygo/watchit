package com.yutkubeygo.watchit.dto;

import java.util.List;

//found: cevap videoya dayanıyor mu (değerlendirmenin "bulunamadı" ölçütü buna bakar).
//basis: cevabın kaynağı: VIDEO (yalnız video), MIXED (video + genel bilgi), GENERAL (videoda yok, genel bilgi).
//sources: cevabın dayandığı parçalar (dakikaya tıklayınca video oraya atlar); genel bilgi cevabında boş.
//score: anlamca benzerlik (0-1); parça zamana göre seçildiyse ("başında", "03:20'de") null.
public record QuestionResponseDto(boolean found, String answer, List<Source> sources, String basis) {

    public static final String VIDEO = "VIDEO";
    public static final String MIXED = "MIXED";
    public static final String GENERAL = "GENERAL";


    public record Source(double start, double end, String text, Double score) {}
}
