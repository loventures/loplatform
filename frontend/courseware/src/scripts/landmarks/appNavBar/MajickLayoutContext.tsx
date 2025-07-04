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

import { ERLandmarkContext } from '../../landmarks/ERLandmarkProvider';
import { debounce } from 'lodash';
import React, { CSSProperties, useContext, useEffect, useMemo, useState } from 'react';
import { useMedia } from 'react-use';

// https://caniuse.com/?search=vw
// Currently all browsers ~but Firefox~ "incorrectly" consider 100vw to be the entire page width, including vertical scroll bar
const computeScrollbarWidth = () => window.innerWidth - document.documentElement.clientWidth;

type MajickLayoutContext = {
  /** Variables to mix in for layout purposes. */
  cssVariables: CSSProperties;
  /** Whether we are at a breakpoint where the sidebar autohides. */
  autohideSidebar: boolean;
};

export const MajickLayoutContext = React.createContext<MajickLayoutContext>({
  cssVariables: {},
  autohideSidebar: false,
});

export const MajickLayoutProvider: React.FC<React.PropsWithChildren> = ({ children }) => {
  const { appFooter, appHeader, content } = useContext(ERLandmarkContext);
  /** The offset of the sidebar/content header from the viewport top in the range [0,appHeaderHeight]. */
  const [headerOffset, setHeaderOffset] = useState(0);
  /** The height of the footer. */
  const [footerHeight, setFooterHeight] = useState(0);
  /** The horizontal offset of the content from the left side, for positioning stuck header. */
  const [contentOffset, setContentOffset] = useState(0);
  /** The amount of blank space between content and footer. */
  const [blankSpace, setBlankSpace] = useState(0);
  /** The margin to push the footer on-screen if the content is short. */
  const footerMargin = Math.min(headerOffset, blankSpace);

  /** The width of the scrollbar to accommodate vertical scrollbar space in horizontal sizing */
  const scrollbarWidth = computeScrollbarWidth();

  /** $mdscreen-max */
  const autohideSidebar = useMedia('(max-width: 63.9375em)');

  /** In which we compute a `headerOffset` value that lets us know how far the app header
   * has scrolled off-screen in order to maintain maximal sidebar height, and in order to
   * push the app footer on-screen when viewing short content. Uses an intersection observer
   * so we only maintain a scrollbar listener while the header is on screen.
   */
  useEffect(() => {
    if (appHeader && typeof IntersectionObserver === 'function') {
      let scrollWatch = false;
      const calculateHeaderOffset = () => {
        setHeaderOffset(Math.max(0, appHeader.getBoundingClientRect().bottom));
      };
      const intersector = new IntersectionObserver(([entry]) => {
        calculateHeaderOffset();
        if (entry.isIntersecting && !scrollWatch) {
          scrollWatch = true;
          window.addEventListener('scroll', calculateHeaderOffset);
        } else if (scrollWatch && !entry.isIntersecting) {
          scrollWatch = false;
          window.removeEventListener('scroll', calculateHeaderOffset);
        }
      });
      calculateHeaderOffset();
      intersector.observe(appHeader);
      return () => {
        intersector.disconnect();
        if (scrollWatch) window.removeEventListener('scroll', calculateHeaderOffset);
      };
    }
  }, [appHeader, setHeaderOffset]);

  /** In which we compute `footerHeight` and `blankSpace` values in order to push the
   * footer beneath the content, and in order to compute offsetting for the footer on short
   * content pages. This has to watch for page resize and resize of the relevant
   * landmark elements. */
  useEffect(() => {
    if (content && appFooter && typeof ResizeObserver === 'function') {
      const setFooterMargin = () => {
        // The amount of blank space between the bottom of the content and the top of
        // the footer. Used to push the footer on-screen when content is short.
        setBlankSpace(
          Math.max(
            0,
            window.innerHeight -
              content.offsetHeight -
              2 * content.offsetTop -
              appFooter.offsetHeight
          )
        );
        setFooterHeight(appFooter.offsetHeight);
        setContentOffset(content.offsetLeft);
      };
      window.addEventListener('resize', setFooterMargin);
      const resizer = new ResizeObserver(debounce(setFooterMargin));
      resizer.observe(content);
      resizer.observe(appFooter);
      setFooterMargin();
      return () => {
        resizer.disconnect();
        window.removeEventListener('resize', setFooterMargin);
        setBlankSpace(0);
      };
    }
  }, [appFooter, content]);

  const cssVariables = useMemo(
    () =>
      ({
        '--scrollbar-width': `${scrollbarWidth}px`,
        '--header-offset': `${headerOffset}px`,
        '--content-offset': `${contentOffset}px`,
        '--footer-margin': `${footerMargin}px`,
        '--footer-height': `${Math.max(footerHeight, 56)}px`, // min height is 3.5rem
      }) as CSSProperties,
    [scrollbarWidth, headerOffset, footerMargin, footerHeight, contentOffset]
  );

  return (
    <MajickLayoutContext.Provider value={{ cssVariables, autohideSidebar }}>
      {children}
    </MajickLayoutContext.Provider>
  );
};
