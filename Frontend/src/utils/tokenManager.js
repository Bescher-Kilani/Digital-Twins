// Token management utilities
const KEYCLOAK_BASE_URL = 'http://localhost:8080/realms/DigiTwinStudio/protocol/openid-connect';
const CLIENT_ID = 'digitwin-auth';

// Helper function to decode base64url (JWT tokens use this instead of standard base64)
const base64urlDecode = (str) => {
  // Add padding if needed
  const padded = str + '='.repeat((4 - str.length % 4) % 4);
  // Convert base64url to base64
  const base64 = padded.replace(/-/g, '+').replace(/_/g, '/');
  try {
    return atob(base64);
  } catch (e) {
    console.error('Failed to decode base64url string:', str, e);
    throw e;
  }
};

// Check if token is expired or will expire soon (within 5 minutes)
export const isTokenExpired = (token) => {
  if (!token) return true;
  
  try {
    const tokenParts = token.split('.');
    const payload = JSON.parse(base64urlDecode(tokenParts[1]));
    const currentTime = Date.now() / 1000;
    
    // Check if token expires within 5 minutes (300 seconds)
    return (payload.exp - currentTime) < 300;
  } catch (error) {
    console.error('Error checking token expiry:', error);
    return true;
  }
};

// Get the current access token from storage
export const getAccessToken = (keycloakInstance = null) => {
  // First check sessionStorage (Safari and manual storage)
  const sessionToken = sessionStorage.getItem('access_token');
  
  if (sessionToken) {
    return sessionToken;
  }
  
  // Check if Keycloak instance has token (non-Safari browsers)
  if (keycloakInstance && keycloakInstance.token) {
    return keycloakInstance.token;
  }
  
  // Fallback to localStorage if needed (legacy support)
  const localToken = localStorage.getItem('authToken');
  
  if (localToken) {
    return localToken;
  }
  
  return null;
};

// Get the current refresh token from storage
export const getRefreshToken = (keycloakInstance = null) => {
  // First check sessionStorage (Safari and manual storage)
  const sessionRefreshToken = sessionStorage.getItem('refresh_token');
  if (sessionRefreshToken) {
    return sessionRefreshToken;
  }
  
  // Check if Keycloak instance has refresh token (non-Safari browsers)
  if (keycloakInstance && keycloakInstance.refreshToken) {
    return keycloakInstance.refreshToken;
  }
  
  return null;
};

// Refresh the access token using the refresh token
export const refreshAccessToken = async (keycloakInstance = null) => {
  const refreshToken = getRefreshToken(keycloakInstance);
  
  if (!refreshToken) {
    throw new Error('No refresh token available');
  }

  try {
    console.log('Attempting to refresh access token...');
    
    const response = await fetch(`${KEYCLOAK_BASE_URL}/token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        grant_type: 'refresh_token',
        client_id: CLIENT_ID,
        refresh_token: refreshToken,
        scope: 'openid profile email custom-profile'
      })
    });

    if (!response.ok) {
      throw new Error(`Token refresh failed: ${response.status} ${response.statusText}`);
    }

    const data = await response.json();
    
    if (data.access_token) {
      // Store the new tokens
      sessionStorage.setItem('access_token', data.access_token);
      
      if (data.refresh_token) {
        sessionStorage.setItem('refresh_token', data.refresh_token);
      }
      
      if (data.id_token) {
        sessionStorage.setItem('id_token', data.id_token);
      }
      
      console.log('Access token refreshed successfully');
      return data.access_token;
    } else {
      throw new Error('No access token in refresh response');
    }
  } catch (error) {
    console.error('Token refresh failed:', error);
    
    // Clear stored tokens if refresh fails
    sessionStorage.removeItem('access_token');
    sessionStorage.removeItem('refresh_token');
    sessionStorage.removeItem('id_token');
    localStorage.removeItem('authToken');
    
    throw error;
  }
};

// Get a valid access token, refreshing if necessary
export const getValidAccessToken = async (keycloakInstance = null) => {
  // For non-Safari browsers with Keycloak instance, use Keycloak's built-in token refresh
  if (keycloakInstance && keycloakInstance.updateToken && typeof keycloakInstance.updateToken === 'function') {
    try {
      // updateToken(minValidity) - refreshes token if it expires within minValidity seconds (default 5)
      const refreshed = await keycloakInstance.updateToken(300); // 5 minutes
      if (refreshed) {
        console.log('Token refreshed via Keycloak');
      }
      return keycloakInstance.token;
    } catch (error) {
      console.error('Keycloak token refresh failed:', error);
      throw new Error('Authentication failed, please log in again');
    }
  }
  
  // Fallback to manual token management (Safari and other cases)
  let token = getAccessToken(keycloakInstance);
  
  if (!token) {
    throw new Error('No access token available');
  }
  
  if (isTokenExpired(token)) {
    console.log('Access token is expired or about to expire, refreshing...');
    token = await refreshAccessToken(keycloakInstance);
  }
  
  return token;
};

// Enhanced fetch function that automatically handles token refresh
export const authenticatedFetch = async (url, options = {}, keycloakInstance = null) => {
  try {
    // Get a valid token (will refresh if needed)
    const token = await getValidAccessToken(keycloakInstance);
    
    // Prepare headers with authentication
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers
    };
    
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    
    // Make the request
    const response = await fetch(url, {
      ...options,
      headers
    });
    
    // If we get a 401, try refreshing the token once more
    if (response.status === 401) {
      console.log('Received 401, attempting token refresh...');
      
      try {
        const newToken = await refreshAccessToken(keycloakInstance);
        headers['Authorization'] = `Bearer ${newToken}`;
        
        // Retry the request with the new token
        return await fetch(url, {
          ...options,
          headers
        });
      } catch (refreshError) {
        console.error('Token refresh failed after 401:', refreshError);
        // Redirect to login or handle authentication failure
        window.location.href = '/signin';
        throw new Error('Authentication failed, please log in again');
      }
    }
    
    return response;
  } catch (error) {
    console.error('Authenticated fetch failed:', error);
    throw error;
  }
};

// Set up automatic token refresh interval
let refreshInterval = null;

export const startTokenRefreshInterval = () => {
  // Clear any existing interval
  if (refreshInterval) {
    clearInterval(refreshInterval);
  }
  
  // Check token every minute and refresh if needed
  refreshInterval = setInterval(async () => {
    const token = getAccessToken();
    if (token && isTokenExpired(token)) {
      try {
        await refreshAccessToken();
        console.log('Token automatically refreshed');
      } catch (error) {
        console.error('Automatic token refresh failed:', error);
        // Could trigger a re-login flow here
      }
    }
  }, 60000); // Check every minute
};

export const stopTokenRefreshInterval = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
    refreshInterval = null;
  }
};
