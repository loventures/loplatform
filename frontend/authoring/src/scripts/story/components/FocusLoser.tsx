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

import React, {
  FocusEventHandler,
  MutableRefObject,
  ReactElement,
  useCallback,
  useEffect,
  useState,
} from 'react';

// Track when a div goes from having focus within to not.
// There's a library, react-foco, that claims to do this, but is
// in fact just shit.

export const FocusLoser: React.FC<{
  divRef: MutableRefObject<HTMLDivElement>;
  focusLost: () => void;
  children: (onFocus: FocusEventHandler, focused: boolean) => ReactElement;
}> = ({ divRef, focusLost, children }) => {
  const [hadFocus, setHadFocus] = useState(false);
  const [focused, setFocused] = useState(false);

  useEffect(() => {
    if (focused) {
      setHadFocus(true);
      const listener = (e: FocusEvent | MouseEvent | TouchEvent) => {
        if (!divRef.current?.contains(e.target as any)) {
          setFocused(false);
        }
      };
      document.addEventListener('focusin', listener);
      document.addEventListener('click', listener);
      document.addEventListener('touchstart', listener);
      return () => {
        document.removeEventListener('touchstart', listener);
        document.removeEventListener('click', listener);
        document.removeEventListener('focusin', listener);
      };
    }
  }, [focused]);

  useEffect(() => {
    if (hadFocus && !focused) {
      setHadFocus(false);
      focusLost();
    }
  }, [focused, hadFocus]);

  const onFocus = useCallback(() => setFocused(true), [setFocused]);

  return children(onFocus, focused);
};
