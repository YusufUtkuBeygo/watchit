import api from './api';

//Bütün istekler ortak api nesnesinden gider; giriş yapılmışsa token kendiliğinden eklenir

export async function getAllCategories()
{
    const response = await api.get('/categories');
    return response.data;
}
