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

import React, { DependencyList, RefObject, useEffect } from 'react';

import { NodeCallbackWithCleanUp, useCallbackWithCleanUp } from './hookUtils';

export const useKeypressEffect = (
  handler: (e: KeyboardEvent) => void,
  deps: DependencyList
): void => {
  useEffect(() => {
    window.addEventListener('keydown', handler);
    return () => {
      window.removeEventListener('keydown', handler);
    };
  }, deps);
};

export const useKeypressCallback = (
  handler: (m: HTMLElement, e: KeyboardEvent) => void,
  deps: DependencyList
): React.Ref<HTMLElement> => {
  return useCallbackWithCleanUp<NodeCallbackWithCleanUp>(node => {
    const _handler = (event: KeyboardEvent) => handler(node, event);
    window.addEventListener('keydown', _handler);
    return () => {
      window.removeEventListener('keydown', _handler);
    };
  }, deps);
};

export const useOutsideClickEffect = (
  ref: RefObject<HTMLElement>,
  outsideClickHandler: (e: MouseEvent) => void,
  deps: DependencyList
): void => {
  useEffect(() => {
    const handler = (event: MouseEvent) => {
      if (event.target && ref.current) {
        const target = event.target as Element;
        if (!ref.current.contains(target)) {
          outsideClickHandler(event);
        }
      }
    };
    window.addEventListener('click', handler);
    return () => {
      window.removeEventListener('click', handler);
    };
  }, deps);
};

export const useOutsideFocusEffect = (
  ref: RefObject<HTMLElement>,
  outsideFocusHandler: (e: Event) => void,
  deps: DependencyList
): void => {
  useEffect(() => {
    const handler = (event: Event) => {
      if (event.target && ref.current) {
        const target = event.target as Element;
        if (!ref.current.contains(target)) {
          outsideFocusHandler(event);
        }
      }
    };
    window.addEventListener('focusin', handler);
    return () => {
      window.removeEventListener('focusin', handler);
    };
  }, deps);
};

export const useEventListener = (
  ref: RefObject<HTMLElement>,
  event: string,
  handler: (e: Event) => void,
  deps: DependencyList
): void => {
  useEffect(() => {
    if (ref.current) {
      ref.current.addEventListener(event, handler);
    }
    return () => {
      if (ref.current) {
        window.removeEventListener(event, handler);
      }
    };
  }, deps);
};
