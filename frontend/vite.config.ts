import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    // Demo's registered dev port (~/.claude/ports.md). strictPort, because Vite
    // otherwise slides to the next free port, which is another app's.
    port: 5175,
    strictPort: true,
    proxy: {
      "/api": {
        // Honour VITE_API_TARGET so an isolated dev/e2e stack can point the
        // proxy at a backend on a non-default port; defaults to demo's :8092.
        target: process.env.VITE_API_TARGET ?? "http://localhost:8092",
        changeOrigin: true,
      },
    },
  },
});
