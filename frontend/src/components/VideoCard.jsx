function VideoCard({ video }) {
  // Eğer video verisi gelmezse boş dön (patlamayı önler)
  if (!video) return null;

  return (
    <div style={{ cursor: 'pointer', display: 'flex', flexDirection: 'column', gap: '10px' }}>
      
      {/* 1. Küçük Resim (Thumbnail) */}
      <div style={{ 
        width: '100%', 
        aspectRatio: '16/9', 
        backgroundColor: '#333', 
        borderRadius: '12px', 
        overflow: 'hidden',
        position: 'relative'
      }}>
        {video.thumbnailUrl ? (
          <img 
            src={video.thumbnailUrl} 
            alt={video.title} 
            style={{ width: '100%', height: '100%', objectFit: 'cover' }} 
          />
        ) : (
          <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#777' }}>
            Resim Yok
          </div>
        )}
        
        {/* Süre (Saniye olarak geldiği için ileride dakikaya çevirebilirsin, şimdilik sabit) */}
        {video.durationInSeconds && (
          <span style={{
            position: 'absolute', bottom: '8px', right: '8px', 
            backgroundColor: 'rgba(0,0,0,0.8)', color: '#fff', 
            padding: '2px 6px', borderRadius: '4px', fontSize: '12px', fontWeight: 'bold'
          }}>
            {Math.floor(video.durationInSeconds / 60)}:{(video.durationInSeconds % 60).toString().padStart(2, '0')}
          </span>
        )}
      </div>

      {/* 2. Video Bilgileri */}
      <div style={{ display: 'flex', gap: '12px', padding: '0 5px' }}>
        
        {/* Kanal Avatarı (Şimdilik yuvarlak bir yer tutucu) */}
        <div style={{ width: '36px', height: '36px', borderRadius: '50%', backgroundColor: '#555', flexShrink: 0 }}></div>

        {/* Başlık ve Detaylar */}
        <div style={{ display: 'flex', flexDirection: 'column' }}>
          
          <h4 style={{ 
            margin: 0, fontSize: '16px', color: '#fff', 
            display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' 
          }}>
            {video.title}
          </h4>
          
          {/* Kanal Adı: ManyToOne ilişkisi olduğu için "video.channel" bir objedir. 
              Soru işareti (?) Optional Chaining'dir; kanal yoksa kod patlamaz. */}
          <span style={{ fontSize: '14px', color: '#aaa', marginTop: '4px' }}>
            {video.channel?.name || 'Bilinmeyen Kanal'}
          </span>
          
          {/* Tarih: Backend'den gelen LocalDateTime formatını okunabilir TR tarihine çeviriyoruz */}
          <span style={{ fontSize: '14px', color: '#aaa' }}>
            10 B görüntülenme • {video.createdAt ? new Date(video.createdAt).toLocaleDateString('tr-TR') : 'Yeni'}
          </span>
          
        </div>
      </div>
    </div>
  );
}

export default VideoCard;