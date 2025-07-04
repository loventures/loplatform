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

import template from './mpQuestionNav.html';

export default angular.module('lo.quizViews.mpQuestionNav', []).component('mpQuestionNav', {
  template,
  bindings: {
    questionTuples: '<',
    currentIndex: '<',
    goto: '<',
    canGoStatus: '<',
  },
  controller: [
    '$element',
    '$translate',
    function ($element, $translate) {
      this.$onChanges = ({ currentIndex }) => {
        if (currentIndex && currentIndex.currentValue >= 0) {
          this.centerOnNewPage(currentIndex.currentValue);
        }
      };

      this.centerOnNewPage = function (newPage) {
        const navElement = $element.find('.pagination')[0];
        const pageElement = $element.find('li').filter(function (idx, li) {
          return angular.element(li).find('.page-link').text() === '' + (newPage + 1);
        })[0];

        if (!navElement || !pageElement) {
          return;
        }

        const navElementDimensions = navElement.getBoundingClientRect();
        const pageElementDimensions = pageElement.getBoundingClientRect();
        const navElementCenter = navElementDimensions.left + navElementDimensions.width / 2;
        const pageElementCenter = pageElementDimensions.left + pageElementDimensions.width / 2;

        if (pageElementCenter !== navElementCenter) {
          navElement.scrollLeft += pageElementCenter - navElementCenter;
        }
      };

      this.getPageLabel = index => {
        const pageNumber = index + 1;
        if (this.currentIndex === index) {
          return $translate.instant('CURR_QUESTION', { pageNumber });
        } else {
          return $translate.instant('GOTO_QUESTION', { pageNumber });
        }
      };
    },
  ],
});
