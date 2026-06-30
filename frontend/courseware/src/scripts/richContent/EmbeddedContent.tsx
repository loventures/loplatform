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

import classNames from 'classnames';
import { get, isNumber } from 'lodash';
import React, { useEffect, useRef } from 'react';
import { useDispatch } from 'react-redux';

import { setFullscreenActionCreator } from '../courseContentModule/actions/contentPageActions.js';
import { useTranslation } from '../i18n/translationContext.tsx';
import { useCourseSelector } from '../loRedux';
import { scroller } from '../utilities/pure/scroller.ts';

// Preserve the last measured iframe height between page navs for less jank (was a module-level var in the
// Angular directive).
let initialHeight: string | null = null;

// Consider the iframe "stable" (done resizing) if its height hasn't changed for this long.
const RESIZING_STABLE_THRESHOLD = 1000;

export interface EmbeddedContentProps {
  url?: string;
  title?: string;
  rawHtml?: string;
  expandable?: boolean;
  /** With `contentHeight` defines a fixed aspect ratio; alone defines a fixed iframe height. */
  contentWidth?: number | null;
  contentHeight?: number | null;
  onLoaded?: () => void;
  printView?: boolean;
}

/**
 * Native React port of the Angular `embeddedContent` directive: the resource/SCORM/LTI/HTML content
 * iframe. Faithfully preserves the directive's behaviour —
 *  - a 400ms resize poll that grows/shrinks the iframe to its content's scrollHeight, with
 *    stability detection (height steady for 1000ms ⇒ "stable", which fires `onLoaded` and stops polling),
 *  - fixed-height / aspect-ratio handling when `contentHeight` is given,
 *  - the optional iframe-resizer (`window.iFrameResize`) integration,
 *  - fullscreen via the `fullscreenState` redux slice + the content postMessage protocol
 *    (`onFullScreen`/`setFullScreen`/`getFullScreen`/`scrollToTop`) + scroll save/restore,
 *  - `rawHtml` via `document.write`, and the cross-frame-error fallback (PDFs on S3) to a fixed height.
 *
 * `onLoaded` is how resource activities learn the content is shown — its timing is preserved exactly
 * (immediately in print view / once stable otherwise). The long-lived poll + window listeners read live
 * props/state through a ref so they never go stale. `$sce.trustAsResourceUrl` is dropped (React assigns
 * the iframe `src` directly). DOM preserved: `.embedded-content`(`.full-screen`), `.full-screen-container`,
 * `.iframe-content > iframe`.
 */
export const EmbeddedContent: React.FC<EmbeddedContentProps> = ({
  url,
  title,
  rawHtml,
  expandable,
  contentWidth,
  contentHeight,
  onLoaded,
  printView,
}) => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const fullscreen = useCourseSelector((s: any) => s.ui.fullscreenState.fullscreen) as boolean;

  const canExpand = get({ expandable }, 'expandable', true) as boolean;

  const iframeRef = useRef<HTMLIFrameElement>(null);
  const frameContainerRef = useRef<HTMLDivElement>(null);

  // Mutable state the long-lived poll/listeners read — kept in a ref so they never capture stale values.
  const s = useRef({
    onLoaded,
    printView,
    rawHtml,
    contentWidth,
    contentHeight,
    fullscreen,
    currentHeight: 0,
    isStable: false,
    latestUnstable: 0,
    savedScrollTop: null as number | null,
    scrollingElement: document.documentElement as Element,
    poll: undefined as ReturnType<typeof setInterval> | undefined,
    iFrameResizer: undefined as any,
    contentWindow: null as Window | null,
  });
  Object.assign(s.current, { onLoaded, printView, rawHtml, contentWidth, contentHeight, fullscreen });

  // ---- height / stability (verbatim from the directive) -------------------------------------------
  const setStable = (stable: boolean) => {
    s.current.isStable = stable;
    iframeRef.current?.setAttribute('resizing-stable', String(stable));
    if (stable && s.current.poll) clearInterval(s.current.poll);
  };

  const checkStable = () => {
    if (Date.now() - s.current.latestUnstable > RESIZING_STABLE_THRESHOLD) {
      setStable(true);
      if (!s.current.printView) s.current.onLoaded?.();
    }
  };

  const unStable = () => {
    s.current.latestUnstable = Date.now();
    if (s.current.isStable) setStable(false);
  };

  const adjustHeight = (scrollHeight: number) => {
    if (s.current.contentHeight) return;
    const frame = iframeRef.current;
    if (!frame) return;
    if (!isNumber(+scrollHeight)) scrollHeight = 1;
    initialHeight = `${Math.min(scrollHeight, window.innerHeight)}px`;

    if (s.current.currentHeight < scrollHeight) {
      s.current.currentHeight = scrollHeight;
      frame.style.height = `${scrollHeight}px`;
      unStable();
      return;
    } else if (s.current.currentHeight > scrollHeight) {
      const ratio = scrollHeight / s.current.currentHeight;
      if (ratio < 0.8) {
        // don't shrink all the time to prevent thrashing
        s.current.currentHeight = scrollHeight;
        frame.style.height = `${scrollHeight}px`;
        unStable();
        return;
      }
    }
    checkStable();
  };

  const updateHeightIfFixed = () => {
    const frame = iframeRef.current;
    if (!frame || !s.current.contentHeight) return;
    if (s.current.fullscreen) {
      frame.style.height = '100%';
      frame.style.aspectRatio = '';
    } else if (s.current.contentWidth) {
      frame.style.height = '100%';
      frame.style.aspectRatio = `${s.current.contentWidth} / ${s.current.contentHeight}`;
    } else {
      frame.style.height = `${s.current.contentHeight}px`;
    }
  };

  const refreshHeight = () => {
    const doc = s.current.contentWindow?.document;
    if (!doc?.documentElement) return;
    adjustHeight(doc.documentElement.scrollHeight);
  };

  // ---- listeners (defined once via a ref to avoid stale closures) ----------------------------------
  const handlers = useRef({
    scrollListener: () => {},
    fullscreenListener: () => {},
    messageListener: (_e: MessageEvent) => {},
  });

  handlers.current.scrollListener = () => {
    // slight lag so document.fullscreenElement is set before chrome's pre-fullscreen scroll-to-0 fires
    setTimeout(() => {
      if (document.fullscreenElement == null) {
        s.current.savedScrollTop = s.current.scrollingElement.scrollTop;
      }
    }, 100);
  };

  handlers.current.fullscreenListener = () => {
    if (document.fullscreenElement == null && s.current.savedScrollTop != null) {
      s.current.scrollingElement.scrollTop = s.current.savedScrollTop;
    }
  };

  const setFullscreen = (fs: boolean) => {
    dispatch(setFullscreenActionCreator(fs));
    refreshHeight();
  };

  handlers.current.messageListener = (event: MessageEvent) => {
    const data = event.data;
    if (data && data.fn === 'setFullScreen') {
      setTimeout(() => setFullscreen(data.arg0), 0);
    } else if (data && data.fn === 'getFullScreen') {
      (event.source as Window | null)?.postMessage({ fn: 'onFullScreen', arg0: s.current.fullscreen }, '*');
    } else if (data && data.fn === 'scrollToTop') {
      setTimeout(() => scroller.scrollTop(null));
    }
  };

  // window listeners + cleanup (mount once)
  useEffect(() => {
    const scroll = (e: Event) => handlers.current.scrollListener();
    const fsc = (e: Event) => handlers.current.fullscreenListener();
    const msg = (e: MessageEvent) => handlers.current.messageListener(e);
    window.addEventListener('scroll', scroll, false);
    window.addEventListener('fullscreenchange', fsc, false);
    window.addEventListener('message', msg, false);
    return () => {
      window.removeEventListener('scroll', scroll);
      window.removeEventListener('fullscreenchange', fsc);
      window.removeEventListener('message', msg);
      if (s.current.poll) clearInterval(s.current.poll);
      s.current.iFrameResizer?.removeListeners?.();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // fullscreen change — the directive's $watch('fullscreen')
  useEffect(() => {
    if (typeof fullscreen !== 'boolean') return;
    s.current.scrollingElement = fullscreen
      ? (frameContainerRef.current ?? document.documentElement)
      : document.documentElement;
    handlers.current.scrollListener();
    updateHeightIfFixed();
    try {
      s.current.contentWindow?.postMessage({ fn: 'onFullScreen', arg0: fullscreen }, '*');
    } catch {
      // in case this is prohibited
    }
    if (fullscreen) {
      setTimeout(() => frameContainerRef.current?.parentElement?.parentElement?.querySelector<HTMLElement>('#exit-full-screen')?.focus(), 0);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fullscreen]);

  // iframe ready — the directive's onInitFrame + onIframeReady
  const onIframeLoad = () => {
    const frame = iframeRef.current;
    const container = frameContainerRef.current;
    if (!frame || !container) return;

    s.current.contentWindow = frame.contentWindow;
    updateHeightIfFixed();

    let doc: Document | undefined;
    try {
      doc = frame.contentWindow?.document;
      if (!doc) throw new Error('no document');
    } catch {
      // usually a cross-frame error (e.g. a PDF served from S3); fall back to a fixed height.
      container.style.height = '11in';
      return;
    }

    if (s.current.printView) s.current.onLoaded?.();

    if (s.current.rawHtml) {
      doc.open();
      doc.write(s.current.rawHtml);
      doc.close();
    }

    s.current.latestUnstable = Date.now();

    // core resize poll
    s.current.poll = setInterval(() => {
      const contentH = Math.max(
        get(s.current, 'contentWindow.document.documentElement.scrollHeight', 0) as number,
        get(s.current, 'contentWindow.document.body.scrollHeight', 0) as number
      );
      adjustHeight(contentH);
    }, 400);

    // fancy resize via iframe-resizer if available
    if ((window as any).iFrameResize) {
      s.current.iFrameResizer = (window as any).iFrameResize(
        { heightCalculationMethod: 'documentElementOffset' },
        frame
      )[0].iFrameResizer;
    }
  };

  if (!url && !rawHtml) {
    return <div>{translate('UNABLE_TO_EMBED_CONTENT')}</div>;
  }

  return (
    <div className={classNames('embedded-content', { 'full-screen': canExpand && fullscreen })}>
      <div className="full-screen-container">
        <div
          className="iframe-content"
          ref={frameContainerRef}
        >
          <iframe
            ref={iframeRef}
            src={url}
            frameBorder="0"
            title={title}
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore — vendor-prefixed fullscreen attributes, as in the original template
            webkitallowfullscreen=""
            mozallowfullscreen=""
            allowFullScreen
            style={{ height: initialHeight ?? '50vh' }}
            onLoad={onIframeLoad}
          />
        </div>
      </div>
    </div>
  );
};

export default EmbeddedContent;
