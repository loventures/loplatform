/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
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

/*
    IE and EDGE treat custom tags (including <math> which they do not support natively)
    very strictly and fails them for simple things like html lookup.
    i.e.
        $('math') //empty list
        var $math = $('.known-container-of-math').children();
        $math //list of 1
        $math.html() //undefined
        $math[0].innerHTML //undefined
    This issue causes MathJax to not be able to get the <math> content for
    inserted <math> dom elements.

    However, If we set it again as html text onto a ie/edge native element
    then MathJax can pick it up.

    So to make it work for ie/edge, we wrap math elements then resets the
    html into these containers before to typeset with MathJax
*/

import { replace } from 'lodash';
import browserType, { EDGE, IE } from './browserType.js';

const mathContainerClass = 'mathms-container';
const mathTexContainerClass = 'math-tex';

const mathStarMatch = /<math/g;
const mathStartReplace = `<div class="${mathContainerClass}"><math`;
const mathEndMatch = /<\/math>/g;
const mathEndReplace = '</math></div>';

export const wrapMath = function (html) {
  html = replace(html, mathStarMatch, mathStartReplace);
  html = replace(html, mathEndMatch, mathEndReplace);
  return html;
};

export const processMathHtml = function (parentElement) {
  if (browserType === IE || browserType === EDGE) {
    parentElement.querySelectorAll('.' + mathContainerClass).forEach(c => {
      //yeah somehow this solves the issue. see the comments above.
      let html = c.innerHTML;
      c.innerHTML = html;
    });
  }

  parentElement.querySelectorAll('.' + mathTexContainerClass).forEach(c => {
    /**
     * There are four kinds of html tags described in the MathJax docs, titled as follows
     * \href{url}{math}
     * \class{name}{math}
     * \cssId{id}{math}
     * \style{css}{math}
     *
     * 'href' was certainly questionable, so the others may be too.
     * We just replace them all, students do not need these things.
     */
    c.innerHTML = replace(c.innerHTML, /\\(href|class|cssId|style)(\{.*})?/g, '');
  });
};

export const queueMathTypeset = function (element) {
  window.MathJax.startup.promise = window.MathJax.startup.promise
    .then(() => window.MathJax.typesetPromise([element]))
    .catch(e => console.log(e));
};
