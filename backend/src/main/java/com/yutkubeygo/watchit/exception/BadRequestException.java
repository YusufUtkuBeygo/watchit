package com.yutkubeygo.watchit.exception;

//Servis "istekteki veri kurala uymuyor" dediğinde fırlatır; GlobalExceptionHandler gövdesiz 400 döner.
//@Size gibi DTO kurallarıyla anlatılamayan kontroller için (ör. adres bizim kovamızdan değil)
public class BadRequestException extends RuntimeException {
}
