package com.yutkubeygo.watchit.mapper;

import com.yutkubeygo.watchit.dto.CategoryResponseDto;
import com.yutkubeygo.watchit.dto.CategoryRequestDto;
import com.yutkubeygo.watchit.entity.Category;
import org.mapstruct.Mapper;

import java.util.List;
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    public CategoryResponseDto toDto(Category category);
    public List<CategoryResponseDto> toDtoList(List<Category> categories);
    public Category toEntity(CategoryRequestDto categoryRequestDto);
}
