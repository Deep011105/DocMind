import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

api.interceptors.response.use(
    (res) => res,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('token');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export const register = (data) => api.post('/auth/register', data);
export const login = (data) => api.post('/auth/login', data);
export const getDocuments = () => api.get('/documents');
export const uploadDocument = (file) => {
    const form = new FormData();
    form.append('file', file);
    return api.post('/documents/upload', form);
};
export const getDocumentStatus = (id) => api.get(`/documents/${id}/status`);
export const deleteDocument = (id) => api.delete(`/documents/${id}`);
export const sendQuery = (question, documentId) =>
    api.post('/chat/query', { question, documentId });
export const getChatHistory = (documentId) =>
    api.get(`/chat/history/${documentId}`);