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

import iframeResizer from 'iframe-resizer/js/iframeResizer';
import { MutableRefObject, useEffect, useState } from 'react';
import { useDebounce } from 'use-debounce';

// The nonsense with element-by-id is to make this happen once per DCM session. Better solved by putting this
// higher in the DCM tree but it doesn't seem it belongs any higher.
export const useDefaultStyles = (branchId: number) => {
  useEffect(() => {
    const id = `stylesheet-${branchId}`;
    if (!document.getElementById(id)) {
      const stylesheet = document.createElement('link');
      stylesheet.setAttribute('id', id);
      stylesheet.setAttribute('rel', 'stylesheet');
      stylesheet.setAttribute('type', 'text/css');
      stylesheet.setAttribute('href', `/api/v2/authoring/branches/${branchId}/defaults.css`);
      document.head.appendChild(stylesheet);
    }
  }, [branchId]);
};

export const useExitEditingOnOnUnfocused = (
  divRef: MutableRefObject<HTMLDivElement | null>,
  editing: boolean,
  focused: boolean,
  exitEditing: () => void
) => {
  // We exit editing mode after blur only when the focus goes to a different input in the page.
  // Otherwise ctrl-F, scrollbar click and various other useful things exit editing.
  useEffect(() => {
    if (editing && !focused) {
      let timeout: any | undefined = undefined;
      const checkFocus = () => {
        const active = document.activeElement;
        if (
          !divRef.current?.contains(active) &&
          !active.closest('.no-exit-edit') &&
          active.closest('input,textarea,.summernote-wrapper,.story-element')
        ) {
          timeout = undefined;
          exitEditing();
        } else {
          timeout = setTimeout(checkFocus, 300);
        }
      };
      timeout = setTimeout(checkFocus, 0);
      return () => {
        if (timeout) clearTimeout(timeout);
      };
    }
    // Summernote sometimes leaves dead tooltips around. Especially if you
    // hover on a button then escape to exit edit mode.
    if (!editing) {
      const tips = document.getElementsByClassName('note-tooltip');
      for (let i = tips.length - 1; i >= 0; --i) {
        tips[i].parentNode.removeChild(tips[i]);
      }
    }
  }, [focused, editing]);
};

// Delivery has machinery that adds an extra 15 pixels to the height to allow
// for a possible horizontal scrollbar. Generally we use the iframe resizer
// which suppresses scrollbars so we do not do this here.
export const useIFrameResizeMachinery = (
  iFrameRef: MutableRefObject<HTMLIFrameElement | null>,
  htmlSrc: string | undefined
) => {
  const [height, setHeight] = useState(0);

  useEffect(() => {
    if (iFrameRef.current && htmlSrc) {
      const resizer = iframeResizer(
        {
          sizeHeight: false, // suppress iframeresizer changing the height itself
          resizedCallback: ({ height }) => setHeight(parseInt(height)),
        },
        iFrameRef.current
      )[0].iFrameResizer;
      return () => resizer?.removeListeners();
    }
  }, [htmlSrc]);

  // The iframe resizer doesnt't work well for initial dynamic layout, often cutting the content short.
  // This is largely derived from course-lw which has the extra poll.
  useEffect(() => {
    if (iFrameRef.current && htmlSrc) {
      let stableCount = 0;
      let priorHeight = 0;
      const poller = setInterval(() => {
        if (!iFrameRef.current) {
          clearInterval(poller);
          return;
        }
        const sh = iFrameRef.current.contentWindow?.document?.documentElement?.scrollHeight ?? 0;
        const bh = iFrameRef.current.contentWindow?.document?.body?.scrollHeight ?? 0;
        const newHeight = Math.max(sh, bh);
        setHeight(newHeight);
        if (newHeight !== priorHeight) {
          priorHeight = newHeight;
          stableCount = 0;
        } else if (++stableCount > 4) {
          clearInterval(poller);
        }
      }, 400);
      return () => clearInterval(poller);
    }
  }, [htmlSrc]);

  // Without a debounce the height tends to bounce slightly on load.
  const [debounced] = useDebounce(height, 100);

  return debounced;
};
