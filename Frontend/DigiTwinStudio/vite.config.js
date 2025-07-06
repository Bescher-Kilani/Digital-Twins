import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
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
