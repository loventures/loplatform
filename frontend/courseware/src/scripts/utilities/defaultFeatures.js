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

const features = {
  /**
   * Settings actually found and used in course-lw code
   * cmd click works except for looking in preferences.ts
   */
  AssessmentSaveWarning: { isEnabled: false },
  CkeditorDisallowedContent: {
    isEnabled: true,
    value: '*[compile] discussion-* script',
  },
  EssayUploadPreviewFileTypes: { isEnabled: false },
  GradebookSettings: {
    isEnabled: true,
    value: {
      gradeDisplayMethod: 'points',
    },
  },
  InstructorRubricManualGrading: { isEnabled: true },
  LOFooter: { isEnabled: true },
  LogoBrandingLO: { isEnabled: true },
  SessionListener: { isEnabled: true },
  ShowPoints: { isEnabled: false },
  SkippingIsOK: { isEnabled: true },
  UseThumbnailize: { isEnabled: true },

  /**
   * Anything with text usages, but that may still be unused
   *
   */
  AllowDirectMessaging: { isEnabled: true },
  // AllowPrintingEntireLesson:  // This is requested in code but has no default.
  EditGatingPolicies: { isEnabled: true },
  isSearchEnabled: { isEnabled: false },
  GlobalNavFeedback: { isEnabled: true },
  NearDueWarning: { isEnabled: true, value: 2 },
  PresenceChatFeature: { isEnabled: true },
  // PretestResultsNondiscouragingMessage: { isEnabled: false },
  // RubricGraderUseMaxCriterionValue:  // This is requested in code but has no default.
};

export default features;

//I need something not constants in angular terms, that also can be overriden at config time per
//application... so this.
window.getDefaultFeatures = function () {
  return angular.copy(features);
};

if (window.lo_platform) {
  window.lo_platform.DefaultFeatures = features;
}
