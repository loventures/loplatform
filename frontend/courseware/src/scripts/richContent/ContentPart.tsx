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

import React from 'react';

import { HtmlWithMathJax } from '../components/HtmlWithMathjax.tsx';

interface ContentPartProps {
  part: any;
}

// The HTML-family content-part types all rendered the `content-html` primitive
// (`<html-with-math-jax html="renderedHtml" responsive-images>`).
const HTML_PART_TYPES = new Set(['html', 'paragraph', 'richContent']);

/**
 * Native React renderer for a content part — the replacement for the Angular `contentPartPlayer`
 * (`<content-directive-loader>` + the `$compile` `ContentDirectiveFactory` registry + the `content-*`
 * primitive directives), which were only ever reached, in the live app, by rendering instructions from
 * React. Reproduces the directive's `shimContentKeys` (the legacy `contentPart*`→`part*` / `html`→
 * `renderedHtml` aliasing) and the two primitives instructions actually use:
 *   - HTML family (`html`/`paragraph`/`richContent`) → the React `HtmlWithMathJax` in `.content-html`.
 *   - `image` → the `.content-image` src + caption HTML.
 * Any other part type falls through to rendering its HTML (the legacy `content-video`/`-audio`/`-url`/
 * `-caseStudy` primitives were inert "use other directive" placeholders, and the `$compile` content
 * renderer is no longer mounted anywhere else).
 */
export const ContentPart: React.FC<ContentPartProps> = ({ part }) => {
  const partType = part?.partType ?? part?.contentPartType;
  const renderedHtml = part?.renderedHtml ?? part?.html;

  if (HTML_PART_TYPES.has(partType)) {
    return (
      <div className="content-html">
        <HtmlWithMathJax
          html={renderedHtml}
          responsiveImages={true}
        />
      </div>
    );
  }

  if (partType === 'image') {
    return (
      <div className="content-image">
        {part?.src ? (
          <div className="content-src">
            <img src={part.src} />
          </div>
        ) : null}
        {part?.html ? <HtmlWithMathJax html={part.html} /> : null}
      </div>
    );
  }

  return renderedHtml ? <HtmlWithMathJax html={renderedHtml} /> : null;
};

export default ContentPart;
