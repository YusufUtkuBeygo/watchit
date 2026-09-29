import React, { useState } from 'react';
import './App.css';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import Categories from './components/Categories';
import VideoGrid from './components/VideoGrid';

function App() {
  const [selectedCategory, setSelectedCategory] = useState(null);

  return (
    <div className="app-container">
      <Navbar />
      <div className="main-content">
        <Sidebar />
        <main className="content-area" style={{ marginLeft: 'var(--sidebar-width)' }}>
          
          <Categories selectedCategory={selectedCategory} onSelectCategory={setSelectedCategory} />
          <VideoGrid selectedCategory={selectedCategory} />
        </main>
      </div>
    </div>
  );
}

export default App;