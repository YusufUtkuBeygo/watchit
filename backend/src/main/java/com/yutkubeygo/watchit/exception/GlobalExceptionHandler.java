package com.yutkubeygo.watchit.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    //Veritabanı kaydı reddetti. Hangi kodu döneceğimizi veritabanının hata kodu (SQLState) söyler:
    //"22..." = veri hatası (metin sütuna sığmıyor, sayı taşıyor, NUL karakteri): girdi bozuk -> 400
    //diğerleri ("23...": benzersizlik, yabancı anahtar) = var olan kayıtlarla çakışma -> 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Void> handleDataIntegrity(DataIntegrityViolationException ex)
    {
        String sqlState = findSqlState(ex);
        if(sqlState != null && sqlState.startsWith("22"))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    //Hata iç içe sarılı gelir (Spring -> Hibernate -> PostgreSQL); zinciri dolaşıp SQLException'ın kodunu bulur
    private String findSqlState(Throwable ex)
    {
        for(Throwable cause = ex; cause != null; cause = cause.getCause())
        {
            if(cause instanceof SQLException sqlException && sqlException.getSQLState() != null)
                return sqlException.getSQLState();
        }
        return null;
    }

    //Servis "istekteki veri kurala uymuyor" dediğinde gövdesiz 400 döner
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Void> handleBadRequest()
    {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    //Servis "bu kayıt senin değil" dediğinde gövdesiz 403 döner
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Void> handleForbidden()
    {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
