import React, { useState, useEffect } from 'react';
import { getAllCategories } from '../services/categoryService';

function Categories({ selectedCategory, onSelectCategory }) {
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    uploadCategories();
  }, []);

  async function uploadCategories() {
    try {
      const data = await getAllCategories();
      setCategories(data);
    } catch (error) {
      console.error("Kategoriler getirilemedi:", error);
    }
  }

  return (
    <div className="categories-container" style={{
      display: 'flex',
      overflowX: 'auto',
      padding: '12px 24px',
      gap: '12px',
      scrollbarWidth: 'none',
      msOverflowStyle: 'none',
    }}>
      <style>{`
        .categories-container::-webkit-scrollbar {
          display: none;
        }
        .category-chip {
          padding: 6px 12px;
          border-radius: 8px;
          background-color: var(--bg-secondary, #272727);
          color: var(--text-primary, #fff);
          white-space: nowrap;
          cursor: pointer;
          border: none;
          font-size: 14px;
          font-weight: 500;
          transition: background-color 0.2s;
        }
        .category-chip:hover {
          background-color: var(--bg-secondary-hover, #3f3f3f);
        }
        .category-chip.active {
          background-color: var(--text-primary, #fff);
          color: var(--bg-primary, #0f0f0f);
        }
      `}</style>

      <button
        className={`category-chip ${selectedCategory === null ? 'active' : ''}`}
        onClick={() => onSelectCategory(null)}
      >
        Tümü
      </button>

      {categories.map((cat) => (
        <button
          key={cat.id}
          className={`category-chip ${selectedCategory === cat.id ? 'active' : ''}`}
          onClick={() => onSelectCategory(cat.id)}
        >
          {cat.name}
        </button>
      ))}
    </div>
  );
}

export default Categories;