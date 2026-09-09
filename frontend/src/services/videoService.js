const BASE_URL = '/api/videos';

export async function getAllVideos()
{

  const response = await fetch(BASE_URL);
  if(!response.ok)
  {
    throw new Error(`Videolar getirilemedi. Durum:${response.status}`);
  }

  return await response.json();

}

export async function getVideosByCategoryId(categoryId) {
  const response = await fetch(`${BASE_URL}?categoryId=${categoryId}`);

  if(!response.ok){
    throw new Error(`Kategoriye ait videolar getirilemedi: ${categoryId}`);

  }
  return await response.json();
}

export async function getVideoById(id)
{
  const response = await fetch(`${BASE_URL}/${id}`);

  if(!response.ok){
    throw new Error('Video bulunamadi. ID:${id}');
  }

  return await response.json();
}

export async function deleteVideoById(id)
{
  const response = await fetch(`${BASE_URL}/${id}`,{
    method:'DELETE',
  });

  if(!response.ok)
  {
    throw new Error(`Video silinemedi. ID: ${id}`);
  }
}