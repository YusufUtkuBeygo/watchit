package com.yutkubeygo.watchit.exception;

//Çok fazla yanlış şifre denemesinden sonra servis bunu fırlatır (GlobalExceptionHandler 429'a çevirir)
public class TooManyRequestsException extends RuntimeException {
}
