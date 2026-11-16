package com.yutkubeygo.watchit.service;

import com.yutkubeygo.watchit.dto.TranscriptionResult;

//Videoyu yazıya çeviren servis. Bugün Whisper konteyneri (WhisperTranscriptionClient);
//ileride hazır bir transkript servisine geçilirse yalnız bu arayüzün yeni bir uygulaması yazılır.
public interface TranscriptionClient {

    //Uzun sürebilir (dakikalar); hata olursa exception fırlatır
    TranscriptionResult transcribe(String videoUrl);
}
