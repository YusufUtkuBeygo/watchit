package com.yutkubeygo.watchit.service;


import com.yutkubeygo.watchit.dto.CategoryResponseDto;
import com.yutkubeygo.watchit.dto.CategoryRequestDto;
import com.yutkubeygo.watchit.entity.Category;
import com.yutkubeygo.watchit.mapper.CategoryMapper;
import com.yutkubeygo.watchit.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Sort;

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
        //Sıra sabit olsun (id'ye göre): sırasız sorguda güncellenen satır listenin sonuna kayıyordu
        return categoryMapper.toDtoList(categoryRepository.findAll(Sort.by("id")));
    }

    public CategoryResponseDto updateCategory(Long id,CategoryRequestDto categoryRequestDto)
    {
        Category category=categoryRepository.findById(id).orElse(null);
        if(category==null)
            return null;

        category.setName(categoryRequestDto.getName());
        return categoryMapper.toDto(categoryRepository.save(category));

    }

    //Silinecek kayıt yoksa false, silindiyse true döner (controller 404/204 kararını buna göre verir)
    public boolean deleteCategory(Long id)
    {
        if(!categoryRepository.existsById(id))
            return false;

        categoryRepository.deleteById(id);
        return true;
    }


}
