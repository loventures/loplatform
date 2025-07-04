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

import Course from '../bootstrap/course.ts';
import User from '../bootstrap/user.ts';

const readPref = <A = boolean>(name: string): A => window.lo_platform.preferences[name];
const hasRight = (right: string): boolean =>
  User.rights.includes(right) || window.lo_platform.course_roles.includes(right);

/*
  features
*/
export const progressReportPageEnabled = false;

export const instructorControlsV2 = readPref('instructorControlsV2');

export const instructorLinkChecker = readPref('enableInstructorLinkChecker');

export const instructorPurgeDiscussions = readPref('enableInstructorPostPurge');

export const contentSearch = readPref('enableContentSearch');

export const enableInstructorFeedback = readPref('enableInstructorFeedback');

export const instructorDashboardPageEnabled = readPref('instructorDashboardPageEnabled');

export const editGatingPolicies = readPref('editGatingPolicies');

export const presenceChatFeature =
  readPref('presenceChatFeature') && !window.lo_platform.course.restricted;

export const groupChatFeature =
  readPref('groupChatFeature') && !window.lo_platform.course.restricted;

export const allowDirectMessaging =
  readPref('allowDirectMessaging') && !window.lo_platform.course.restricted;

export const disablePresence = readPref('disablePresence');

export const customLogoUrl = readPref<string>('customLogoUrl');

export const globalNavFeedback = readPref('globalNavFeedback');

export const allowPrintingEntireLesson = readPref('allowPrintingEntireLesson');

export const enableNotifications = readPref('enableNotifications');

export const surveyCollectorUrl = readPref<string>('surveyCollectorUrl');

export const surveyCollectorLaterHours = readPref<number>('surveyCollectorLaterHours');

export const gradebookSync = readPref('CBLPROD18092GradebookSync');

export const showContentSurveys = readPref('showContentSurveys');

export const enableInlineLTI = readPref('enableInlineLTI');

export const instructorRoster = readPref('instructorRoster');

/*
  values
*/
export const reviewPeriodOffset = readPref('reviewPeriodOffset');

export const nearDueWarning = readPref<number>('nearDueWarning');

export const strictDueDate = readPref('strictDueDate');

//this doesn't toggle the footer, only custom links within the footer
export const lOFooter = readPref('lOFooter');

//this is only for the footer
export const logoBrandingLO = readPref('logoBrandingLO');

/*
  discussions
*/
export const discussionJumpBar = readPref('discussionJumpBar');

/*
  assessments
*/
export const skippingIsOK = readPref('skippingIsOK');

/*
  grades
 */
export const useProjectedGrade = readPref('useProjectedGrade');

/*
  experiments
*/
export const CBLPROD16934InstructorResources = readPref('CBLPROD16934InstructorResources');

export const rubricGraderUseMaxCriterionValue = readPref('rubricGraderUseMaxCriterionValue');

export const isSearchEnabled = readPref('isSearchEnabled');

export const isMasteryEnabled = readPref('masteryEnabled');

export const cdnWebAssets = readPref('cdnWebAssets');

export const smeFeedbackEnabled =
  (Course.groupType === 'TestSection' || Course.groupType === 'PreviewSection') &&
  window.lo_platform.preferences.enableContentFeedback;

export const enableAnalyticsPage =
  readPref('enableAnalyticsPage') && hasRight('loi.cp.admin.right.ReportingAdminRight');

export const learnerDashboardId = readPref<number>('learnerDashboardId');

export const sectionDashboardId = readPref<number>('sectionDashboardId');

export const enableVideoRecording = readPref('enableVideoRecording');

export const qnaEnabled = readPref('enableQna');

export const topicQuizzing = readPref('topicQuizzing');

export const ltiISBN = readPref<string>('ltiISBN');

export const ltiCourseKey = readPref<string>('ltiCourseKey');
