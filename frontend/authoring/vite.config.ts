import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { ViteEjsPlugin } from 'vite-plugin-ejs';
import ViteBasicSslPlugin from '@vitejs/plugin-basic-ssl';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import dsv from '@rollup/plugin-dsv';

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
      dsv(),
      viteStaticCopy({
        // vite-plugin-static-copy v4 always preserves the matched path's directory
        // structure under `dest`; a string `rename` only replaces a file's basename.
        // So to land each source tree at `assets/<name>/…` we strip the leading source
        // segments with `rename: { stripBase: N }` (N = depth of the source directory).
        targets: [
          {
            src: './node_modules/mathjax/es5',
            dest: 'assets/mathjax',
            rename: { stripBase: 3 }, // strip node_modules/mathjax/es5
          },
          {
            src: './node_modules/iframe-resizer/js',
            dest: 'assets/iframe-resizer',
            rename: { stripBase: 3 }, // strip node_modules/iframe-resizer/js
          },
          {
            src: './node_modules/jquery/dist',
            dest: 'assets/jquery',
            rename: { stripBase: 3 }, // strip node_modules/jquery/dist
          },
        ],
      }),
      {
        name: 'xtitle',
        configureServer: () => console.log('\x1b]0;Authoring\x07'),
      },
    ],

    css: {
      preprocessorOptions: {
        // quietDeps silences deprecation warnings from dependencies (e.g. bootstrap);
        // silenceDeprecations: ['import' as const] silences the @import deprecation, which we
        // are deliberately keeping for now.
        scss: { quietDeps: true, silenceDeprecations: ['import' as const] },
        sass: { quietDeps: true, silenceDeprecations: ['import' as const] },
      },
    },

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

    build: {
      // Set DEBUG=1 for a readable, source-mapped build (local debugging).
      minify: !process.env.DEBUG,
      sourcemap: !!process.env.DEBUG,
    },

    base: './',
  };
});
