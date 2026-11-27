package com.yutkubeygo.watchit.util;

import com.yutkubeygo.watchit.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagingTest {

    @Test
    void defaultsToFirstPageOf24()
    {
        Pageable p = Paging.of(null, null);
        assertThat(p.getPageNumber()).isZero();
        assertThat(p.getPageSize()).isEqualTo(24);
    }

    @Test
    void rejectsNegativePageAndOutOfRangeSize()
    {
        assertThatThrownBy(() -> Paging.of(-1, 10)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> Paging.of(0, 0)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> Paging.of(0, 51)).isInstanceOf(BadRequestException.class);
        assertThat(Paging.of(3, 50).getPageSize()).isEqualTo(50);
    }
}
