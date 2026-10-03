package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.CategoryRequestDto;
import com.yutkubeygo.watchit.dto.CategoryResponseDto;
import com.yutkubeygo.watchit.entity.Category;
import com.yutkubeygo.watchit.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<CategoryResponseDto> createCategory(@Valid @RequestBody CategoryRequestDto categoryRequestDto)
    {
        CategoryResponseDto category = categoryService.createCategory(categoryRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getCategory(@PathVariable Long id)
    {
        CategoryResponseDto category = categoryService.getCategoryById(id);

        if(category == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(category);
    }

    @GetMapping
    public List<CategoryResponseDto> getAllCategories()
    {
        return categoryService.getAllCategories();
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDto categoryRequestDto
    )
    {
        CategoryResponseDto category = categoryService.updateCategory(id,categoryRequestDto);

        if(category == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id)
    {
        if(!categoryService.deleteCategory(id))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
