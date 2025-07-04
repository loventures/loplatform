import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { ViteEjsPlugin } from 'vite-plugin-ejs';
import { globSync } from 'glob';

const ProxyConfig = {
  target: 'https://localhost:8181',
  changeOrigin: true,
  secure: false,
  ws: false,
};

// https://vite.dev/config/
export default defineConfig(() => {
  const app = process.env.APP ?? 'unknown';

  return {
    plugins: [
      ViteEjsPlugin(),
      react(),
      {
        name: 'xtitle',
        configureServer: () => console.log('\x1b]0;Platform\x07'),
      },
    ],

    server: {
      port: 5171,
      strictPort: true,
      // open: "/admin.html",
      proxy: {
        '/api': ProxyConfig,
        '/sys': ProxyConfig,
        '/Domain/Media': ProxyConfig,
        '/Users/': ProxyConfig,
      },
    },

    build: {
      rollupOptions: {
        input: globSync(`html/${app}/**/*.html`),
      },
      outDir: `dist/${app}`,
    },

    base: './',
  };
});
