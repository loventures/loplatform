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

import { TutorialInfo } from './tutorial/tutorialApi';
import { Step } from 'react-joyride';

export type UserId = number;

export type UserType =
  | 'Anonymous'
  | 'Unknown'
  | 'Guest'
  | 'Standard'
  | 'System'
  | 'Overlord'
  | 'Preview';

export type UserInfo = {
  givenName: string;
  middleName: string;
  familyName: string;
  emailAddress: string;
  userState: string;
  externalId: string;
  userName: string;
  fullName: string;
  imageUrl: string;
  title: string;
  id: UserId;
  handle: string;
  subtenant_id: number;
  user_type: UserType;
  rights: string[]; // todo: enumerate these
  roles: string[]; // todo: enumerate these
  inactive?: boolean;
  tutorials: Record<string, TutorialInfo>;
  preferences: object;
};

export type Session = {
  sessionPk: number;
  logoutReturnUrl: string;
  documentTarget: 'frame' | 'iframe' | 'window';
  sudoed: boolean;
  integrated: boolean;
  customTitle: string;
  returnUrl: string;
  user_id: number;
};

export type Enrollment = {
  stopTime: string;
  createdOn: string;
  startTime: string;
  disabled: boolean;
  changeNotes: string;
  id: number;
  role_id: number;
  context_id: number;
  role_name: string;
  user_id: number;
};

export type DomainState = 'Normal' | 'ReadOnly' | 'Maintenance' | 'Suspended' | 'Init' | 'Deleted';

export type Domain = {
  id: number;
  domainId: string;
  name: string;
  shortName: string;
  hostName: string;
  type: string;
  locale: string;
  timeZone: string;
  loginRequired: boolean;
  securityLevel: 'NoSecurity' | 'SecureAlways' | 'SecureWhenLoggedIn';
  rememberTimeout: number;
  membershipLimit: number;
  enrollmentLimit: number;
  startDate: string;
  endDate: string;
  maximumFileSize: number;
  state: DomainState;
  message: string;
  userUrlFormat: string;
  groupUrlFormat: string;
  licenseRequired: boolean;
  googleAnalyticsAccount: string;
  logo?: {
    url: string;
  };
  logo2?: {
    url: string;
  };
  favicon?: {
    url: string;
  };
  css?: {
    url: string;
  };
  appearance: {
    'color-primary'?: string;
    'color-secondary'?: string;
    'color-accent'?: string;
  };
};

export type DomainLinkEntry = {
  title: string;
  url: string;
  newWindow: boolean;
};

export type Course = {
  configuredShutdownDate: string;
  shutdownDate: string;
  commitId: number;
  endDate: string;
  startDate: string;
  archived: boolean;
  lightweight: true;
  name: string;
  groupId: string;
  externalId: string;
  url: string;
  createTime: string;
  disabled: boolean;
  id: number;
  hasEnded: boolean;
  hasShutdown: boolean;
  program_id: number;
  hasStarted: boolean;
  project_id: number;
  offering_id: number;
  subtenant_id: number;
  asset_guid: string;
  groupType: 'CourseSection' | 'TestSection' | 'PreviewSection';
  branch_id: number;
  restricted: boolean; // learner has no access to communication tools

  // These are added for meekMode.js and seems pointless?
  contentItemRoot?: string;
  noHeader?: boolean;
  // Added for headers
  isEnded?: boolean;
  // Added by course.ts and used only by logoutRedirects.js
  LTI?: boolean;
  // Added by course.ts and used only by debugBear
  effectiveStartDate?: Date;
  // Never set. Used only by modalActions.js
  title?: string;
};

export type PlatformPreferences = any; // à² _à²

declare global {
  interface Window {
    $: any;
    jQuery: any;
    CKEDITOR: any;
    lo_platform: {
      user: UserInfo;
      session: Session;
      // enrollments: Enrollment[];
      domain: Domain;
      adminLink: string;
      authoringLink: string;
      isProduction: boolean;
      isProdLike: boolean;
      isOverlord: boolean;
      clusterType: string;
      clusterName: string;
      course_roles: string[];
      features: any; // ಠ_ಠ
      environment: {
        isLocal: boolean;
        isProdLike: boolean;
        isMock?: boolean;
      };
      binding_path: string;
      cdn_url: string;
      i18n_cdn_url: string;
      i18n: {
        language: string;
        country: string;
        extension: string;
        locale: string;
      };
      i18n_url: string;
      version: number;
      identifier: string;
      timeInfo: {
        timeZone: string;
        currentTime: string;
      };
      transcriptEnabled: boolean;
      // header: {
      //   objects: DomainLinkEntry[];
      //   count: number;
      // };
      footer: {
        objects: DomainLinkEntry[];
        count: number;
      };
      preferences: PlatformPreferences;
      tutorials: Record<string, { steps: Step[] }>;

      course: Course;

      domain_name: string;
      client_ga_id: string;
      logo: string;
      googeAnalyticsToken: string;

      // used for setting courseId if no lo_platform.course.id exists
      global_course_id?: number;
    };
    MathJax: any;
    feedbackEnabled?: boolean;

    lonav?: (edgeId: string) => void;
  }
}
