package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.CategoryRequestDto;
import com.yutkubeygo.watchit.dto.CategoryResponseDto;
import com.yutkubeygo.watchit.entity.Category;
import com.yutkubeygo.watchit.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
//todo postgreyi dockera tasi , minioyu docker ile kaldir , thumbnailleri minio ile sotarageye at ,baska bir react projesi ile admin panel kaldir






@RestController
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService categoryService;
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public CategoryResponseDto createCategory(@RequestBody CategoryRequestDto categoryRequestDto)
    {
        return categoryService.createCategory(categoryRequestDto);
    }

    @GetMapping("/{id}")
    public CategoryResponseDto getCategory(@PathVariable Long id)
    {
        return categoryService.getCategoryById(id);
    }

    @GetMapping
    public List<CategoryResponseDto> getAllCategories()
    {
        return categoryService.getAllCategories();
    }

    @PutMapping("/{id}")
    public CategoryResponseDto updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryRequestDto categoryRequestDto
    )
    {
        return categoryService.updateCategory(id,categoryRequestDto);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id)
    {
        categoryService.deleteCategory(id);
    }
}
