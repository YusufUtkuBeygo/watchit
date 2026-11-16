package com.yutkubeygo.watchit.exception;

//Dış servis (Gemini) şu an cevap veremiyor: kota doldu, bağlantı yok... (GlobalExceptionHandler 503'e çevirir)
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(Throwable cause)
    {
        super(cause);
    }
}
