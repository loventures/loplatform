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

import { FromApp } from '../utils/linkUtils';
import React, { HTMLAttributes, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router';

type Landmark = 'appHeader' | 'appFooter' | 'content' | 'mainHeader';

type ERLandmarkContext = Record<Landmark, HTMLElement | null> & {
  setLandmarkElement: (landmark: Landmark, element: HTMLElement | null) => void;
};

export const ERLandmarkContext = React.createContext<ERLandmarkContext>({
  appHeader: null,
  appFooter: null,
  content: null,
  mainHeader: null,
  setLandmarkElement: () => {},
});

export const ERLandmarkProvider: React.FC<React.PropsWithChildren> = ({ children }) => {
  const [landmarks, setLandmarks] = useState<Record<Landmark, HTMLElement | null>>({
    appHeader: null,
    appFooter: null,
    content: null,
    mainHeader: null,
  });
  const setLandmarkElement = useCallback(
    (landmark: Landmark, element: HTMLElement | null) =>
      setLandmarks(lm => ({ ...lm, [landmark]: element })),
    [setLandmarks]
  );

  // this side effect is unrelated to skip nav link but...
  // intent is to focus main content on every react-router route change
  // so that screen readers immediately start reading new main content
  // after up next nav or sidebar nav.
  const location = useLocation<FromApp>();
  useEffect(() => {
    if (location.state?.fromApp) {
      landmarks.mainHeader?.focus({ preventScroll: true });
    }
  }, [location.pathname, location.state?.fromApp, landmarks.mainHeader]);

  return (
    <ERLandmarkContext.Provider
      value={{
        ...landmarks,
        setLandmarkElement,
      }}
    >
      {children}
    </ERLandmarkContext.Provider>
  );
};

type ERLandmarkProps = {
  landmark: Landmark;
  tag?: React.ElementType;
} & HTMLAttributes<HTMLElement>;

export const ERLandmark: React.FC<ERLandmarkProps> = ({ landmark, tag: Tag = 'div', ...props }) => {
  const { setLandmarkElement } = useContext(ERLandmarkContext);
  const ref = useRef<HTMLElement>(null);
  useEffect(
    () => setLandmarkElement(landmark, ref.current),
    [landmark, setLandmarkElement, ref.current]
  );
  return (
    <Tag
      {...props}
      ref={ref}
    />
  );
};
