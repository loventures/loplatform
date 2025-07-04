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

import { DependencyList, useCallback } from 'react';

type CB<T extends HTMLElement> = (node: T) => void;

export const useFocusOn = <T extends HTMLElement>(
  condition: boolean,
  deps: DependencyList
): CB<T> => {
  return useCallback<(node: T) => void>(node => {
    if (condition && node) {
      // setTimeout is here to give components a chance to register any focus handlers they'll need
      setTimeout(() => node.focus());
    }
  }, deps);
};

export const useFocusOnRender = <T extends HTMLElement>(): CB<T> => {
  return useFocusOn<T>(true, []);
};

export const useFocusOnCondition = <T extends HTMLElement>(condition: boolean): CB<T> => {
  return useFocusOn<T>(condition, [condition]);
};
