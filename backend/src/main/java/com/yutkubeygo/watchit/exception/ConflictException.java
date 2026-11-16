package com.yutkubeygo.watchit.exception;

//İstek, kaydın şu anki durumuyla çakışıyorsa servis bunu fırlatır (GlobalExceptionHandler 409'a çevirir).
//Örnek: transkript zaten hazırlanırken yeniden istenmesi.
public class ConflictException extends RuntimeException {
}
