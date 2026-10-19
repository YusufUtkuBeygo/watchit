import { useSearchParams } from 'react-router-dom';
import Categories from '../components/Categories';
import VideoGrid from '../components/VideoGrid';

//Ana sayfa: kategori çipleri + videolar
//Seçili kategori adreste durur (/?category=2): sol menüdeki kategori linkleri de buraya gelir, sayfa yenilenince seçim kalır
function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedCategory = searchParams.get('category') ? Number(searchParams.get('category')) : null;

  const selectCategory = (categoryId) => {
    setSearchParams(categoryId ? { category: String(categoryId) } : {});
  };

  return (
    <>
      <Categories selectedCategory={selectedCategory} onSelectCategory={selectCategory} />
      <VideoGrid selectedCategory={selectedCategory} />
    </>
  );
}

export default HomePage;
