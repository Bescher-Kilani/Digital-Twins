import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.jsx";
import "bootstrap/dist/css/bootstrap.min.css";
import "./i18n";
import KeycloakProvider from "./KeycloakProvider";

ReactDOM.createRoot(document.getElementById("root")).render(
  <KeycloakProvider>
    <App />
  </KeycloakProvider>
);
