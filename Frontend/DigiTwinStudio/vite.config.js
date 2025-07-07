import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import svgr from 'vite-plugin-svgr';

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    svgr()
  ],
  server: {
    port: 5173,
    strictPort: true,
    hmr: {
      overlay: false // Disable error overlay that might interfere with Keycloak
    }
  },
  define: {
    global: 'globalThis', // Fix for libraries that expect global
  }
})
