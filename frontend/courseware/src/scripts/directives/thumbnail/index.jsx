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

import { each, extend } from 'lodash';

angular
  .module('lo.directives')
  /**
   * @ngdoc service
   * @name thumbnailService
   * @memberof lo.directives
   * @description
   *  This service finds img elements in html and add a thumbnail directive to it
   *  Also provides methods for thumbnail directive to use
   */
  .factory('thumbnailService', [
    '$uibModal',
    'Settings',
    function ($uibModal, Settings) {
      /** @alias thumbnailService **/
      var thumbnailService = {};

      /**
       * @description
       *  To be used where html is being processed and images needs to be thumnailized.
       * @params {String} str the html that needs processing
       * @params {string} size in ['small', 'large'] keyword for the size of the eventual thumbnail
       * @params {string} action in ['inline', 'modal'] keyword for the method of expanding to original
       */
      thumbnailService.thumbnailize = function (str, size, action) {
        if (!Settings.isFeatureEnabled('UseThumbnailize')) {
          return str;
        }

        if (typeof size == 'boolean') {
          size = size ? 'small' : 'large';
        }
        if (!size || !thumbnailService.sizes[size]) {
          size = 'large';
        }
        return thumbnailService.bindDirective(str, size + '-' + (action || 'inline'));
      };

      thumbnailService.bindDirective = function (str, value) {
        if (!str || str.indexOf('img') === -1) {
          return str;
        }
        try {
          var parser = new DOMParser();
          var html = parser.parseFromString(str, 'text/html');
          each(html.getElementsByTagName('img'), function (el) {
            el.setAttribute('thumbnail', value);
          });
          return html.getElementsByTagName('body')[0].innerHTML;
        } catch (e) {
          console.warn('DOMParser Thumbnailize Failed');
          if (str.match(new RegExp('<img thumbnail="'))) {
            return str;
          }
          return str.replace(/<img +/g, '<img thumbnail="' + value + '" ');
        }
      };

      thumbnailService.sizes = {
        large: {
          width: 128,
          height: 128,
        },
        medium: {
          width: 64,
          height: 64,
        },
        small: {
          width: 32,
          height: 32,
        },
      };

      thumbnailService.actions = {
        inline: {
          expand: function (scope, element) {
            scope.expanded = !scope.expanded;

            if (scope.expanded) {
              element.css('max-width', '');
              element.css('max-height', '');
            } else {
              element.css('max-width', scope.maxWidth);
              element.css('max-height', scope.maxHeight);
            }
          },
        },
        modal: {
          expand: function (scope) {
            $uibModal.open({
              template: '<div class="modal-body"><img src="' + scope.src + '"></img></div>',
            });
          },
        },
      };

      thumbnailService.getOptions = function (thumbnailConfig) {
        thumbnailConfig = thumbnailConfig || 'large-inline';
        var options = {};
        var config = thumbnailConfig.split('-');

        if (config[0].match('^[a-zA-Z]*$')) {
          extend(options, thumbnailService.sizes[config[0]]);
        } else if (config[0].match('^[0-9]*x[0-9]*$')) {
          var size = config[0].split('x');
          extend(options, {
            width: parseInt(size[0], 10),
            height: parseInt(size[1], 10),
          });
        }

        if (config[1] && config[1].match('[a-zA-Z]')) {
          extend(options, thumbnailService.actions[config[1]]);
        }

        return options;
      };

      return thumbnailService;
    },
  ])
  .directive('thumbnail', [
    'thumbnailService',
    function (thumbnailService) {
      return {
        restrict: 'A',
        template:
          '<div ng-class="{expanded:expanded, expandable: expandable}" ' +
          'class="fake_thumbnail" ng-click="expand()">' +
          '<span class="icon icon-plus"></span>' +
          '<span class="icon icon-minus"></span>' +
          '<img ng-src="{{src}}" />' +
          '</div>',
        replace: true,
        priority: 10,
        scope: {
          src: '@',
        },
        controller: [
          '$scope',
          function ($scope) {
            this.redraw = function (src) {
              $scope.src = src;
            };
          },
        ],
        link: function (scope, element, attr) {
          var options = thumbnailService.getOptions(attr.thumbnail);
          scope.expanded = false;

          scope.setExpandable = function (isSmallImage) {
            scope.expandable = !attr.noExpand && !isSmallImage;
          };

          scope.maxWidth = options.width;
          scope.maxHeight = options.height;
          element.css('max-width', scope.maxWidth);
          element.css('max-height', scope.maxHeight);

          scope.expand = function () {
            if (!scope.expandable) {
              return;
            }
            options.expand(scope, element);
          };

          var natural = function (imgElement) {
            var img = new Image();
            img.src = imgElement.src;

            return {
              width: img.width,
              height: img.height,
            };
          };

          scope.onload = function (imgElement) {
            var p = element.parent('p');
            if (p && p.css('text-align') != 'center') {
              //CBLPROD-212, don't block images in center
              p.css('display', 'inline-block').css('margin-right', '4px'); //This is probably RT?
            }
            var naturalSize = natural(imgElement);

            scope.naturalHeight = imgElement.naturalHeight || naturalSize.height;
            scope.naturalWidth = imgElement.naturalWidth || naturalSize.width;

            scope.setExpandable(
              scope.naturalWidth <= scope.maxWidth || scope.naturalHeight <= scope.maxHeight
            );
          };

          var imgSelect = element.find('img');
          imgSelect.bind('load', function () {
            scope.$apply(function () {
              scope.onload(imgSelect[0]);
            });
          });
        },
      };
    },
  ]);
