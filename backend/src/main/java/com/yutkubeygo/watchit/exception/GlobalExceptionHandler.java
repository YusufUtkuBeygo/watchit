package com.yutkubeygo.watchit.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Void> handleDataIntegrity()
    {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> handleValidation(MethodArgumentNotValidException ex)
    {
        //1)Bos bir map olustur
        Map<String,String> errors = new HashMap<>();

        //2)Hata listesine bak ve her hatayi errors mapine ekle

        for(FieldError error: ex.getBindingResult().getFieldErrors())
        {
            errors.put(error.getField(),error.getDefaultMessage());
        }

        //3)Hata ve karsilginda sebebini barinidiran errors mapini return eder

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);

    }




}
