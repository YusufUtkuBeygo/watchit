package com.yutkubeygo.watchit.dto;

import java.util.List;

//found: cevap videoda bulundu mu. sources: cevabın dayandığı parçalar (dakikaya tıklayınca video oraya atlar).
public record QuestionResponseDto(boolean found, String answer, List<Source> sources) {

    public record Source(double start, double end, String text, double score) {}
}
