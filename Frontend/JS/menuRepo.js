const API_URL = 'https://despliegueeduc.duckdns.org';

document.addEventListener('DOMContentLoaded', async () => {
    try {
        const response = await fetch(`${API_URL}/session`, { credentials: 'include' });
        if (response.status === 401) {
            window.location.href = '../../index.html';
        }
        // Tanto Docente como Director pueden usar este módulo.
    } catch (error) {
        console.error('Error verificando sesión:', error);
    }
});