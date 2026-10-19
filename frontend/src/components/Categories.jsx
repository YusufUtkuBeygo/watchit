import { useState, useEffect } from 'react';
import { getAllCategories } from '../services/categoryService';
import './Categories.css';

//Kategori çipleri: "Tümü" + backend'deki kategoriler. Seçim HomePage'de (adreste) tutulur
function Categories({ selectedCategory, onSelectCategory }) {
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    getAllCategories()
      .then(setCategories)
      .catch((error) => console.error('Kategoriler getirilemedi:', error));
  }, []);

  return (
    <div className="categories-container" role="toolbar" aria-label="Kategoriler">
      <button
        className={`category-chip ${selectedCategory === null ? 'active' : ''}`}
        aria-pressed={selectedCategory === null}
        onClick={() => onSelectCategory(null)}
      >
        Tümü
      </button>

      {categories.map((cat) => (
        <button
          key={cat.id}
          className={`category-chip ${selectedCategory === cat.id ? 'active' : ''}`}
          aria-pressed={selectedCategory === cat.id}
          onClick={() => onSelectCategory(cat.id)}
        >
          {cat.name}
        </button>
      ))}
    </div>
  );
}

export default Categories;
