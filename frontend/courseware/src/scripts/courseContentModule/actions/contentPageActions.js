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

import { debounce } from 'lodash';

import { gotoLinkActionCreator } from '../../utilities/routingUtils.js';
import { ContentPlayerPageLink } from '../../utils/pageLinks.js';
import { SET_CONTENT_FULLSCREEN } from '../reducers/fullscreenReducer.js';
import { lojector } from '../../loject.js';

export const printPageActionCreator = content => {
  lojector.get('Print').print(void 0, content);
  return { type: 'PRINT' };
};

export const closeModuleNavActionCreator = () => ({
  type: 'STATUS_FLAG_TOGGLE',
  sliceName: 'moduleNavigationPanelOpen',
  data: { status: false },
});

export const manuallySetLessonExpansion = (id, expanded) => ({
  type: 'STATUS_FLAG_TOGGLE',
  sliceName: 'moduleLessonManuallyExpanded',
  id,
  data: { status: expanded },
});

const scrollFn = debounce(
  domId => lojector.get('Scroller').scrollTop(angular.element(`#${domId}`)),
  250
);

export const scrollToModuleChild = domId => {
  scrollFn(domId);
  return { type: 'SCROLL_TO_MODULE' };
};

export const viewParentFromContentActionCreator = content =>
  gotoLinkActionCreator(ContentPlayerPageLink.toLink({ content: content }));

export const setFullscreenActionCreator = fullscreen => ({
  type: SET_CONTENT_FULLSCREEN,
  sliceName: 'fullscreenState',
  fullscreen,
});
