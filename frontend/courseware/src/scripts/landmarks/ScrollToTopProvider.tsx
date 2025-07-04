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

import { ERLandmarkContext } from '../landmarks/ERLandmarkProvider';
import React, { useCallback, useContext } from 'react';

export const ScrollToTopContext = React.createContext<() => void>(() => {});

export const ScrollToTopProvider: React.FC<React.PropsWithChildren> = ({ children }) => {
  const { appHeader } = useContext(ERLandmarkContext);
  const scrollToTop = useCallback(() => {
    setTimeout(() => {
      if (document.scrollingElement && appHeader) {
        const scrollTop = document.scrollingElement.scrollTop;
        const header = appHeader.offsetHeight;
        const top = Math.min(header, scrollTop);
        document.scrollingElement.scrollTo({ top });
      }
    }, 0);
  }, [appHeader]);

  return <ScrollToTopContext.Provider value={scrollToTop}>{children}</ScrollToTopContext.Provider>;
};
