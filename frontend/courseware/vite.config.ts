import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { ViteEjsPlugin } from 'vite-plugin-ejs';
import ViteBasicSslPlugin from '@vitejs/plugin-basic-ssl';
import { htmlImportBuild, htmlImportServe } from './scripts/viteHtmlImport';
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
      htmlImportBuild(),
      htmlImportServe(),
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
            src: './node_modules/jquery/dist',
            dest: 'assets/jquery',
            rename: { stripBase: 3 }, // strip node_modules/jquery/dist
          },
          {
            src: './src/lo-ckeditor/',
            dest: 'assets/lo-ckeditor',
            rename: { stripBase: 2 }, // strip src/lo-ckeditor
          },
          {
            src: './src/custom.scss',
            dest: 'assets',
            rename: { stripBase: 1 }, // strip src/
          },
          {
            src: './node_modules/bootstrap/scss/',
            dest: 'assets/bootstrap',
            rename: { stripBase: 3 }, // strip node_modules/bootstrap/scss
          },
        ],
      }),
      {
        name: 'xtitle',
        configureServer: () => console.log('\x1b]0;Courseware\x07'),
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
      port: 5174,
      strictPort: true,
      proxy: {
        '/api/': ProxyConfig,
        '/sys/': ProxyConfig,
        '/event/': ProxyConfig,
        '/Domain/Media/': ProxyConfig,
        '/Users/': ProxyConfig,
        '/static/': ProxyConfig,
      },
      hmr: {
        // Run hot-reload web socket on separate port so browser connects
        // directly and not through detomcat local proxy.
        port: 5198,
      },
    },

    build: {
      // Set DEBUG=1 for a readable, source-mapped build (local debugging).
      minify: !process.env.DEBUG,
      sourcemap: !!process.env.DEBUG,
      rollupOptions: {
        input: ['index.html', 'instructor.html'],
      },
    },

    // https://github.com/vitejs/vite/discussions/5912#discussioncomment-2908994
    define: {
      global: {},
      process: { env: {} },
    },

    base: './',
  };
});
