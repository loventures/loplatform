import { createRequire } from 'module';
import fs from 'fs';
import child_process from 'child_process';
import jetpack from 'fs-jetpack';

const git = createRequire(import.meta.url)('git-rev-sync');

import components from './components.json' with { type: 'json' };

const branch = process.env.GIT_BRANCH || git.branch();
const revision = process.env.GIT_COMMIT || git.long();
const buildNumber = process.env.BUILD_NUMBER || 'unset';
const buildDate = new Date().toISOString();

const car = {
  identifier: 'loi.platform',
  name: 'Platform',
  version: process.env.DE_VERSION || 'dev-SNAPSHOT',
  branch,
  revision,
  buildNumber,
  buildDate,
  ...components,
};

const TARGET = 'target';
const DEST = `${TARGET}/loi/platform`;

fs.rmSync(TARGET, { recursive: true, force: true });
fs.mkdirSync(DEST, { recursive: true });
fs.writeFileSync(`${TARGET}/car.json`, JSON.stringify(car, undefined, 2));

const APPS = ['admin', 'analytics', 'domain', 'errors', 'etc', 'overlord', 'sys'];

for (const app of APPS) {
  const src = jetpack.cwd(`dist/${app}/html`);
  const dst = jetpack.cwd(DEST);

  src.find({ matching: '*.html' }).forEach(path => {
    const content = src.read(path);
    const transformedContent = content.replaceAll(/(\.\.\/)+assets/g, '$$$$url/assets');
    dst.write(path, transformedContent);
  });

  fs.cpSync(`dist/${app}/assets`, `${DEST}/${app}/assets`, { recursive: true });
  fs.cpSync(`src/i18n`, `${DEST}/${app}`, { recursive: true });
}

// awful
fs.mkdirSync(`${DEST}/etc/imgs`, { recursive: true });
fs.cpSync(`src/imgs/ponies`, `${DEST}/etc/imgs/ponies`, { recursive: true });

child_process.execSync(`zip -r platform.zip car.json loi`, {
  cwd: TARGET,
});

console.log(`Created ${TARGET}/platform.zip`);
