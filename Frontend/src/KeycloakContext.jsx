import { createContext } from "react";

export const KeycloakContext = createContext({
  keycloak: null,
  authenticated: false,
  ready: false
});
