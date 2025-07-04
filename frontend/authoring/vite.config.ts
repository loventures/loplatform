import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { ViteEjsPlugin } from 'vite-plugin-ejs';
import ViteBasicSslPlugin from '@vitejs/plugin-basic-ssl';
import { viteStaticCopy } from 'vite-plugin-static-copy';

const ProxyConfig = {
  target: 'https://localhost:8181',
  changeOrigin: true,
  secure: false,
  ws: false,
};

export default defineConfig(() => {
  return {
    plugins: [
      ViteEjsPlugin(),
      ViteBasicSslPlugin(),
      react(),
      viteStaticCopy({
        targets: [
          {
            src: './node_modules/mathjax/es5',
            dest: 'assets/',
            rename: 'mathjax',
          },
          {
            src: './node_modules/iframe-resizer/js',
            dest: 'assets/',
            rename: 'iframe-resizer',
          },
          {
            src: './node_modules/jquery/dist',
            dest: 'assets/',
            rename: 'jquery',
          },
        ],
      }),
      {
        name: 'xtitle',
        configureServer: () => console.log('\x1b]0;Authoring\x07'),
      },
    ],

    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': ProxyConfig,
        '/event/': ProxyConfig,
        '/sys': ProxyConfig,
        '/Domain/Media': ProxyConfig,
        '/Users/': ProxyConfig,
        '/static': ProxyConfig,
      },
      hmr: {
        // Run hot-reload web socket on separate port so browser connects
        // directly and not through detomcat local proxy.
        port: 5199,
      },
    },

    base: './',
  };
});
