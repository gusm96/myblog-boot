import axios from "axios";
import { CATEGORIES, CATEGORY_CRUD } from "../apiConfig";

const getToken = (accessToken) => {
  return `bearer ${accessToken}`;
};
export const getCategories = () => {
  return axios.get(`${CATEGORY_CRUD}`).then((res) => res.data);
};
export const getCategoriesV2 = () => {
  return axios.get(`${CATEGORIES}`).then((res) => res.data);
};
export const addNewCategory = (categoryName, accessToken) => {
  return axios.post(
    `${CATEGORY_CRUD}`,
    {
      categoryName: categoryName,
    },

    {
      headers: {
        Authorization: getToken(accessToken),
      },
    },
    {
      withCredentials: true,
    }
  );
};
export const deleteCategory = (categoryId, accessToken) => {
  return axios
    .delete(`${CATEGORY_CRUD}/${categoryId}`, {
      headers: {
        Authorization: getToken(accessToken),
      },
    })
    .then((res) => res.data);
};
