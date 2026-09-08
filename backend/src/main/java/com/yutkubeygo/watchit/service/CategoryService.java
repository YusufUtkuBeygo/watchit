package com.yutkubeygo.watchit.service;


import com.yutkubeygo.watchit.dto.CategoryResponseDto;
import com.yutkubeygo.watchit.dto.CategoryRequestDto;
import com.yutkubeygo.watchit.entity.Category;
import com.yutkubeygo.watchit.mapper.CategoryMapper;
import com.yutkubeygo.watchit.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    public CategoryResponseDto createCategory(CategoryRequestDto categoryRequestDto)
    {
        Category category=categoryMapper.toEntity(categoryRequestDto);
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    public CategoryResponseDto getCategoryById(Long id)
    {
        Category category=categoryRepository.findById(id).orElse(null);
        if(category==null)
            return null;

        return categoryMapper.toDto(category);
    }

    public List<CategoryResponseDto> getAllCategories()
    {
        return categoryMapper.toDtoList(categoryRepository.findAll());
    }

    public CategoryResponseDto updateCategory(Long id,CategoryRequestDto categoryRequestDto)
    {
        Category category=categoryRepository.findById(id).orElse(null);
        if(category==null)
            return null;

        category.setName(categoryRequestDto.getName());
        return categoryMapper.toDto(categoryRepository.save(category));

    }

    public void deleteCategory(Long id)
    {
        categoryRepository.deleteById(id);
    }


}
