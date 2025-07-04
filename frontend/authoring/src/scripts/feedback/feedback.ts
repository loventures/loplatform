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

import { useEffect, useState } from 'react';
import { useMediaQuery } from 'react-responsive';

export const useEpicScreen = () => useMediaQuery({ query: '(min-width: 94em)' });

/** Returns whether the target div or document is scrolled.
 */
export const useIsScrolled = (node?: HTMLDivElement | Document): boolean => {
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const el = node === document ? document.scrollingElement : (node as HTMLDivElement);
    if (el) {
      let wasScrolled = scrolled;
      const listener = () => {
        const isScrolled = el.scrollTop >= 1; // allow a fraction of a pixel
        if (isScrolled !== wasScrolled) setScrolled((wasScrolled = isScrolled));
      };
      listener();
      node.addEventListener('scroll', listener);
      return () => node.removeEventListener('scroll', listener);
    }
  }, [node]);
  return scrolled;
};
