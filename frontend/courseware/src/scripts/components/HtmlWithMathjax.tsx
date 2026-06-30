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

import { processMathHtml, queueMathTypeset, wrapMath } from '../utilities/mathml';
import { applyResponsiveImagePlaceholders } from '../utilities/responsiveImages';
import React, { useEffect, useRef } from 'react';

import RichTextFailureErrorBoundary from '../directives/RichTextFailureErrorBoundary';

// Opt-in extras (off by default so the many other callers are unaffected):
//  - `responsiveImages` ports the old `contentHtml` directive's image-placeholder
//    sizing;
//  - `onRendered(renderedData)` fires once the content is in the DOM — the React
//    analogue of the `ng-init` "this content rendered" signals (e.g. the ordering
//    question's `choiceRendered` drag-drop positioning).
export const HtmlWithMathJax = ({ html, className, responsiveImages, onRendered, renderedData }: any) => {
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = wrapperRef.current;
    if (!el) {
      return;
    }

    if (responsiveImages) {
      applyResponsiveImagePlaceholders(el);
    }

    if (window.MathJax) {
      processMathHtml(el);
      queueMathTypeset(el);
    }

    if (onRendered) {
      onRendered(renderedData);
    }
  }, [html, responsiveImages, onRendered, renderedData]);

  return (
    <RichTextFailureErrorBoundary>
      <div
        className={className}
        ref={wrapperRef}
        dangerouslySetInnerHTML={{ __html: wrapMath(html) }}
      />
    </RichTextFailureErrorBoundary>
  );
};
