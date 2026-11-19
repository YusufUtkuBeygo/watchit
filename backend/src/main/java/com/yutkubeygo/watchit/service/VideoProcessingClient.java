package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.TranscodeResult;

//Videoyu oynatılabilir tek biçime çeviren servis. Testlerde sahtesi kullanılır.
public interface VideoProcessingClient {

    //Dönüşmüş dosyayı geçici bir dosyaya indirir. Başarısızsa (dosya video değil, servis yok) IllegalStateException.
    TranscodeResult transcode(String videoUrl);
}
