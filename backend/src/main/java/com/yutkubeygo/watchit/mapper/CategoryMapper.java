package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.CategoryResponseDto;
import com.yutkubeygo.watchit.dto.CategoryRequestDto;
import com.yutkubeygo.watchit.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.BeanMapping;

import java.util.List;
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    public CategoryResponseDto toDto(Category category);
    public List<CategoryResponseDto> toDtoList(List<Category> categories);
    //Request DTO'sunda olmayan alanlar (id, ilişkiler, tarihler) serviste ya da veritabanında dolar
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    public Category toEntity(CategoryRequestDto categoryRequestDto);
}
