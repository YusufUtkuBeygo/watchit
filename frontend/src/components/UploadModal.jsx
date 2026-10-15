import { useState, useRef } from 'react';
import { X, UploadCloud } from 'lucide-react';
import api from '../services/api';
import './UploadModal.css';

export default function UploadModal({ isOpen, onClose }) {
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);

  const fileInputRef = useRef(null);

  if (!isOpen) return null;

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      setSelectedFile(e.dataTransfer.files[0]);
      setPreviewUrl(null); // Reset previous upload preview
    }
  };

  const handleFileSelect = (event) => {
    if (event.target.files && event.target.files.length > 0) {
      setSelectedFile(event.target.files[0]);
      setPreviewUrl(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return alert("Lütfen bir dosya seçin.");

    setIsUploading(true);
    const formData = new FormData();
    formData.append("file", selectedFile);

    try {
      //Backend'deki adres /api/files/upload. Proxy baştaki /api'yi sildiği için
      //burada /api iki kez geçer: biri proxy için (baseURL), biri backend adresinin kendisi
      const response = await api.post("/api/files/upload", formData);
      setPreviewUrl(response.data);
    } catch (error) {
      console.error("Yükleme hatası:", error);
      if (error.response && error.response.status === 401) {
        alert("Oturumun süresi dolmuş, tekrar giriş yap.");
      } else {
        alert("Yükleme başarısız!");
      }
    } finally {
      setIsUploading(false);
    }
  };

  const handleClose = () => {
    setSelectedFile(null);
    setPreviewUrl(null);
    setIsDragging(false);
    onClose();
  };

  return (
    <div className="upload-overlay">
      <div className="upload-modal">

        {/* Header */}
        <div className="upload-header">
          <h2>Video Yükle</h2>
          <button className="icon-btn" onClick={handleClose} aria-label="Kapat">
            <X size={22} />
          </button>
        </div>

        {/* Content */}
        <div className="upload-content">

          {/* Dropzone */}
          <div
            className={`upload-dropzone ${isDragging ? 'dragging' : ''}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
          >
            <input
              type="file"
              className="upload-file-input"
              ref={fileInputRef}
              onChange={handleFileSelect}
              accept="image/*,video/*"
            />
            <div className="upload-icon">
              <UploadCloud size={32} />
            </div>
            <p className="upload-title">
              {selectedFile ? selectedFile.name : "Dosya Seç veya Sürükle"}
            </p>
            {!selectedFile && (
              <p className="upload-hint">
                Yüklemek istediğiniz dosyayı buraya sürükleyin veya dosyalarınızdan seçin.
              </p>
            )}
          </div>

          {/* Upload Button */}
          {selectedFile && !previewUrl && (
            <button
              onClick={handleUpload}
              disabled={isUploading}
              className="upload-submit"
            >
              {isUploading ? (
                <>Yükleniyor...</>
              ) : (
                <>Sunucuya Gönder</>
              )}
            </button>
          )}

          {/* Preview */}
          {previewUrl && (
            <div className="upload-result">
              <span className="upload-success">Yükleme Başarılı!</span>
              <div className="upload-preview">
                <img src={previewUrl} alt="Preview" />
              </div>
              <p className="upload-url">
                {previewUrl}
              </p>
            </div>
          )}

        </div>
      </div>
    </div>
  );
}
