/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import { trackImgCSSAlterationCompile } from '../analytics/trackEvents.js';

/**
 * Reserve a placeholder height for author-content `<img>`s before they load, to
 * avoid layout shift — ported verbatim from the old `contentHtml` directive's
 * `addImagePlaceholder` link (it ran at `$element.ready`; here it runs in the
 * React renderer's effect, after the HTML is in the DOM).
 *
 * For each not-yet-loaded image that declares both a width and height (via the
 * `width`/`height` attrs or inline style — `%` excluded, as it says nothing about
 * intrinsic size), set its height to the author aspect ratio scaled to the
 * element's *rendered* width; on load, reset to the css height (or `auto`).
 */

// purposely excludes % — a percentage dimension tells us nothing about size
const widthMatcher = /width:\s?(\d+)([a-z]+)\s?(;|$)/;
const heightMatcher = /height:\s?(\d+)([a-z]+)\s?(;|$)/;
const attrMatcher = /\s?(\d+)([a-z]+)\s?/;

const getPropFromString = (value: string | null, matcher: RegExp): number | null => {
  const match = value && value.match(matcher);
  return match ? +match[1] : null;
};

export function applyResponsiveImagePlaceholders(container: HTMLElement): void {
  container.querySelectorAll('img').forEach(img => {
    if (img.getAttribute('image-loaded') === 'true') {
      return;
    }

    const heightAttr = getPropFromString(img.getAttribute('height'), attrMatcher);
    const heightCss = getPropFromString(img.getAttribute('style'), heightMatcher);
    const widthAttr = getPropFromString(img.getAttribute('width'), attrMatcher);
    const widthCss = getPropFromString(img.getAttribute('style'), widthMatcher);

    const height = heightAttr || heightCss;
    const width = widthAttr || widthCss;

    if (!height || !width || width <= 0) {
      return;
    }

    // only apply if the image already has a rendered width
    if (img.width <= 0) {
      return;
    }

    const responsiveHeight = (height / width) * img.width;
    img.style.height = `${responsiveHeight}px`;

    const onLoad = () => {
      img.style.height = heightCss ? `${heightCss}px` : 'auto';
      img.setAttribute('image-loaded', 'true');
      img.removeEventListener('load', onLoad);
    };
    img.addEventListener('load', onLoad);

    trackImgCSSAlterationCompile();
  });
}
