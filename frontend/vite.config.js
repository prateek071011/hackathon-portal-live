// Vite build settings. Port 5173 is fixed because the backend's CORS rule allows only this origin.
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
  },
});
