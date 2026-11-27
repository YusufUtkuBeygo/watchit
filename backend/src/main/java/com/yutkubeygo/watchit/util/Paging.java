package com.yutkubeygo.watchit.util;

import com.yutkubeygo.watchit.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

//?page=0&size=24 parametrelerini doğrulayıp Pageable'a çevirir (bütün sayfalı uçlar aynı kuralları kullansın diye tek yerde)
public final class Paging {

    public static final int DEFAULT_SIZE = 24;
    //Üst sınır: biri size=1000000 yazıp sayfalamayı boşa çıkarmasın
    public static final int MAX_SIZE = 50;

    private Paging() {}

    //page boşsa 0, size boşsa 24; eksi sayfa ya da 1-50 dışı boyut 400
    public static Pageable of(Integer page, Integer size, Sort sort)
    {
        int p = page == null ? 0 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        if(p < 0 || s < 1 || s > MAX_SIZE)
            throw new BadRequestException();
        return PageRequest.of(p, s, sort);
    }

    public static Pageable of(Integer page, Integer size)
    {
        return of(page, size, Sort.unsorted());
    }
}
