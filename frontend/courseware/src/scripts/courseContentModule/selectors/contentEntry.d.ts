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

import { UserInfo } from '../../../loPlatform';
import { ContentGate, GatingInformation } from '../../api/contentsApi.ts';
import { GradeWithDetails } from '../../selectors/gradeSelectors.ts';
import { ProgressWithDetails } from '../../selectors/progressSelectors.ts';
import { ContentDisplayInfo } from '../../utilities/contentDisplayInfo.ts';
import { ContentThinnedWithLearningIndex } from '../../utilities/contentResponse.ts';

// I'm using unknown so that if these ever get used, we know we have to type them. any is too permissive.
type ActivityWithDetails = unknown & { discussion: unknown; discussionSummary: unknown };
type GatingInformationWithNebulousDetails = {
  gatingInformation: GatingInformation; // I think this is here
  thisGate: unknown;
  isOpen: boolean;
  isLocked: boolean;
  isReadOnly: boolean;
  isGated: boolean;
  allGates: ContentGate[];
};
export type ContentWithNebulousDetails = ContentThinnedWithLearningIndex & {
  displayIcon: string;
  displayKey: ContentDisplayInfo['displayKey'];
  availability: GatingInformationWithNebulousDetails;
  grade: GradeWithDetails;
  progress: ProgressWithDetails & { title: string };
  activity: ActivityWithDetails;
  hasCompetencies: boolean;
  dueDateExempt: boolean; // this exists on the DTO too. It's just removed and added back for no reason.
  isClosedAsActivity: boolean;
  isForCredit: boolean;
};
export type ViewingAs = UserInfo & {
  isPreviewing: boolean;
  isInstructor: boolean;
  isUnderTrialAccess: boolean;
  isStudent: boolean;
};
