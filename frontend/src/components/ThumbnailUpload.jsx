import { useState } from 'react';
import axios from 'axios';

export default function ThumbnailUpload() {
  // 1. HAFIZA (State) ALANLARI
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);

  // 2. KULLANICI BİLGİSAYARINDAN DOSYA SEÇTİĞİNDE ÇALIŞAN KISIM
  const handleFileSelect = (event) => {
    const file = event.target.files[0];
    setSelectedFile(file); // Seçilen dosyayı hafızaya atıyoruz
  };

  // 3. YÜKLE BUTONUNA BASILDIĞINDA ÇALIŞAN KISIM
  const handleUpload = async () => {
    if (!selectedFile) return alert("Önce dosya seç cano!");

    // Postman'deki "Body -> form-data" işleminin aynısı
    const formData = new FormData();
    formData.append("file", selectedFile);

    try {
      // Postman'deki SEND butonunun koddaki karşılığı (Backend 8080 portuna istek atıyoruz)
      const response = await axios.post("http://localhost:8080/api/files/upload", formData);
      
      // Backend'den dönen MinIO URL'sini alıp hafızaya kaydediyoruz
      setPreviewUrl(response.data);
      console.log("Yükleme başarılı, MinIO URL:", response.data);
    } catch (error) {
      console.error("Sunucuya bağlanırken hata oluştu:", error);
      alert("Yükleme başarısız! Konsolu kontrol et.");
    }
  };

  return (
    <div className="p-6 bg-zinc-900 text-white rounded-xl flex flex-col gap-4 max-w-md shadow-xl border border-zinc-800">
      <h2 className="text-xl font-bold">Kapak Fotoğrafı Yükleme Paneli</h2>
      
      {/* Dosya Seçme Inputu */}
      <input 
        type="file" 
        onChange={handleFileSelect}
        className="file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-blue-600 file:text-white hover:file:bg-blue-700 cursor-pointer text-sm text-zinc-400"
      />
      
      {/* Yükle Butonu */}
      <button 
        onClick={handleUpload}
        className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-lg font-semibold transition duration-200"
      >
        Sunucuya (MinIO'ya) Gönder
      </button>

      {/* Eğer sunucudan URL döndüyse resmi ekranda göster */}
      {previewUrl && (
        <div className="mt-4 flex flex-col gap-2">
          <p className="text-xs text-emerald-400 font-mono">Yüklendi: {previewUrl}</p>
          <img src={previewUrl} alt="Yüklenen Kapak" className="w-full h-auto rounded-lg shadow-md border border-zinc-700" />
        </div>
      )}
    </div>
  );
}
