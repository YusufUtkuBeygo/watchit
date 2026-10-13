import api from './api';

//Bütün istekler ortak api nesnesinden gider; giriş yapılmışsa token kendiliğinden eklenir

export async function getAllCategories()
{
    const response = await api.get('/categories');
    return response.data;
}

//categoryData: dışarıdan gelen nesne, axios onu JSON'a çevirip CategoryRequestDto olarak yollar
export async function createCategory(categoryData)
{
    const response = await api.post('/categories', categoryData);
    return response.data;
}

export async function deleteCategory(id)
{
    await api.delete(`/categories/${id}`);
}
