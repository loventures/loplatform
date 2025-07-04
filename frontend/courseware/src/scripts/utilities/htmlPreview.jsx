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

import { forEach, isNil } from 'lodash';

/**
 * @description compile the html and return a string to display as preview
 * optionally pass in a list of selectors to remove form preview
 * @param {String} text the html text
 * @returns a string for display
 */
export default function htmlPreview(text) {
  if (isNil(text)) {
    return '';
  }

  var el = angular.element(angular.element.parseHTML(text));

  //If user manages to submit non-html text, don't break things
  if (isNil(el.html())) {
    return text;
  }

  //replace math
  const tobeReplaced = el.find('.math-tex');

  forEach(tobeReplaced, tbr => {
    angular.element(tbr).replaceWith('MATH_EQUATION_PLACEHOLDER');
  });

  return el
    .html()
    .replace(/MATH_EQUATION_PLACEHOLDER/g, '<span translate="MATH_EQUATION_PLACEHOLDER"></span>');
}
