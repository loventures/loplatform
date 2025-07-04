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

import { isEmpty, cloneDeep } from 'lodash';

/**
 *  This will be where we register directives that are going to render small
 *  sub-view elements for our assets.  For example a section directive could
 *  just render the section as a small subheading without any child information.
 *
 *  The common use will be to choose which directive will render audio, video
 *  or little helper html handlers.  Note that this is NOT for chaining assets
 *  and instead is supposed to be "render this asset please."
 */
export default angular
  .module('cbl.richContent.contentLoader', [])
  .provider('ContentDirectiveFactory', function () {
    var CDF = {
      registry: {}, //In primatives.js in the content directory
    };

    this.register = function (types) {
      if (!isEmpty(types)) {
        CDF.registry = types;
      }
    };

    this.addType = function (type, cfg) {
      CDF.registry[type] = cfg;
    };

    this.$get = [
      '$compile',
      function ($compile) {
        CDF.createContentType = function (content) {
          if (content && CDF.registry[content.partType]) {
            return $compile(document.createElement(CDF.registry[content.partType])); // eslint-disable-line angular/document-service
          } else {
            return function (s) {
              const div = document.createElement('div');
              div.innerText = 'Unknown Type: ' + s.content.partType;
              return div;
            };
          }
        };
        return CDF;
      },
    ];
  })
  .directive('contentDirectiveLoader', [
    'ContentDirectiveFactory',
    function (ContentDirectiveFactory) {
      return {
        restrict: 'AE',
        scope: {
          content: '=content', //A content block that we want to render.
          parentAsset: '=?', //Modifiers... or something
        },
        link: function (scope, elem) {
          scope.build = function () {
            let childScope = scope.$new();
            let content = scope.shimContentKeys(cloneDeep(scope.content));
            childScope.content = content;
            elem.html(ContentDirectiveFactory.createContentType(content)(childScope));
          };

          //Remove once we stop using/make more consistent usage of contentPartInterface - see CBLPROD-9218
          scope.shimContentKeys = function (content) {
            content.parts = content.parts || content.contentParts;
            content.partType = content.partType || content.contentPartType;
            content.partProperties = content.partProperties || content.contentPartProperties;
            content.renderedHtml = content.renderedHtml || content.html;
            return content;
          };

          scope.$watch('content', function () {
            if (!isEmpty(scope.content) && scope.content) {
              scope.build();
            }
          });
        },
      };
    },
  ]);

const component = {
  bindings: {
    part: '<',
  },
  template: `
        <content-directive-loader
            content="$ctrl.part"
        ></content-directive-loader
    `,
};

import { angular2react } from 'angular2react';
export let ContentPartPlayer = 'ContentPartPlayer: ng module not included';
angular
  .module('cbl.richContent.contentLoader')
  .component('contentPartPlayer', component)
  .run([
    '$injector',
    function ($injector) {
      ContentPartPlayer = angular2react('contentPartPlayer', component, $injector);
    },
  ]);
