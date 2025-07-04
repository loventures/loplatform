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
  identifier: 'loi.courseware',
  name: 'Courseware',
  version: process.env.DE_VERSION || 'dev-SNAPSHOT',
  branch,
  revision,
  buildNumber,
  buildDate,
  ...components,
};

const TARGET = 'target';
const DEST = `${TARGET}/loi/courseware`;

fs.rmSync(TARGET, { recursive: true, force: true });
fs.mkdirSync(DEST, { recursive: true });
fs.writeFileSync(`${TARGET}/car.json`, JSON.stringify(car, undefined, 2));

const src = jetpack.cwd(`dist`);
const dst = jetpack.cwd(DEST);

['index.html', 'instructor.html'].forEach(html => {
  const content = src.read(html);
  const transformedContent = content.replaceAll(/\.\/assets/g, '$$$$url/assets');
  dst.write(html, transformedContent);
});

fs.cpSync(`dist/assets`, `${DEST}/assets`, { recursive: true });
fs.cpSync(`src/i18n`, DEST, { recursive: true });

child_process.execSync(`zip -r courseware.zip car.json loi`, {
  cwd: TARGET,
});

console.log(`Created ${TARGET}/courseware.zip`);
