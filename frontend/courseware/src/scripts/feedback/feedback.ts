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

import { FeedbackDto } from '../feedback/feedbackApi';
import { useUiState } from '../loRedux';
import React, { Dispatch, MutableRefObject, SetStateAction, useEffect } from 'react';

// Listens for pastes and if there are copied files, sends them instead to a file
// handler. This works well within traditional inputs, but not the horror of CKE.
export const usePasteListener = (onDrop: (files: File[]) => void) => {
  useEffect(() => {
    const listener = (event: ClipboardEvent) => {
      if (event.clipboardData?.files?.length) {
        const files = new Array<File>();
        for (let i = 0; i < event.clipboardData.files.length; ++i) {
          files.push(event.clipboardData.files[i]);
        }
        onDrop(files);
        event.preventDefault();
        event.stopPropagation();
      }
    };
    document.addEventListener('paste', listener, true);
    return () => {
      document.removeEventListener('paste', listener, true);
    };
  }, [onDrop]);
};

// Wraps a handler to first stop event propagation. Used within a drop zone
// to prevent the drop zone opening a file selector.
export const unpropagated = (f: (e: React.MouseEvent) => void) => (e: React.MouseEvent) => {
  e.stopPropagation();
  f(e);
};

export const clearMainSelection = () => window.getSelection()?.empty();

export const clearIFrameSelection = () => {
  const iframes = document.getElementsByTagName('iframe');
  for (let i = 0; i < iframes.length; ++i) {
    iframes[i].contentWindow?.postMessage({ fn: 'clearSelection' }, '*');
  }
};

export const isElement = (node: Node): node is Element => node.nodeType === Node.ELEMENT_NODE;

// Searches for the nearest ancestor of the selection anchor node with a [data-asset-name]
// attribute identifying the asset that provided the data. Currently only used for
// identifying questions within an assessment. Could plausibly identify rubrics but that
// would need much work.
export const findSelectionAssetName = (): string | undefined => {
  let assetName: string | null = null;
  let node = document.getSelection()?.anchorNode;
  while (node && !assetName) {
    assetName = isElement(node) ? node.getAttribute('data-asset-name') : null;
    node = node.parentNode;
  }
  return assetName ?? undefined;
};

export type SelectionStatus = {
  active: boolean; // new selection disabled because one is active
  asset?: string; // the quoted asset
  quote?: string; // the selected text
  id?: string; // context id
  x?: number; // the selection mouse x
  y?: number; // the selection mouse y
};

// Listens for mouse up in the main window and if the mouse up is
// associated with a change in the selected text, send that to the
// feedback gadget.
export const useMouseSelectionStatus = (
  gadgetRef: MutableRefObject<HTMLButtonElement>,
  setStatus: Dispatch<SetStateAction<SelectionStatus>>
) => {
  useEffect(() => {
    const listener = (e: MouseEvent) => {
      const target = e.target as any;
      if (gadgetRef.current?.contains(target)) return; // otherwise clicking the gadget cancels
      const feedbackable = !!target?.closest('.feedback-context');
      const selection = document.getSelection()?.toString();
      const id = target?.closest('[data-id]')?.getAttribute('data-id');
      if (selection) clearIFrameSelection();
      const selected = (feedbackable && selection) || undefined;
      setStatus(status =>
        status.active
          ? status
          : {
              active: false,
              asset: selected ? findSelectionAssetName() : undefined,
              quote: selected,
              id: id,
              x: e.x,
              y: e.y + (document.scrollingElement?.scrollTop ?? 0),
            }
      );
    };
    document.addEventListener('mouseup', listener);
    return () => document.removeEventListener('mouseup', listener);
  }, [setStatus, gadgetRef]);
};

// Listens for events from the iframe indicating that a selection was
// made, send that to the feedback widget.
export const useIFrameSelectionStatus = (setStatus: Dispatch<SetStateAction<SelectionStatus>>) => {
  useEffect(() => {
    const listener = (
      event: MessageEvent<{
        fn: string;
        arg0: string;
        arg1: number;
        arg2: number;
        arg3: string | undefined;
      }>
    ) => {
      if (event.data?.fn === 'onSelection') {
        // better selection of iframe
        const { arg0: selected, arg1: x, arg2: y, arg3: id } = event.data;
        const pos = document.getElementsByTagName('iframe')[0]?.getBoundingClientRect();
        if (selected) clearMainSelection();
        setStatus(status =>
          status.active
            ? status
            : {
                active: false,
                asset: undefined,
                quote: selected,
                id: id,
                x: x + (pos.x ?? 0),
                y: y + (pos.y ?? 0) + (document.scrollingElement?.scrollTop ?? 0),
              }
        );
      }
    };
    window.addEventListener('message', listener);
    return () => window.removeEventListener('message', listener);
  }, [setStatus]);
};

export const ImageLikeRE = /^http.*\.(?:jpg|jpeg|gif|png|webp|svg)$/i;

export const useFeedbackDtos = (id: string): FeedbackDto[] => {
  const { feedback } = useUiState();
  return feedback.id === id ? feedback.feedback : [];
};
