/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

/**
 * Native-DOM port of the AngularJS `Scroller` service (which used jQuery's
 * angular.element/.animate/.offset). The logic is preserved verbatim: scroll
 * the page so the target lands below any sticky chrome, but never scroll *down*
 * (only up toward the target), and when no target is given scroll to the top,
 * accounting for the app header height.
 */

const totalHeight = (selector: string): number =>
  Array.from(document.querySelectorAll<HTMLElement>(selector)).reduce((sum, el) => sum + el.offsetHeight, 0);

/**
 * Scroll the window so `element` is visible below sticky headers, or to the top
 * when `element` is null/undefined.
 */
export const scrollTop = (element?: Element | null): void => {
  const scroller = document.scrollingElement ?? document.documentElement;

  let targetTop: number;
  if (!element) {
    // If scrolling to top of screen, accommodate app header
    targetTop = totalHeight('.er-page-header');
  } else {
    // Otherwise find the target offset and allow for any sticky elements that would overlap it
    const stickyHeight = totalHeight('.sticky-container-active') + totalHeight('.stuck .content-title');
    targetTop = Math.max(0, element.getBoundingClientRect().top + window.scrollY - stickyHeight);
  }

  // If I'm already scrolled above the target, don't scroll down.
  const top = Math.min(scroller.scrollTop, targetTop);
  window.scrollTo({ top, behavior: 'smooth' });
};
