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

import { InstructorPreviewEnterLink, InstructorPreviewExitLink } from '../../utils/pageLinks';

import { gotoLinkActionCreator } from '../../utilities/routingUtils';
import { statusFlagToggleActionCreatorMaker } from '../../utilities/statusFlagReducer';

const modalStatusActionCreator = statusFlagToggleActionCreatorMaker({
  sliceName: 'learnerPreviewUserPickerModalState',
});

export const openLearnerPreviewPickerActionCreator = () => {
  return modalStatusActionCreator(true);
};

export const toggleLearnerPreviewPickerActionCreator = () => {
  return modalStatusActionCreator();
};

export const pickPreviewUserActionCreator = learner => {
  return gotoLinkActionCreator(InstructorPreviewEnterLink.toLink({ learner }));
};

export const exitPreviewActionCreator = () => {
  return gotoLinkActionCreator(InstructorPreviewExitLink.toLink());
};
