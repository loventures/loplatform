import { PluginOption } from 'vite';
import fsp from 'fs/promises';

// import htmlImport from '@ayatkyo/vite-plugin-html-import';

const htmlFileRegex = /\.html$/;
const postfixRE = /[?#].*$/s;
const postfix = '?html-import';

function cleanUrl(url: string) {
  return url.replace(postfixRE, '');
}

export const htmlImportBuild = (): PluginOption => ({
  name: 'html-import:build',
  enforce: 'pre',
  apply: 'build',
  async resolveId(id: string, importer: string | undefined, options: any) {
    if (htmlFileRegex.test(id) && !options.isEntry) {
      let res = await this.resolve(id, importer, {
        skipSelf: true,
        ...options,
      });

      if (!res || res.external) return res;

      return res.id + postfix;
    }
  },

  async load(id: string) {
    if (!id.endsWith(postfix)) return;
    let htmlContent = await fsp.readFile(cleanUrl(id));
    return `export default ${JSON.stringify(htmlContent.toString('utf-8'))}`;
  },
});

export function htmlImportServe(): PluginOption {
  return {
    name: 'html-import:serve',
    apply: 'serve',
    transform(src: string, id: string) {
      if (htmlFileRegex.test(id)) {
        return {
          code: `export default ${JSON.stringify(src)}`,
        };
      }
    },
  };
}
