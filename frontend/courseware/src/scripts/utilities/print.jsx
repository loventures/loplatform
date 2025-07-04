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

import modal from 'angular-ui-bootstrap/src/modal';
import { trackPrintEvent } from '../analytics/trackEvents.js';

export default angular
  .module('lo.utilities.print', [modal])
  .service('PrintContentUtils', function () {
    const service = {};

    service.removeScriptsFrom = function ($element) {
      $element.find('script,embed').remove();
    };

    service.iframeContentToData = function ($element) {
      const iframes = $element.find('iframe');

      for (let i = 0; i < iframes.length; i++) {
        try {
          let iframe = angular.element(iframes[i]);
          let iframeDom = angular.element(iframe[0].contentWindow.document.documentElement);
          service.removeScriptsFrom(iframeDom);
          iframe.data('html', iframeDom.html());
        } catch (e) {
          console.error('error manipulating iframe \n', e);
        }
      }
    };

    service.undoIframeContentToData = function ($element) {
      const iframes = $element.find('iframe');
      for (let i = 0; i < iframes.length; i++) {
        let iframe = angular.element(iframes[i]);
        iframe.data('html', null);
      }
    };

    service.replaceIframesWithData = function ($element) {
      const iframes = $element.find('iframe');

      for (let i = 0; i < iframes.length; i++) {
        let iframe = angular.element(iframes[i]);
        iframe.after(iframe.data('html'));
        iframe.remove();
      }
    };

    service.fixPageBreaks = function ($element) {
      //This prevents the page breaking inline for <p>
      //and also for <img> in since some put <img> inside <p>
      $element.find('p').css('page-break-inside', 'avoid');

      //This only works for block images. For inline images, their container
      //needs to have this styles
      $element.find('img').css('page-break-inside', 'avoid');
    };

    service.preparePrintingHtml = function ($element, refToMain = 'parent') {
      //when we are cloning an iframe, loaded iframe contents don't get copied
      //we want the content in its current state, not reloading the content again,
      //so we set the current iframe html content as data on the iframe,
      //so it can be copied, and remove it after we copy it.
      service.iframeContentToData($element);
      //deep cloning to get data
      const $clonedElement = $element.clone(true, true);
      service.undoIframeContentToData($element);
      service.replaceIframesWithData($clonedElement);

      service.removeScriptsFrom($clonedElement);

      service.fixPageBreaks($clonedElement);

      const content = $clonedElement[0].outerHTML;

      //we need the style sheets in header
      const $headerElem = angular.element('head').clone();
      service.removeScriptsFrom($headerElem);
      const header = $headerElem[0].outerHTML;

      //onload for a reliable method to tell when render is complete
      const body = `
            <body class="prepare-print" onload="${refToMain}.doPrint(self)" onafterprint="${refToMain}.afterPrint(self)">
                ${content}
            </body>
        `;

      return header + body;
    };

    service.detectUnprintable = function ($element) {
      const iframes = $element.find('iframe');
      for (let i = 0; i < iframes.length; i++) {
        let iframeDom = angular.element(iframes[i].contentWindow.document.documentElement);
        if (iframeDom.find('.unprintable').length > 0) {
          return true;
        }
      }
      return false;
    };

    return service;
  })
  .service('PrintMethodUtils', [
    '$window',
    '$timeout',
    '$uibModal',
    function ($window, $timeout, $uibModal) {
      const service = {};

      service.createHiddenIframe = function () {
        return angular
          .element('<iframe class="printing-iframe"></iframe>')
          .css('visibility', 'hidden')
          .css('top', '99999px')
          .css('position', 'fixed')[0];
      };

      service.removeIframes = function () {
        //the jquery .remove() does not work for some reason
        const iframes = angular.element($window.document.body).find('.printing-iframe');
        for (var i = 0; i < iframes.length; i++) {
          $window.document.body.removeChild(iframes[i]);
        }
      };

      $window.doPrint = function (childWindow) {
        $timeout(function () {
          childWindow.focus(); //required for IE

          if (window.SeleniumAfterPrint) {
            $window.afterPrint();
            //This works better with iframe and IE, but FF does not support this
          } else if (childWindow.document.queryCommandSupported('print')) {
            childWindow.document.execCommand('print', false, null);
          } else {
            childWindow.print();
          }

          //childWindow.setTimeout(service.removeIframes);
        }, 100); //some delay to let things settle
      };
      $window.afterPrint = function () {
        $window.printCallback?.();
        delete $window.printCallback;
      };
      service.writeToDocument = function (newDocument, printHtml) {
        newDocument.open();
        newDocument.write(printHtml);
        newDocument.close();
      };

      service.printInHiddenIframe = function (printHtml) {
        return $timeout(() => {
          const printFrame = service.createHiddenIframe();

          $window.document.body.appendChild(printFrame);

          service.writeToDocument(printFrame.contentWindow.document, printHtml);
        }, 100);
      };

      service.printInNewWindow = function (printHtml) {
        const newWindow = $window.open('', 'print window');

        if (newWindow && !newWindow.closed && newWindow.document) {
          service.writeToDocument(newWindow.document, printHtml);
        } else {
          $uibModal
            .open({
              component: 'confirm-modal',
              resolve: {
                message: () => 'PRINTING_POPUP_BLOCKED_MESSAGE',
                confirmButton: () => 'PRINTING_POPUP_BLOCKED_CONTINUE',
                cancelButton: () => 'PRINTING_POPUP_BLOCKED_ABORT',
              },
            })
            .result.then(() => service.printInNewWindow(printHtml));
        }
      };

      return service;
    },
  ])
  .service('Print', [
    'PrintContentUtils',
    'PrintMethodUtils',
    '$timeout',
    '$window',
    function (PrintContentUtils, PrintMethodUtils, $timeout, $window) {
      const isUnprintable = function (containerSelect = '#course-app') {
        const $element = angular.element(containerSelect);
        return PrintContentUtils.detectUnprintable($element);
      };

      const prepareHtml = function (containerSelect = '#course-app', refToMain = 'parent') {
        const $element = angular.element(containerSelect);
        return PrintContentUtils.preparePrintingHtml($element, refToMain);
      };

      const print = function (containerSelect, content, callback) {
        trackPrintEvent(content.typeId, content.id);

        //Explicitly flag and queue a digest loop before preparing the html
        //so that any print templates will have time to render
        $window.inPrintMode = true;
        $window.printCallback = callback;
        return $timeout(() => {
          const html = prepareHtml(containerSelect, 'parent');
          $window.inPrintMode = false;
          return PrintMethodUtils.printInHiddenIframe(html);
        }, 100);
      };

      // This avoids the `inPrintMode` digest loop above because `inPrintMode` is forced on in printview
      const printViewPrint = function (containerSelect, content, callback) {
        trackPrintEvent(content.typeId, content.id);
        $window.printCallback = callback; // for the win
        const html = prepareHtml(containerSelect, 'parent');
        PrintMethodUtils.printInHiddenIframe(html);
        return () => PrintMethodUtils.removeIframes();
      };

      const printNewWindow = function (containerSelect, content) {
        trackPrintEvent(content.typeId, content.id);

        const html = prepareHtml(containerSelect, 'opener');

        return PrintMethodUtils.printInNewWindow(html);
      };

      return {
        isUnprintable,
        print,
        printViewPrint,
        printNewWindow,
      };
    },
  ]);
