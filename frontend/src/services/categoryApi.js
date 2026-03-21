import apiClient from "./apiClient";
import axios from "axios";
import { CATEGORIES, CATEGORIES_FOR_ADMIN, CATEGORY_CRUD } from "../apiConfig";

export const getCategories = () => {
  return axios.get(`${CATEGORY_CRUD}`).then((res) => res.data);
};

export const getCategoriesV2 = () => {
  return axios.get(`${CATEGORIES}`).then((res) => res.data);
};

export const getCategoriesForAdmin = () => {
  return apiClient.get(`${CATEGORIES_FOR_ADMIN}`).then((res) => res.data);
};

export const addNewCategory = (categoryName) => {
  return apiClient.post(
    `${CATEGORY_CRUD}`,
    { categoryName },
    { withCredentials: true }
  );
};

export const deleteCategory = (categoryId) => {
  return apiClient
    .delete(`${CATEGORY_CRUD}/${categoryId}`)
    .then((res) => res.data);
};
