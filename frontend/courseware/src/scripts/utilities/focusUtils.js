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

/*
  Focus on main content but prevents default browser behavior of scrolling.
  The default scrolling behavior does not account for sticky/fixed headers and
  usually resulting in the main content getting cropped after scrolling.

  Also useful for resetting tab order for keyboard navigations when switching
  between views or dynamic content where the previousily focused element becomes
  hidden/unrendered resulting in inconsistent tab order.
*/
export const focusMainContent = () => {
  const mainContentInterval = window.setInterval(() => {
    const maincontent = document.getElementById('maincontent');
    if (maincontent) {
      maincontent.focus();
      window.scrollTo(0, 0);
      window.clearInterval(mainContentInterval);
    }
  }, 300);
};
