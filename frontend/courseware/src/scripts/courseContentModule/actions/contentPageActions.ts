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

import { debounce } from 'lodash';

import { gotoLinkActionCreator } from '../../utilities/routingUtils.js';
import { ContentPlayerPageLink } from '../../utils/pageLinks.js';
import { SET_CONTENT_FULLSCREEN } from '../reducers/fullscreenReducer.js';
import { printService } from '../../utilities/printService.ts';
import { scroller } from '../../utilities/pure/scroller.ts';

export const printPageActionCreator = (content: any) => {
  printService.print(void 0, content);
  return { type: 'PRINT' };
};

export const closeModuleNavActionCreator = () => ({
  type: 'STATUS_FLAG_TOGGLE',
  sliceName: 'moduleNavigationPanelOpen',
  data: { status: false },
});

export const manuallySetLessonExpansion = (id: any, expanded: any) => ({
  type: 'STATUS_FLAG_TOGGLE',
  sliceName: 'moduleLessonManuallyExpanded',
  id,
  data: { status: expanded },
});

const scrollFn = debounce(
  (domId: string) => scroller.scrollTop(document.getElementById(domId)),
  250
);

export const scrollToModuleChild = (domId: string) => {
  scrollFn(domId);
  return { type: 'SCROLL_TO_MODULE' };
};

export const viewParentFromContentActionCreator = (content: any) =>
  gotoLinkActionCreator(ContentPlayerPageLink.toLink({ content: content }));

export const setFullscreenActionCreator = (fullscreen: any) => ({
  type: SET_CONTENT_FULLSCREEN,
  sliceName: 'fullscreenState',
  fullscreen,
});
