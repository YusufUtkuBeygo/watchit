package com.yutkubeygo.watchit.exception;

//E-postasını doğrulamamış kullanıcı yükleme/yorum gibi bir işe kalkıştı (403 + "EMAIL_NOT_VERIFIED").
//Düz 403'ten ayrı, çünkü frontend bunu görünce "e-postanı doğrula" penceresini açar.
public class EmailNotVerifiedException extends RuntimeException {
}
