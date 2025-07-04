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

import { DependencyList, useCallback, useRef } from 'react';

export type NodeCallbackWithCleanUp<TNode extends HTMLElement = HTMLElement> = (
  n: TNode
) => () => void;

export const useCallbackWithCleanUp = <TCallback extends (...args: any[]) => () => void>(
  callback: TCallback,
  deps: DependencyList
): ((...args: Parameters<TCallback>) => void) => {
  const cleanUpRef = useRef<(() => void) | null>(null);

  const callbackInner = (...args: any[]) => {
    if (cleanUpRef.current) {
      cleanUpRef.current();
      cleanUpRef.current = null;
    }
    cleanUpRef.current = callback(...args);
  };

  const callbackRef = useCallback(callbackInner, deps);

  return callbackRef;
};
