package com.yutkubeygo.watchit.controller;

import com.yutkubeygo.watchit.dto.CourseResponseDto;
import com.yutkubeygo.watchit.service.CourseService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//Kurslar sayfası. Girişsiz de açık (SecurityConfig); giriş varsa her kursta ilerlemem de gelir.
@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService)
    {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseResponseDto> getCourses(@AuthenticationPrincipal Long userId)
    {
        return courseService.getCourses(userId);
    }
}
