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

import { get, isEmpty } from 'lodash';
import { getHotspotImageUrl } from '../../../utilities/assetRendering.js';
import domainUrl from '../../../utilities/domainUrl.js';
import { SELECTION_TYPE_HOTSPOT } from '../../../utilities/questionTypes.js';

import template from './hotspotQuestion.html';
import baseViewTemplate from './hotspotQuestionBaseView.html';
import printViewTemplate from './hotspotQuestionPrintView.html';

const getImageUrl = question => {
  if (question.imageUrl) {
    return domainUrl + question.imageUrl;
  } else if (question.image) {
    return getHotspotImageUrl(question.image.nodeName);
  }
};

const hotspotQuestionCtrl = [
  '$element',
  'HotspotQuestionImageCache',
  function ($element, HotspotQuestionImageCache) {
    this.selectionRadius = 10;

    this.$onChanges = ({ question, response }) => {
      if (question && question.currentValue) {
        this.setupPaper(getImageUrl(question.currentValue));
      }

      if (response && response.currentValue) {
        this.selection = this.responseToSelection(response.currentValue);
      }
    };

    this.setImageDetails = image => {
      this.imageWidth = image.width;
      this.imageHeight = image.height;
      this.imageUrl = image.src;
      this.imageLoaded = true;

      const maxPrintImageSize = 5 * 96; //assume 5inches and 96DPI
      const printScale =
        this.imageWidth > maxPrintImageSize ? maxPrintImageSize / this.imageWidth : 1;

      this.printFriendlyWidth = this.imageWidth * printScale;
      this.printFriendlyHeight = this.imageHeight * printScale;
    };

    this.setupPaper = imageUrl => {
      this.image = HotspotQuestionImageCache.get(this.question.id);

      if (!this.image) {
        this.image = new window.Image();
        angular.element(this.image).on('load', () => {
          this.setImageDetails(this.image);
          HotspotQuestionImageCache.add(this.question.id, this.image);
        });
        this.image.src = imageUrl;
      } else {
        this.setImageDetails(this.image);
      }
    };

    this.responseToSelection = response => {
      const point = get(response, 'selection.point', null);
      this.hasSelection = !isEmpty(point);
      return { ...point };
    };

    this.selectionToResponse = point => {
      const selection = this.response.selection || {
        responseType: SELECTION_TYPE_HOTSPOT,
      };
      return {
        ...this.response,
        selection: {
          ...selection,
          point,
        },
      };
    };

    this.getLocation = $event => {
      //Normally these properties are copied over from the browser mouse event
      //to the jquery event, sometimes (like in firefox's case) they are not
      const pageX = $event.pageX || $event.originalEvent.pageX;
      const pageY = $event.pageY || $event.originalEvent.pageY;
      const { left, top } = $element.find('svg').offset();
      return {
        // Browser compatibility for calculation of center point
        x: pageX - left,
        y: pageY - top,
      };
    };

    this.clickOnPaper = $event => {
      if (!this.canEditAnswer) {
        return;
      }
      const point = this.getLocation($event);
      this.changeAnswer(this.index, this.selectionToResponse(point));
    };
  },
];

export default angular
  .module('lo.questions.hotspotQuestion', [])
  .service('HotspotQuestionImageCache', function HotspotQuestionImageCache() {
    var HotspotQuestionImageCache = {
      cache: {},
    };

    HotspotQuestionImageCache.add = (questionId, image) => {
      HotspotQuestionImageCache.cache[questionId] = image;
    };
    HotspotQuestionImageCache.get = questionId => {
      return HotspotQuestionImageCache.cache[questionId];
    };

    return HotspotQuestionImageCache;
  })
  .component('hotspotQuestion', {
    bindings: {
      index: '<',
      assessment: '<?',
      questionCount: '<?',
      question: '<',
      response: '<',
      score: '<',
      changeAnswer: '<',
      canEditAnswer: '<',
    },
    template,
    controller: [
      '$window',
      function ($window) {
        this.inPrintMode = () => $window.inPrintMode;
      },
    ],
  })
  .component('hotspotQuestionBaseView', {
    bindings: {
      index: '<',
      assessment: '<?',
      questionCount: '<?',
      question: '<',
      response: '<',
      score: '<',
      changeAnswer: '<',
      canEditAnswer: '<',
    },
    template: baseViewTemplate,
    controller: hotspotQuestionCtrl,
  })
  .component('hotspotQuestionPrintView', {
    bindings: {
      index: '<',
      assessment: '<?',
      questionCount: '<?',
      question: '<',
      response: '<',
      score: '<',
      changeAnswer: '<',
      canEditAnswer: '<',
    },
    template: printViewTemplate,
    controller: hotspotQuestionCtrl,
  });
