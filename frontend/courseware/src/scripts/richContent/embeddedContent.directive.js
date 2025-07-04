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

import { bindComponent, component } from './embeddedContent.js';
import { get, isNumber } from 'lodash';
import { setFullscreenActionCreator } from '../courseContentModule/actions/contentPageActions.js';
import browserType, { SAFARI } from '../utilities/browserType.js';

import tmpl from './embeddedContent.html';

// preserve initial height between page navs for less jank
var initialHeight = null;

export default angular.module('cbl.richContent.embeddedContent', []).directive('embeddedContent', [
  '$interval',
  '$timeout',
  '$window',
  '$ngRedux',
  'Scroller',
  function ($interval, $timeout, $window, $ngRedux, Scroller) {
    return {
      restrict: 'E',
      scope: {
        url: '=',
        title: '=',
        rawHtml: '=?',
        expandable: '=',
        contentWidth: '=?', // if specified with height defines iframe aspect ratio
        contentHeight: '=?', // if specified without height defines fixed iframe height
        onLoaded: '=?',
        printView: '=?',
      },
      template: tmpl,
      link: function (scope, elem) {
        scope.canExpand = get(scope, 'expandable', true);

        $ngRedux.connectToScope(state => {
          setTimeout(() => scope.$digest(), 0);
          return {
            fullscreen: state.ui.fullscreenState.fullscreen,
          };
        })(scope);

        scope.currentHeight = 0;
        scope.initialHeight = `height: ${initialHeight ?? '50vh'}`;

        //consider it stable if the height does not change for 1000ms
        scope.resizingStableThreshold = 1000;

        scope.setStable = stable => {
          scope.isStable = stable;
          angular.element(scope.frame).attr('resizing-stable', stable);

          //If we've stabilized, we've decided the iframe is 'done' - no need to poll anymore
          if (stable) {
            $interval.cancel(scope.pollForSize);
          }
        };

        scope.checkStable = () => {
          const now = new Date().valueOf();
          if (now - scope.latestUnstable > scope.resizingStableThreshold) {
            scope.setStable(true);
            if (!scope.printView) scope.onLoaded?.();
          }
        };

        scope.unStable = () => {
          scope.latestUnstable = new Date().valueOf();
          if (scope.isStable) {
            scope.setStable(false);
          }
        };

        //called in response to scroll height changes to allow content to resize appropriately
        scope.adjustHeight = scrollHeight => {
          if (!scope.contentHeight) {
            if (!isNumber(+scrollHeight)) {
              scrollHeight = 1;
            }
            initialHeight = `${Math.min(scrollHeight, window.innerHeight)}px`;

            //potential edge case fix, certain content will cause a horizontal scrollbar to show up
            //within the iframe which may cause the vertical scrollbar to show up if the content
            //intrudes on the space of the horizontal scrollbar.  Add this to the adjustment to prevent this.
            //bullshit
            const horizScrollbarHeight = 0;//15;

            if (scope.currentHeight + horizScrollbarHeight < scrollHeight) {
              console.log('increasing height to:', scrollHeight);
              scope.currentHeight = scrollHeight;

              scope.frame.css('height', scrollHeight + horizScrollbarHeight + 'px');

              scope.unStable();
              return;
            } else if (scope.currentHeight > scrollHeight) {
              let ratio = scrollHeight / scope.currentHeight;
              if (ratio < 0.8) {
                //don't shrink all the time to prevent trashing...
                console.log('decreasing height to:', scrollHeight);
                scope.currentHeight = scrollHeight;
                scope.frame.css('height', scrollHeight + 'px');

                scope.unStable();
                return;
              }
            }

            scope.checkStable();
          }
        };

        //function used to make sure content respects a fixed height or aspect ratio IF fullscreen is not currently enabled
        scope.updateHeightIfFixed = () => {
          if (scope.contentHeight) {
            if (scope.fullscreen) {
              scope.frame.css('height', '100%');
              scope.frame.css('aspect-ratio', undefined);
            } else if (scope.contentWidth) {
              scope.frame.css('height', '100%');
              scope.frame.css('aspect-ratio', `${scope.contentWidth} / ${scope.contentHeight}`);
            } else {
              scope.frame.css('height', `${scope.contentHeight}px`);
            }
          }
        };

        scope.refreshHeight = () => {
          if (!scope.contentWindow.document.documentElement) {
            return;
          }

          const scrollHeight = scope.contentWindow.document.documentElement.scrollHeight;
          scope.adjustHeight(scrollHeight);
        };

        scope.onIframeReady = function () {
          // In print view we call onloaded once the frame is ready, not once resizing
          // has stabilized, so module print can do some parallel work.
          if (scope.printView) scope.onLoaded?.();

          if (scope.rawHtml) {
            scope.contentWindow.document.open();
            scope.contentWindow.document.write(scope.rawHtml);
            scope.contentWindow.document.close();
          }

          scope.latestUnstable = new Date().valueOf();

          //Core resize functionality - base functionality that works for all frames
          scope.pollForSize = $interval(function () {
            // poll even when in full screen.
            const contentHeight = Math.max(
              get(scope, 'contentWindow.document.documentElement.scrollHeight', 0),
              get(scope, 'contentWindow.document.body.scrollHeight', 0)
            );

            scope.adjustHeight(contentHeight);
          }, 400);

          //Fancy resize - adds listeners on the iframe and disables scrolling. Content can trigger/manage resize if it interacts with iFrameResizer.
          //http://davidjbradshaw.github.io/iframe-resizer/
          if (window.iFrameResize) {
            const heightCalculationMethod =
              browserType === SAFARI
                ? // 'lowestElement' is very slow, only use for safari where documentElementOffset doesnt work
                  'lowestElement'
                : //default calculation 'bodyOffset' cuts frame off slightly
                  'documentElementOffset';
            scope.iFrameResizer = window.iFrameResize(
              { heightCalculationMethod },
              scope.frame[0]
            )[0].iFrameResizer;
          }

          scope.$on('$destroy', function () {
            $interval.cancel(scope.pollForSize);
            scope.iFrameResizer?.removeListeners();
          });
        };

        scope.onInitFrame = function () {
          scope.frameContainer = elem.find('.iframe-content');
          scope.frame = elem.find('iframe');
          scope.contentWindow = scope.frame[0].contentWindow;

          scope.updateHeightIfFixed();

          try {
            angular.element(scope.contentWindow.document).ready(scope.onIframeReady);
          } catch (e) {
            //usually cross frame error
            //probably because this is a pdf file server via amazon

            //11inch for 1 common page height
            scope.frameContainer.css('height', '11in');
          }
          scope.frameContainer[0].addEventListener('scroll', scope.scrollListener, false);

          scope.$on('$destroy', () => {
            scope.frameContainer[0].removeEventListener('scroll', scope.scrollListener);
          });
        };

        scope.scrollingElement = document.documentElement; //default document, becomes iframe container when expando'd

        scope.$watch('fullscreen', newFullscreenValue => {
          if (typeof newFullscreenValue === 'boolean') {
            scope.scrollingElement = newFullscreenValue
              ? scope.frameContainer[0]
              : document.documentElement;
            scope.scrollListener(); //make sure this is updated after the screen changes
            scope.updateHeightIfFixed(); //set-or-unset fixed height if necessary
            try {
              scope.contentWindow.postMessage(
                { fn: 'onFullScreen', arg0: newFullscreenValue },
                '*'
              );
            } catch (e) {
              // in case this is prohibited
            }
            if (newFullscreenValue) setTimeout(() => elem.find('#exit-full-screen')?.focus(), 0);
          }
        });

        scope.setFullscreen = function (fullscreen) {
          $ngRedux.dispatch(setFullscreenActionCreator(fullscreen));
          scope.refreshHeight();
        };

        /**
         * API for communicating between the app and content frame.
         * App
         *  sends "onFullScreen" message with state to content window when fullscreen is toggled/set
         *
         * Content Window
         *  sends "scrollToTop" to request the browser scrolls to the top
         *  sends "setFullScreen" to dictate the fullscreen state from inside the content
         *  sends "getFullScreen" to ask for fullscreen state. The answer is sent using an "onFullScreen" message.
         * */

        const messageListener = event => {
          if (event.data && event.data.fn === 'setFullScreen') {
            $timeout(() => scope.setFullscreen(event.data.arg0), 0);
          } else if (event.data && event.data.fn === 'getFullScreen') {
            event.source.postMessage({ fn: 'onFullScreen', arg0: scope.fullscreen }, '*');
          } else if (event.data && event.data.fn === 'scrollToTop') {
            $timeout(() => Scroller.scrollTop(null));
          }
        };

        /**
         * Every time the page is scrolled, store this value to the scope
         *
         * We do this with a *slight* lag, because chrome wants to scroll to 0 just *before* the fullscreenElement
         * is set. With this minor lag, document.fullscreenElement will exist when the 0 event is processed,
         * preventing the value from being stored to scope.savedScrollTop.
         * */
        scope.scrollListener = () => {
          setTimeout(function () {
            //only save location if we're in regularscreen mode
            if (document.fullscreenElement == null) {
              scope.savedScrollTop = scope.scrollingElement.scrollTop;
            }
          }, 100);
        };

        /**
         * Fired every time after we either enter or leave fullscreen mode
         *
         * Used to restore scroll after exiting full screen
         * */
        const fullscreenListener = () => {
          //if back to regular, restore the scroll
          if (document.fullscreenElement == null && scope.savedScrollTop != null) {
            scope.scrollingElement.scrollTop = scope.savedScrollTop;
          }
        };

        $window.addEventListener('scroll', scope.scrollListener, false);
        $window.addEventListener('fullscreenchange', fullscreenListener, false);
        $window.addEventListener('message', messageListener, false);
        scope.$on('$destroy', () => {
          $window.removeEventListener('scroll', scope.scrollListener);
          $window.removeEventListener('fullscreenchange', fullscreenListener);
          $window.removeEventListener('message', messageListener);
        });
      },
    };
  },
]);

angular
  .module('cbl.richContent.embeddedContent')
  .component('embeddedContentForReact', component)
  .run([
    '$injector',
    function ($injector) {
      bindComponent($injector);
    },
  ]);
