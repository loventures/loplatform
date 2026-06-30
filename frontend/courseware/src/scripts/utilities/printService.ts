/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import $ from 'jquery';

import { ContentLite } from '../api/contentsApi.ts';
import { openConfirmModal } from '../directives/modalHost/ConfirmModal.tsx';
import { trackPrintEvent } from '../analytics/trackEvents.js';

/**
 * Pure-TS port of the AngularJS `Print` service (was `utilities/print.jsx`'s PrintContentUtils +
 * PrintMethodUtils + Print). Angular deps are dropped: `angular.element` → jQuery (`$`), `$window` →
 * `window`, `$timeout` → `setTimeout`, `reactModal.confirm` → the pure `openConfirmModal`.
 *
 * `$timeout` previously also queued an Angular digest so print *templates* would re-render before the DOM
 * was cloned; `window.inPrintMode` is now read only by React components (which don't re-render off the
 * global anyway), so the digest was vestigial — a plain `setTimeout` is equivalent.
 */

const W = window as any;

// ---- DOM / HTML preparation (was PrintContentUtils) ----

const removeScriptsFrom = ($element: any) => {
  $element.find('script,embed').remove();
};

const iframeContentToData = ($element: any) => {
  const iframes = $element.find('iframe');
  for (let i = 0; i < iframes.length; i++) {
    try {
      const iframe = $(iframes[i]);
      const iframeDom = $(iframe[0].contentWindow.document.documentElement);
      removeScriptsFrom(iframeDom);
      iframe.data('html', iframeDom.html());
    } catch (e) {
      console.error('error manipulating iframe \n', e);
    }
  }
};

const undoIframeContentToData = ($element: any) => {
  const iframes = $element.find('iframe');
  for (let i = 0; i < iframes.length; i++) {
    $(iframes[i]).data('html', null);
  }
};

const replaceIframesWithData = ($element: any) => {
  const iframes = $element.find('iframe');
  for (let i = 0; i < iframes.length; i++) {
    const iframe = $(iframes[i]);
    iframe.after(iframe.data('html'));
    iframe.remove();
  }
};

const fixPageBreaks = ($element: any) => {
  // Prevent the page breaking inline for <p> (and <img>, since some put <img> inside <p>).
  $element.find('p').css('page-break-inside', 'avoid');
  // Only works for block images; inline images need this on their container.
  $element.find('img').css('page-break-inside', 'avoid');
};

const preparePrintingHtml = ($element: any, refToMain = 'parent') => {
  // When cloning an iframe, loaded iframe contents don't get copied; capture the current iframe html as
  // data so it can be copied, then remove it after.
  iframeContentToData($element);
  const $clonedElement = $element.clone(true, true);
  undoIframeContentToData($element);
  replaceIframesWithData($clonedElement);

  removeScriptsFrom($clonedElement);
  fixPageBreaks($clonedElement);

  const content = $clonedElement[0].outerHTML;

  // The stylesheets live in <head>.
  const $headerElem = $('head').clone();
  removeScriptsFrom($headerElem);
  const header = ($headerElem[0] as HTMLElement).outerHTML;

  // onload/onafterprint give a reliable signal for when render/print is complete.
  const body = `
        <body class="prepare-print" onload="${refToMain}.doPrint(self)" onafterprint="${refToMain}.afterPrint(self)">
            ${content}
        </body>
    `;

  return header + body;
};

const detectUnprintable = ($element: any) => {
  const iframes = $element.find('iframe');
  for (let i = 0; i < iframes.length; i++) {
    const iframeDom = $((iframes[i] as HTMLIFrameElement).contentWindow!.document.documentElement);
    if (iframeDom.find('.unprintable').length > 0) {
      return true;
    }
  }
  return false;
};

// ---- iframe / window print execution (was PrintMethodUtils) ----

const createHiddenIframe = () =>
  $('<iframe class="printing-iframe"></iframe>')
    .css('visibility', 'hidden')
    .css('top', '99999px')
    .css('position', 'fixed')[0];

const removeIframes = () => {
  // jQuery .remove() doesn't work here for some reason.
  const iframes = $(W.document.body).find('.printing-iframe');
  for (let i = 0; i < iframes.length; i++) {
    W.document.body.removeChild(iframes[i]);
  }
};

W.doPrint = function (childWindow: any) {
  setTimeout(function () {
    childWindow.focus(); // required for IE

    if (W.SeleniumAfterPrint) {
      W.afterPrint();
      // Works better with iframe + IE, but FF doesn't support this.
    } else if (childWindow.document.queryCommandSupported('print')) {
      childWindow.document.execCommand('print', false, null);
    } else {
      childWindow.print();
    }
  }, 100); // some delay to let things settle
};

W.afterPrint = function () {
  W.printCallback?.();
  delete W.printCallback;
};

const writeToDocument = (newDocument: Document, printHtml: string) => {
  newDocument.open();
  newDocument.write(printHtml);
  newDocument.close();
};

const printInHiddenIframe = (printHtml: string) => {
  setTimeout(() => {
    const printFrame = createHiddenIframe() as HTMLIFrameElement;
    W.document.body.appendChild(printFrame);
    writeToDocument(printFrame.contentWindow!.document, printHtml);
  }, 100);
};

const printInNewWindow = (printHtml: string): void => {
  const newWindow = W.open('', 'print window');

  if (newWindow && !newWindow.closed && newWindow.document) {
    writeToDocument(newWindow.document, printHtml);
  } else {
    openConfirmModal({
      message: 'PRINTING_POPUP_BLOCKED_MESSAGE',
      confirmButton: 'PRINTING_POPUP_BLOCKED_CONTINUE',
      cancelButton: 'PRINTING_POPUP_BLOCKED_ABORT',
    } as any).then(() => printInNewWindow(printHtml));
  }
};

// ---- public API (was Print) ----

const prepareHtml = (containerSelect = '#course-app', refToMain = 'parent') =>
  preparePrintingHtml($(containerSelect), refToMain);

export interface PrintService {
  isUnprintable: (containerSelect?: string) => boolean;
  print: (containerSelect: string | undefined, content: ContentLite, callback?: () => void) => void;
  printViewPrint: (
    containerSelect: string | undefined,
    content: ContentLite,
    callback?: () => void
  ) => () => void;
  printNewWindow: (containerSelect: string | undefined, content: ContentLite) => void;
}

export const printService: PrintService = {
  isUnprintable(containerSelect = '#course-app') {
    return detectUnprintable($(containerSelect));
  },

  print(_containerSelect, content, callback) {
    trackPrintEvent(content.typeId, content.id);

    // Flag print mode and let print templates render before preparing the html, then clear it.
    W.inPrintMode = true;
    W.printCallback = callback;
    setTimeout(() => {
      const html = prepareHtml(_containerSelect, 'parent');
      W.inPrintMode = false;
      printInHiddenIframe(html);
    }, 100);
  },

  // Avoids the `inPrintMode` delay above because inPrintMode is forced on in the print view.
  printViewPrint(_containerSelect, content, callback) {
    trackPrintEvent(content.typeId, content.id);
    W.printCallback = callback; // for the win
    const html = prepareHtml(_containerSelect, 'parent');
    printInHiddenIframe(html);
    return () => removeIframes();
  },

  printNewWindow(_containerSelect, content) {
    trackPrintEvent(content.typeId, content.id);
    const html = prepareHtml(_containerSelect, 'opener');
    printInNewWindow(html);
  },
};

export default printService;
