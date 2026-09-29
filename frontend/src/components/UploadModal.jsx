import React, { useState, useRef } from 'react';
import { X, UploadCloud } from 'lucide-react';
import axios from 'axios';

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
      const response = await axios.post("http://localhost:8080/api/files/upload", formData);
      setPreviewUrl(response.data);
    } catch (error) {
      console.error("Yükleme hatası:", error);
      alert("Yükleme başarısız!");
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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm">
      <div className="w-full max-w-lg bg-zinc-900 rounded-2xl shadow-2xl overflow-hidden flex flex-col border border-zinc-700 mx-4">
        
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-zinc-700">
          <h2 className="text-xl font-semibold text-white">Video Yükle</h2>
          <button 
            onClick={handleClose}
            className="text-zinc-400 hover:text-white transition-colors p-1 rounded-full hover:bg-zinc-800"
          >
            <X size={24} />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 flex flex-col gap-6">
          
          {/* Dropzone */}
          <div 
            className={`relative flex flex-col items-center justify-center p-10 border-2 border-dashed rounded-xl cursor-pointer transition-all duration-200
              ${isDragging ? 'border-blue-500 bg-blue-500/10' : 'border-zinc-700 hover:border-zinc-500 hover:bg-zinc-800/50'}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
          >
            <input 
              type="file" 
              className="hidden" 
              ref={fileInputRef}
              onChange={handleFileSelect}
              accept="image/*,video/*"
            />
            <div className="w-16 h-16 bg-zinc-800 rounded-full flex items-center justify-center mb-4">
              <UploadCloud size={32} className={isDragging ? 'text-blue-500' : 'text-zinc-400'} />
            </div>
            <p className="text-white font-medium text-lg mb-1 text-center">
              {selectedFile ? selectedFile.name : "Dosya Seç veya Sürükle"}
            </p>
            {!selectedFile && (
              <p className="text-zinc-400 text-sm text-center">
                Yüklemek istediğiniz dosyayı buraya sürükleyin veya dosyalarınızdan seçin.
              </p>
            )}
          </div>

          {/* Upload Button */}
          {selectedFile && !previewUrl && (
            <button 
              onClick={handleUpload}
              disabled={isUploading}
              className="w-full py-3 bg-blue-600 hover:bg-blue-500 disabled:bg-blue-600/50 text-white font-semibold rounded-lg transition-colors flex items-center justify-center gap-2"
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
            <div className="mt-2 flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium text-emerald-400">Yükleme Başarılı!</span>
              </div>
              <div className="relative rounded-lg overflow-hidden border border-zinc-700 bg-zinc-950 flex items-center justify-center min-h-[200px]">
                <img 
                  src={previewUrl} 
                  alt="Preview" 
                  className="max-h-64 object-contain"
                />
              </div>
              <p className="text-xs text-zinc-500 font-mono break-all bg-zinc-950 p-2 rounded border border-zinc-800">
                {previewUrl}
              </p>
            </div>
          )}

        </div>
      </div>
    </div>
  );
}
