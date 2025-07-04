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

import React, { Dispatch, SetStateAction, useCallback, useState } from 'react';

type Sidemark = 'active' | 'topPadding' | 'bottomPadding';

type ERSidebarLandmarkContext = {
  /** The DOM container element that should be scrolled. */
  containerDiv: HTMLElement | null;
  /** The DOM element that should be scrolled into view. */
  activeDiv: HTMLElement | null;
  /** The top sticky DOM element that scroll-into-view should account for. */
  topPaddingDiv: HTMLElement | null;
  /** The bottom sticky DOM element that scroll-into-view should account for. */
  bottomPaddingDiv: HTMLElement | null;

  /** Set sidebar landmarks. */
  setSidemark: (sidemark: Sidemark, value: HTMLElement | null) => void | (() => void);
};

export const ERSidebarLandmarkContext = React.createContext<ERSidebarLandmarkContext>({
  containerDiv: null,
  activeDiv: null,
  topPaddingDiv: null,
  bottomPaddingDiv: null,
  setSidemark: () => {},
});

export const ERSidebarLandmarkProvider: React.FC<
  { containerDiv: HTMLElement | null } & React.PropsWithChildren
> = ({ containerDiv, children }) => {
  const [activeDiv, setActiveDiv] = useState<HTMLElement | null>(null);
  const [topPaddingDiv, setTopPaddingDiv] = useState<HTMLElement | null>(null);
  const [bottomPaddingDiv, setBottomPaddingDiv] = useState<HTMLElement | null>(null);
  const updateLandmark = useCallback(
    (sidemark: Sidemark, value: HTMLElement | null) => {
      const setLandmark = <A,>(setState: Dispatch<SetStateAction<A | null>>, value: A | null) => {
        setState(value); // set the new value
        return () => setState(prior => (prior === value ? null : prior)); // clear on unmount
      };
      if (sidemark === 'active') return setLandmark(setActiveDiv, value);
      else if (sidemark === 'topPadding') return setLandmark(setTopPaddingDiv, value);
      else if (sidemark === 'bottomPadding') return setLandmark(setBottomPaddingDiv, value);
    },
    [setActiveDiv, setTopPaddingDiv, setBottomPaddingDiv]
  );
  return (
    <ERSidebarLandmarkContext.Provider
      value={{
        containerDiv,
        activeDiv,
        topPaddingDiv,
        bottomPaddingDiv,
        setSidemark: updateLandmark,
      }}
    >
      {children}
    </ERSidebarLandmarkContext.Provider>
  );
};
