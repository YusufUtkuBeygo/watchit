import React, { useState, useEffect } from 'react';
import VideoCard from './VideoCard';
import './VideoGrid.css';
import { getAllVideos, getVideosByCategoryId } from '../services/videoService';

function VideoGrid({selectedCategory}){

  //memorybox where we kept the videos
  const[videos,setVideos] = useState([]);
  
  //when page opens first time and whe choosen category informs it runs
  useEffect(() => {
    loadVideos();
  },[selectedCategory]);

  async function loadVideos(){

    try{
      let data;
      //if a category choosen and its not all get as a filter
      if(selectedCategory&&selectedCategory!=='all')
      {
        data = await getVideosByCategoryId(selectedCategory);
      }
      else{
        //if all videos choosen and not yet made a decision get all 
        data = await getAllVideos();
      }

      setVideos(data);

    }
    catch(error){
      console.error("An error occured while loading videos:",error);
    }

    
  }
  
return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))',
      gap: '20px',
      padding: '20px 0'
    }}>
      {/* Eğer backend'den hiç video dönmediyse mesaj göster */}
      {videos.length === 0 ? (
        <p style={{ color: '#aaa', fontSize: '16px' }}>Bu kategoride henüz video bulunmuyor.</p>
      ) : (
        /* Gelen her bir video nesnesi için ekrana bir VideoCard çiz (Java ForEach mantığı) */
        videos.map((video) => (
          <VideoCard key={video.id} video={video} />
        ))
      )}
    </div>
  );
  
}

export default VideoGrid;
