package com.yutkubeygo.watchit.exception;

//Kayıt var ama giriş yapan kullanıcıya ait değilse servis bunu fırlatır (GlobalExceptionHandler 403'e çevirir)
public class ForbiddenException extends RuntimeException {
}
