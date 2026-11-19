package com.yutkubeygo.watchit.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//Python medya servisinin hata cevabı: {"detail": "..."}. Kullanıcıya ham HTTP cevabı değil, yalnız açıklama gösterilir.
final class MediaServiceError {

    private static final Pattern DETAIL = Pattern.compile("\"detail\"\\s*:\\s*\"([^\"]*)\"");

    private MediaServiceError() {}

    static String message(String body, int status)
    {
        Matcher m = DETAIL.matcher(body == null ? "" : body);
        return m.find() ? m.group(1) : "Medya servisi hata verdi (" + status + ").";
    }
}
