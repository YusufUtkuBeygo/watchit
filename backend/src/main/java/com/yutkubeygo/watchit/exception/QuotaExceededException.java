package com.yutkubeygo.watchit.exception;

//Yükleme kotası doldu (413 + {"error":"QUOTA_EXCEEDED","limit":"TOTAL"|"DAILY"}).
//Frontend "limit"e bakarak "depolama alanın doldu" ya da "bugünlük yükleme hakkın doldu" yazar
public class QuotaExceededException extends RuntimeException {

    public enum Limit { TOTAL, DAILY }

    private final Limit limit;

    public QuotaExceededException(Limit limit)
    {
        this.limit = limit;
    }

    public Limit getLimit()
    {
        return limit;
    }
}
