//base address defination
const BASE_URL='/api/categories'

//get all categories func.

//export -> public this function can reachelable from other files
//await means wait until the condititon happens (in this case wait until fetch the files BASE_URL)
//there no more attribute for fetch mothod for this reason it behaves like get method
export async function getAllCategories()
{
    const response = await fetch(BASE_URL);
    return await response.json();
    
}

//categoryData:its an object that comes from the outside
//method:it informs the springboot this request is a post request
//headers:it informs the incomong package is json package
//body: JSON.stringify(categoryData):it converts to JavaScript objet into json and populate the infos CategoryRequestDto object
export async function createCategory(categoryData)
{

    const response = await fetch(BASE_URL,{
        method:'POST',
        headers:{
            'Content-type':'application/json'
        },
        body:JSON.stringify(categoryData)
    });
    return await response.json();
}

//${BASE_URL}/${id}:its an strong concatenation
//method:it inform this request is an delete request
export async function deleteCategory(id)
{
    await fetch('${BASE_URL}/${id}',{
        method:'DELETE'
    });
}