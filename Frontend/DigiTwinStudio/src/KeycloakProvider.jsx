import { useState, useEffect } from "react";
import { KeycloakContext } from "./KeycloakContext";

// PKCE helper functions
const generateCodeVerifier = () => {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return btoa(String.fromCharCode.apply(null, array))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
};

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

const generateCodeChallenge = async (codeVerifier) => {
  const encoder = new TextEncoder();
  const data = encoder.encode(codeVerifier);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return btoa(String.fromCharCode.apply(null, new Uint8Array(digest)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '');
};

// Helper function for Safari login
const initiateLogin = () => {
  // Generate PKCE code verifier
  const codeVerifier = generateCodeVerifier();
  sessionStorage.setItem('pkce_code_verifier', codeVerifier);
  
  // Generate code challenge
  generateCodeChallenge(codeVerifier).then(codeChallenge => {
    const authUrl = new URL('http://localhost:8080/realms/DigiTwinStudio/protocol/openid-connect/auth');
    authUrl.searchParams.set('client_id', 'digitwin-auth');
    authUrl.searchParams.set('redirect_uri', window.location.origin);
    authUrl.searchParams.set('response_type', 'code');
    authUrl.searchParams.set('scope', 'openid profile email custom-profile');
    authUrl.searchParams.set('code_challenge', codeChallenge);
    authUrl.searchParams.set('code_challenge_method', 'S256');
    
    window.location.href = authUrl.toString();
  });
};

// Helper function for logout
const logoutUser = () => {
  const idToken = sessionStorage.getItem('id_token');
  sessionStorage.removeItem('access_token');
  sessionStorage.removeItem('refresh_token');
  sessionStorage.removeItem('id_token');
  
  // Build logout URL with id_token_hint if available
  let logoutUrl = "http://localhost:8080/realms/DigiTwinStudio/protocol/openid-connect/logout?post_logout_redirect_uri=" + encodeURIComponent(window.location.origin);
  if (idToken) {
    logoutUrl += "&id_token_hint=" + encodeURIComponent(idToken);
  }
  
  window.location.href = logoutUrl;
};

// Helper function to check for existing Keycloak session
const checkSilentSSO = () => {
  console.log("Safari: Checking for existing Keycloak session");
  
  // Try to get tokens silently using a hidden iframe approach
  const iframe = document.createElement('iframe');
  iframe.style.display = 'none';
  iframe.src = 'http://localhost:8080/realms/DigiTwinStudio/protocol/openid-connect/auth?' + 
    'client_id=digitwin-auth&' +
    'redirect_uri=' + encodeURIComponent(window.location.origin + '/silent-check-sso.html') + '&' +
    'response_type=code&' +
    'scope=openid profile email custom-profile&' +
    'prompt=none'; // This tells Keycloak to not show login screen
  
  document.body.appendChild(iframe);
  
  // Listen for messages from the iframe
  const handleMessage = (event) => {
    if (event.origin !== window.location.origin) return;
    
    try {
      const url = new URL(event.data);
      const code = url.searchParams.get('code');
      const error = url.searchParams.get('error');
      
      if (code) {
        console.log("Safari: Found existing session, exchanging code");
        // Exchange code for tokens
        exchangeCodeForTokens(code);
      } else if (error === 'login_required') {
        console.log("Safari: No existing session found");
        setNotAuthenticated();
      } else {
        console.log("Safari: Silent SSO check completed, no session");
        setNotAuthenticated();
      }
    } catch {
      console.log("Safari: Silent SSO check completed");
      setNotAuthenticated();
    }
    
    // Clean up
    window.removeEventListener('message', handleMessage);
    document.body.removeChild(iframe);
  };
  
  window.addEventListener('message', handleMessage);
  
  // Timeout fallback
  setTimeout(() => {
    if (iframe.parentNode) {
      window.removeEventListener('message', handleMessage);
      document.body.removeChild(iframe);
      setNotAuthenticated();
    }
  }, 5000);
};

// Helper to exchange code for tokens
const exchangeCodeForTokens = (code) => {
  fetch('http://localhost:8080/realms/DigiTwinStudio/protocol/openid-connect/token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: 'digitwin-auth',
      code: code,
      redirect_uri: window.location.origin + '/silent-check-sso.html',
      scope: 'openid profile email custom-profile'
    })
  })
  .then(response => response.json())
  .then(data => {
    if (data.access_token) {
      console.log("Response from token endpoint:", data);
      const tokenParts = data.access_token.split('.');
      const payload = JSON.parse(base64urlDecode(tokenParts[1]));              console.log("Safari: Silent authentication successful", payload.preferred_username);
              console.log("Safari: Silent token payload:", payload);
      
      // Store tokens
      sessionStorage.setItem('access_token', data.access_token);
      sessionStorage.setItem('refresh_token', data.refresh_token);
      if (data.id_token) {
        sessionStorage.setItem('id_token', data.id_token);
      }
      
      // Update auth state
      setAuthState({
        keycloak: {
          login: () => initiateLogin(),
          logout: () => logoutUser(),
          tokenParsed: payload
        },
        authenticated: true,
        ready: true
      });
    } else {
      setNotAuthenticated();
    }
  })
  .catch(err => {
    console.error("Safari: Silent token exchange failed", err);
    setNotAuthenticated();
  });
};

// Helper to set unauthenticated state
const setNotAuthenticated = () => {
  setAuthState({
    keycloak: { login: () => initiateLogin(), logout: () => {} },
    authenticated: false,
    ready: true
  });
};

// Reference to setAuthState (will be set in component)
let setAuthState = null;

export default function KeycloakProvider({ children }) {
  const [mounted, setMounted] = useState(false);
  const [authState, setAuthStateInternal] = useState({
    keycloak: null,
    authenticated: false,
    ready: false
  });

  // Connect the external setAuthState reference
  useEffect(() => {
    setAuthState = setAuthStateInternal;
  }, []);

  // Only run after component is mounted (client-side only)
  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (!mounted) return;

    const isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent);
    
    if (isSafari) {
      console.log("Safari detected - simplified auth flow");
      
      // Check if we're returning from a login redirect
      const urlParams = new URLSearchParams(window.location.search);
      const code = urlParams.get('code');
      const sessionState = urlParams.get('session_state');
      
      if (code && sessionState) {
        // We're returning from login - exchange code for token
        console.log("Safari: Processing login callback");
        
        // Exchange authorization code for tokens
        fetch('http://localhost:8080/realms/DigiTwinStudio/protocol/openid-connect/token', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          body: new URLSearchParams({
            grant_type: 'authorization_code',
            client_id: 'digitwin-auth',
            code: code,
            redirect_uri: window.location.origin,
            code_verifier: sessionStorage.getItem('pkce_code_verifier') || '',
            scope: 'openid profile email custom-profile'
          })
        })
        .then(response => response.json())
        .then(data => {
          if (data.access_token) {
            // Parse the token to get user info
            const tokenParts = data.access_token.split('.');
            const payload = JSON.parse(base64urlDecode(tokenParts[1]));
            
            console.log("Safari: Authentication successful", payload.preferred_username);
            console.log("Safari: Token payload:", payload);
            
            // Store tokens (including ID token if available)
            sessionStorage.setItem('access_token', data.access_token);
            sessionStorage.setItem('refresh_token', data.refresh_token);
            if (data.id_token) {
              sessionStorage.setItem('id_token', data.id_token);
            }
            
            // Clean up URL
            window.history.replaceState({}, document.title, window.location.pathname);
            
            setAuthStateInternal({
              keycloak: {
                login: () => initiateLogin(),
                logout: () => logoutUser(),
                tokenParsed: payload
              },
              authenticated: true,
              ready: true
            });
          } else {
            console.error("Safari: Token exchange failed", data);
            // Try silent SSO check if token exchange fails
            checkSilentSSO();
          }
        })
        .catch(err => {
          console.error("Safari: Token exchange error", err);
          // Try silent SSO check if token exchange fails
          checkSilentSSO();
        });
      } else {
        // Check if we already have a stored token
        const storedToken = sessionStorage.getItem('access_token');
        if (storedToken) {
          try {
            const tokenParts = storedToken.split('.');
            const payload = JSON.parse(base64urlDecode(tokenParts[1]));
            
            // Check if token is still valid
            if (payload.exp * 1000 > Date.now()) {
              console.log("Safari: Using stored token", payload.preferred_username);
              console.log("Safari: Stored token payload:", payload);
              setAuthStateInternal({
                keycloak: {
                  login: () => initiateLogin(),
                  logout: () => logoutUser(),
                  tokenParsed: payload
                },
                authenticated: true,
                ready: true
              });
              return;
            }
          } catch (e) {
            console.error("Safari: Invalid stored token", e);
            sessionStorage.removeItem('access_token');
            sessionStorage.removeItem('refresh_token');
            sessionStorage.removeItem('id_token');
          }
        }
        
        // No valid local token - check for existing Keycloak session
        checkSilentSSO();
      }
    } else {
      // Normal Keycloak initialization for other browsers
      import("keycloak-js").then(({ default: Keycloak }) => {
        const kc = new Keycloak({
          url: "http://localhost:8080",
          realm: "DigiTwinStudio",
          clientId: "digitwin-auth",
        });

        kc.init({
          onLoad: "check-sso",
          silentCheckSsoRedirectUri: window.location.origin + "/silent-check-sso.html",
          pkceMethod: "S256",
          checkLoginIframe: false,
          enableLogging: false
        })
        .then(auth => {
          setAuthStateInternal({
            keycloak: kc,
            authenticated: auth,
            ready: true
          });
          
          if (auth) {
            console.log("User authenticated:", kc.tokenParsed?.preferred_username);
          }
        })
        .catch(err => {
          console.error("Keycloak initialization failed:", err);
          setAuthStateInternal({
            keycloak: kc,
            authenticated: false,
            ready: true
          });
        });
      });
    }
  }, [mounted]);

  // Don't render anything until mounted (prevents SSR issues)
  if (!mounted) {
    return (
      <KeycloakContext.Provider value={{
        keycloak: null,
        authenticated: false,
        ready: false
      }}>
        {children}
      </KeycloakContext.Provider>
    );
  }

  return (
    <KeycloakContext.Provider value={authState}>
      {children}
    </KeycloakContext.Provider>
  );
}
