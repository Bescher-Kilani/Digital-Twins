import { useEffect, useState, useRef } from "react";
import { KeycloakContext } from "./KeycloakContext";
import Keycloak from "keycloak-js";

export default function KeycloakProvider({ children }) {
  const [initialized, setInitialized] = useState(false);
  const [authenticated, setAuthenticated] = useState(false);
  const [keycloak, setKeycloak] = useState(null);
  const [initError, setInitError] = useState(null);
  const initRef = useRef(false);

  useEffect(() => {
    // Prevent multiple initializations
    if (initRef.current) return;
    initRef.current = true;

    console.log("🔄 Initializing Keycloak...");

    // Create a fresh Keycloak instance
    const kc = new Keycloak({
        url: "http://localhost:8080",
        realm: "DigiTwinStudio",
        clientId: "digitwin-auth",
    });

    setKeycloak(kc);

    const initOptions = {
      onLoad: "check-sso",
      silentCheckSsoRedirectUri: window.location.origin + "/silent-check-sso.html",
      pkceMethod: "S256",
      checkLoginIframe: false, // Disable iframe check to prevent refresh loops
      enableLogging: true
    };

    kc.init(initOptions)
      .then(auth => {
        console.log("🔐 Keycloak initialization result:", auth);
        setAuthenticated(auth);
        setInitialized(true);
        
        if (auth) {
          console.log("✅ Authenticated:", kc.tokenParsed?.preferred_username);
          
          // Set up token refresh
          const refreshInterval = setInterval(() => {
            kc.updateToken(70).then((refreshed) => {
              if (refreshed) {
                console.log('🔄 Token refreshed');
              }
            }).catch(() => {
              console.log('❌ Failed to refresh token');
              clearInterval(refreshInterval);
            });
          }, 60000);

          // Clean up interval on unmount
          return () => clearInterval(refreshInterval);
        } else {
          console.log("🚪 Not logged in.");
        }
      })
      .catch(err => {
        console.error("❌ Keycloak init error:", err);
        setInitError(err.message || "Failed to initialize Keycloak");
        setInitialized(true);
        setAuthenticated(false);
      });
  }, []);

  // Show error state if initialization failed
  if (initError) {
    return (
      <div style={{ padding: "20px", textAlign: "center" }}>
        <h3>Authentication Error</h3>
        <p>Failed to initialize authentication: {initError}</p>
        <button onClick={() => window.location.reload()}>Retry</button>
      </div>
    );
  }

  // Show loading state while initializing
  if (!initialized || !keycloak) {
    return (
      <div style={{ padding: "20px", textAlign: "center" }}>
        <div>Loading authentication…</div>
      </div>
    );
  }

  return (
    <KeycloakContext.Provider value={{ keycloak, authenticated }}>
      {children}
    </KeycloakContext.Provider>
  );
}
