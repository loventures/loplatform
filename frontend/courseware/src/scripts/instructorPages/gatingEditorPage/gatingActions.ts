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

import { GATING_EDITOR_LOCKDATE_UPDATED, GATING_EDITOR_ACTIVITY_TOGGLED } from './actionTypes';

export const temporalPolicyUpdatedAC = (contentId: any, lockDate: any) => ({
  type: GATING_EDITOR_LOCKDATE_UPDATED,
  contentId,
  data: { lockDate },
});

export const activityPolicyRemovedAC = (contentId: any, removedAssignmentId: any) => ({
  type: GATING_EDITOR_ACTIVITY_TOGGLED,
  contentId,
  data: { removedAssignmentId },
});
