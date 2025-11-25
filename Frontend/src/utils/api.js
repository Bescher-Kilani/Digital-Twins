// Get API URL from environment variable with fallback
const API_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:9090';

export const apiClient = {
    get: async (endpoint) => {
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        });
        return response.json();
    },

    post: async (endpoint, data) => {
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data),
        });
        return response.json();
    },

};

export default API_URL;