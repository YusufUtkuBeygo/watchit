package com.yutkubeygo.watchit.dto;

import org.springframework.data.domain.Page;

import java.util.List;

//Sayfalı liste cevabı: bu sayfadaki kayıtlar + "sonrası var mı".
//Neden: listeler eskiden bütün kayıtları tek cevapta veriyordu; 5.000 videoda her açılışta binlercesi inerdi.
//Frontend aşağı kaydırdıkça page=1, 2... ister; hasNext false olunca durur.
public record PageResponseDto<T>(List<T> items, int page, int size, boolean hasNext, long total)
{
    //Spring Data'nın Page nesnesinden, kayıtları DTO'ya çevrilmiş hâliyle
    public static <T> PageResponseDto<T> of(Page<?> page, List<T> items)
    {
        return new PageResponseDto<>(items, page.getNumber(), page.getSize(), page.hasNext(), page.getTotalElements());
    }
}
