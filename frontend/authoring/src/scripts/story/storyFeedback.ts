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

import { MutableRefObject, useEffect } from 'react';
import { useDispatch } from 'react-redux';

import { resetHtmlFeedback, setHtmlFeedback } from '../feedback/feedbackActions';

export const clearMainSelection = () => window.getSelection()?.empty();

export const broadcastToIFrames = (msg: any) => {
  const iframes = document.getElementsByTagName('iframe');
  for (let i = 0; i < iframes.length; ++i) {
    iframes[i].contentWindow?.postMessage(msg, '*');
  }
};

export const clearIFrameSelection = () => broadcastToIFrames({ fn: 'clearSelection' });

// Listens for events from an iframe indicating that a selection was
// made, send that to the feedback widget.
export const useIFrameSelectionListener = (enabled: boolean) => {
  const dispatch = useDispatch();
  useEffect(() => {
    if (!enabled) return;
    const listener = (
      event: MessageEvent<{ fn: string; arg0: string; arg1: number; arg2: number; arg3?: string }>
    ) => {
      if (event.data?.fn === 'onSelection') {
        // better selection of iframe
        const { arg0: selected, arg1: x, arg2: y, arg3: id } = event.data;
        const iFrame = (event.source as Window).frameElement;
        const pos = iFrame?.getBoundingClientRect();
        if (selected) clearMainSelection();
        const dataPath = iFrame?.attributes?.getNamedItem('data-path');
        const path = dataPath ? dataPath.value.split('.') : [];
        dispatch(
          selected
            ? setHtmlFeedback(
                selected,
                x + (pos.x ?? 0),
                y + (pos.y ?? 0) + (document.scrollingElement?.scrollTop ?? 0),
                path,
                id
              )
            : resetHtmlFeedback()
        );
      }
    };
    // This lets the iframe know that we will accept feedback events so it should
    // enable selection machinery and update the selection CSS
    (window as any).feedbackEnabled = true;
    window.addEventListener('message', listener);
    broadcastToIFrames({ fn: 'checkEnabled' });
    return () => {
      delete (window as any).feedbackEnabled;
      window.removeEventListener('message', listener);
      broadcastToIFrames({ fn: 'checkEnabled' });
    };
  }, [enabled]);
};

export const isElement = (node: Node | null): node is Element =>
  node?.nodeType === Node.ELEMENT_NODE;

// Listens for mouse up in the main window and if the mouse up is
// associated with a change in the selected text, send that to the
// feedback gadget. Not supporting double-click images for now because
// these are exceedingly rare outside of HTML.
export const useMouseSelectionStatus = (
  gadgetRef: MutableRefObject<HTMLButtonElement>,
  enabled: boolean
) => {
  const dispatch = useDispatch();
  useEffect(() => {
    if (!enabled) return;
    const listener = (e: MouseEvent) => {
      const target = e.target as any;
      if (gadgetRef.current?.contains(target)) return; // otherwise clicking the gadget cancels
      const feedbackable = !!target?.closest?.('.feedback-context');
      const id = target?.closest('[data-id]')?.getAttribute('data-id');
      const selection = document.getSelection()?.toString();
      if (selection) clearIFrameSelection();
      const selected = (feedbackable && selection) || undefined;
      let dataPath = '';
      let node = document.getSelection()?.anchorNode;
      while (node && !dataPath) {
        dataPath = isElement(node) ? node.getAttribute('data-path') : null;
        node = node.parentNode;
      }
      const path = dataPath ? dataPath.split('.') : [];
      dispatch(
        selected && path.length
          ? setHtmlFeedback(
              selected,
              e.x,
              e.y + (document.scrollingElement?.scrollTop ?? 0),
              path,
              id
            )
          : resetHtmlFeedback()
      );
    };
    document.addEventListener('mouseup', listener);
    return () => document.removeEventListener('mouseup', listener);
  }, [gadgetRef, enabled]);
};
