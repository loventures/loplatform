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

import { map, sum } from 'lodash';

const totalHeight = selector => sum(map(angular.element(selector), e => e.offsetHeight));

export default angular.module('lo.utilities.Scroller', []).service('Scroller', function () {
  const scrollContainer = angular.element('body');

  const ScrollAnimationMS = 60;

  const scrollTop = element => {
    let targetTop;
    if (!element?.length) {
      // If scrolling to top of screen, accommodate app header
      targetTop = totalHeight('.er-page-header');
    } else {
      // Otherwise find the target offset and allow for any sticky elements that would overlap it
      const stickyHeight =
        totalHeight('.sticky-container-active') + totalHeight('.stuck .content-title');
      targetTop = Math.max(0, element.offset().top - stickyHeight);
    }
    // If I'm already scrolled above the target, don't scroll down.
    const scrollTop = Math.min(scrollContainer.scrollTop(), targetTop);
    scrollContainer.animate(
      {
        scrollTop,
      },
      ScrollAnimationMS
    );
  };

  return {
    scrollTop,
  };
});
