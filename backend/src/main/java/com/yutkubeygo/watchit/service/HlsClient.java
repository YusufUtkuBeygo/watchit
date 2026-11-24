package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.HlsResult;

//Videoyu çoklu kalite HLS'ye çeviren servis. Testlerde sahtesi kullanılır.
public interface HlsClient {

    //HLS paketini (zip) geçici dosyaya indirir. Başarısızsa (dosya video değil, servis yok) IllegalStateException.
    HlsResult createHls(String videoUrl);
}
