import { useState } from 'react';
import Categories from '../components/Categories';
import VideoGrid from '../components/VideoGrid';

function HomePage() {
  const [selectedCategory, setSelectedCategory] = useState(null);

  return (
    <>
      <Categories selectedCategory={selectedCategory} onSelectCategory={setSelectedCategory} />
      <VideoGrid selectedCategory={selectedCategory} />
    </>
  );
}

export default HomePage;
