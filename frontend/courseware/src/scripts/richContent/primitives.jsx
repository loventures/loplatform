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

import { trackImgCSSAlterationCompile, trackPrimitivesUsage } from '../analytics/trackEvents.js';
// import $ from 'jquery';
import { each, isEmpty } from 'lodash';

import carouselTmpl from './carousel.directive.html';
import embeddedPrimitiveTmpl from './embeddedPrimitive.html';

export default angular
  .module('cbl.richContent.primitives', [])
  .config([
    'ContentDirectiveFactoryProvider',
    function (ContentDirectiveFactoryProvider) {
      ContentDirectiveFactoryProvider.register({
        html: 'content-html',
        paragraph: 'content-html',
        video: 'content-video',
        audio: 'content-audio',
        image: 'content-image',
        mediaGallery: 'content-carousel',
        carousel: 'content-carousel',
        caseStudy: 'case-study',
        url: 'content-url',
        richContent: 'content-html',
        embeddable: 'content-embeddable',
      });
    },
  ])
  .directive('contentHtml', function () {
    //this purposefully exclude % because a percentage dimension
    //does not tell us anything meaningful about its size
    const widthMatcher = /width:\s?(\d+)([a-z]+)\s?(;|$)/;
    const heightMatcher = /height:\s?(\d+)([a-z]+)\s?(;|$)/;
    const attrMatcher = /\s?(\d+)([a-z]+)\s?/;

    const getPropFromString = (string, matcher) => {
      const match = string && string.match(matcher);
      return match && +match[1];
    };

    const d = {
      restrict: 'AE',
      replace: true,
      template: '<div class="content-html" compile="content.renderedHtml"></div>',
      link: function ($scope, $element) {
        const addImagePlaceholder = (idx, elem) => {
          const img = angular.element(elem);

          if (img.attr('image-loaded') === 'true') {
            return;
          }

          const heightAttr = getPropFromString(img.attr('height'), attrMatcher);
          const heightCss = getPropFromString(img.attr('style'), heightMatcher);
          const widthAttr = getPropFromString(img.attr('width'), attrMatcher);
          const widthCss = getPropFromString(img.attr('style'), widthMatcher);

          const height = heightAttr || heightCss;
          const width = widthAttr || widthCss;

          if (!height || !width || width <= 0) {
            return;
          }

          //only apply if some css has been applied indicated the desired size
          if (elem.width <= 0) {
            return;
          }

          const ratio = height / width;
          const renderedWidth = elem.width;
          const responsiveHeight = ratio * renderedWidth;

          img.css('height', responsiveHeight);

          img.on('load', function () {
            img.css('height', heightCss || 'auto');
            img.attr('image-loaded', 'true');
          });
          trackImgCSSAlterationCompile();
        };

        $element.ready(() => {
          $element.find('img').each(addImagePlaceholder);
        });
        trackPrimitivesUsage('contentHtml');
      },
    };
    return d;
  })
  .directive('contentText', function () {
    var d = {
      restrict: 'AE',
      replace: true,
      template: '<span class="content-text" compile="content.text"></span>',
      link: function () {
        trackPrimitivesUsage('contentText');
      },
    };
    return d;
  })
  .directive('contentVideo', function () {
    var d = {
      restrict: 'AE',
      replace: true,
      template: '<div class="content-video"> VIDEO content (use other directive)</div>',
      link: function () {
        trackPrimitivesUsage('contentVideo');
      },
    };
    return d;
  })
  .directive('contentAudio', function () {
    var d = {
      restrict: 'AE',
      replace: true,
      template: '<div class="content-audio"> audio (use other directive)</div>',
      link: function () {
        trackPrimitivesUsage('contentAudio');
      },
    };
    return d;
  })
  .directive('contentCarousel', [
    '$timeout',
    function ($timeout) {
      var d = {
        restrict: 'AE',
        replace: true,
        template: carouselTmpl,
        link: function (scope, element) {
          //TODO: Figure out the DCM version
          if (!isEmpty(scope.content)) {
            var slides = scope.content.parts;

            //Deal with content part mappings
            each(slides, function (s, idx) {
              s.id = idx;
              s.imageCaption = s.caption;
              s.image = s.renderedHtml;
              s.alt = s.altText;
              s.thumbnail = s.renderedHtml;
              //s.html = $sce.trustAsHtml(s.renderedHtml);
            });
            scope.slides = slides;
          }

          scope.getCaption = function () {
            var activeSlide = scope.slides[scope.carousel.activeSlide];
            return activeSlide ? activeSlide.imageCaption || '' : '';
          };

          scope.carousel = {
            activeSlide: 0,
          };

          scope.selectThumb = function (slide, $event) {
            if ($event) {
              $event.stopPropagation();
              $event.preventDefault();
            }

            //GREATER HACK this is trying to trigger
            //uib-carousel's own slide transition, which means
            //it will use the animation queue uib-carousel comes with
            var slideElem = element.find('.carousel-indicators > li')[slide.id];
            $timeout(() => angular.element(slideElem).click());
          };
          trackPrimitivesUsage('contentCarousel');
        },
      };
      return d;
    },
  ])
  .directive('contentImage', function () {
    var d = {
      restrict: 'AE',
      replace: true,
      template:
        '<div class="content-image">' +
        '<div ng-if="content.src" class="content-src"></div>' +
        '<div ng-if="content.html" compile="content.html"></div>' +
        '</div>',
      link: function (scope, elem) {
        //If you just do a nice simple '<img src="{{content.src}}" /> it will screw up and try and load
        //empty strings and other odd template compile issues.   Sooooo, this.
        console.log('Scope', scope.content, scope.content ? scope.content.src : 'NO CONTENT');
        if (scope.content && !isEmpty(scope.content.src) && typeof scope.content.src === 'string') {
          $('.content-src', elem).append($('<img src="' + scope.content.src + '" >'));
        }
        trackPrimitivesUsage('contentImage');
      },
    };
    return d;
  })
  .directive('contentCaseStudy', function () {
    var d = {
      restrict: 'AE',
      replace: true,
      template: '<div class="content-case-study"> Case Study </div>',
      link: function () {
        trackPrimitivesUsage('contentCaseStudy');
      },
    };
    return d;
  })
  .directive('contentUrl', function () {
    var d = {
      restrict: 'AE',
      replace: true,
      template:
        '<div class="content-url">' +
        '<a href="{{content.url}} >{{content.title || "link"</a>' +
        '</div>',
      link: function () {
        trackPrimitivesUsage('contentUrl');
      },
    };
    return d;
  })
  .directive('contentEmbeddable', function () {
    var d = {
      restrict: 'AE',
      replace: true,
      template: embeddedPrimitiveTmpl,
      link: function () {
        trackPrimitivesUsage('contentEmbeddable');
        console.error('Embedding content item not supported');
      },
    };
    return d;
  });
