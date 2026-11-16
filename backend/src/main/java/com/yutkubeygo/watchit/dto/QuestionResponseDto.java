package com.yutkubeygo.watchit.dto;

import java.util.List;

//found: cevap videoda bulundu mu. sources: cevabın dayandığı parçalar (dakikaya tıklayınca video oraya atlar).
//score: anlamca benzerlik (0-1); parça zamana göre seçildiyse ("başında", "03:20'de") null.
public record QuestionResponseDto(boolean found, String answer, List<Source> sources) {

    public record Source(double start, double end, String text, Double score) {}
}
