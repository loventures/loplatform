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

import { mapValues } from 'lodash';

export type ContentStatus = string;
export type ProjectStatus = string;
export type AuthoringRole = string;
export type AccessName = string;
export type DomainRole = string;

export interface ContentStatusConfiguration {
  projectStatuses: Record<ProjectStatus, string>; // key -> label
  contentStatuses: Record<ContentStatus, string>; // key -> label
  projectStatusMapping: Record<ProjectStatus, ContentStatus>;
  projectStatusColor: Record<ProjectStatus, string>; // key -> color
  authoringRoles: Record<AuthoringRole, string>; // key -> label
  domainRoleMapping: Record<DomainRole, AuthoringRole>;
  projectAccesses: Record<AccessName, Array<ProjectAccess>>; // key -> set of access
  contentAccesses: Record<AccessName, Array<ContentAccess>>; // key -> set of access
  accessNames: Record<AccessName, string>; // key -> label
  projectAccessByRoleAndStatus: Record<AuthoringRole, Record<ProjectStatus, AccessName>>;
  contentAccessByRoleAndStatus: Record<AuthoringRole, Record<ContentStatus, AccessName>>;
}

export const contentAccessRights = {
  ViewContent: 'View Content',
  EditContent: 'Edit Content',
  AlignContent: 'Align Content',
  EditSettings: 'Edit Content Settings',
  AddRemoveContent: 'Add/Remove Content',
  ViewFeedback: 'View Feedback',
  AddFeedback: 'Add Feedback',
  ViewSurvey: 'View Survey',
  EditSurvey: 'Edit Survey',
  PageHistory: 'Page History',
  Import: 'Import',
  Export: 'Export',
} as const;

export type ContentAccess = keyof typeof contentAccessRights;

export const projectAccessRights = {
  ViewObjectives: 'View Learning Objectives',
  EditObjectives: 'Edit Learning Objectives',
  ViewGradebook: 'View Course Gradebook',
  EditGradebook: 'Edit Course Gradebook',
  ViewTimeline: 'View Course Timeline',
  EditTimeline: 'Edit Course Timeline',
  ViewMultiverse: 'View Multiverse',
  EditMultiverse: 'Edit Multiverse',
  ViewProjectHistory: 'View Project History',
  ContentRepo: 'Content Repository', // deprecated
  FeedbackApp: 'Feedback App',
  ViewAllFeedback: 'View All Feedback',
  VaultApp: 'Vault App',
  ProjectSettings: 'Project Settings',
  PublishProject: 'Project Versions',
  ContentStatus: 'Edit Content Status',
  Preview: 'Preview Content',
  TestSections: 'Create Test Sections',
  AddExternal: 'Add External Content',
} as const;

export type ProjectAccess = keyof typeof projectAccessRights;

export type ProjectAccessRights = Partial<Record<ProjectAccess, boolean>>;

export type ContentAccessRights = Partial<Record<ContentAccess, boolean>>;

export type AccessByProjectStatus = Record<ProjectStatus, ProjectAccessRights>;

export type AccessByContentStatus = Record<ContentStatus, ContentAccessRights>;

export type ProjectAccessByRoleAndStatus = Record<AuthoringRole, AccessByProjectStatus>;

export type ContentAccessByRoleAndStatus = Record<AuthoringRole, AccessByContentStatus>;

export const allAccess: Record<ProjectAccess | ContentAccess, true> = {
  ...mapValues(contentAccessRights, (): true => true),
  ...mapValues(projectAccessRights, (): true => true),
};

export const noAccess: Partial<Record<ProjectAccess | ContentAccess, true>> = {};

export const computeProjectAccessByRoleAndStatus = (
  config: ContentStatusConfiguration
): ProjectAccessByRoleAndStatus => {
  const access: ProjectAccessByRoleAndStatus = {};
  const projectAccesses = {
    ...config.projectAccesses,
    '': [],
    '*': Object.keys(projectAccessRights),
  };
  for (const [role, statusAccess] of Object.entries(config.projectAccessByRoleAndStatus ?? {})) {
    if (!config.authoringRoles[role]) console.log(`ACL: Bad project access role ${role}`);
    const roleAccess: AccessByProjectStatus = (access[role] ??= {});
    for (const [status, name] of Object.entries(statusAccess)) {
      if (status !== '*' && !config.projectStatuses[status])
        console.log(`ACL: Bad project access status ${status}`);
      if (!projectAccesses[name]) console.log(`ACL: Bad project access profile ${name}`);
      const statusAccess: ProjectAccessRights = (roleAccess[status] ??= {});
      for (const access of projectAccesses[name] ?? []) {
        statusAccess[access] = true;
      }
    }
  }
  return access;
};

export const computeContentAccessByRoleAndStatus = (
  config: ContentStatusConfiguration
): ContentAccessByRoleAndStatus => {
  const access: ContentAccessByRoleAndStatus = {};
  const contentAccesses = {
    ...config.contentAccesses,
    '': [],
    '*': Object.keys(contentAccessRights),
  };
  for (const [role, statusAccess] of Object.entries(config.contentAccessByRoleAndStatus ?? {})) {
    if (!config.authoringRoles[role]) console.log(`ACL: Bad course access role ${role}`);
    const roleAccess: AccessByContentStatus = (access[role] ??= {});
    for (const [status, name] of Object.entries(statusAccess)) {
      if (status !== '*' && !config.contentStatuses[status])
        console.log(`ACL: Bad course access status ${status}`);
      if (!contentAccesses[name]) console.log(`ACL: Bad course access profile ${name}`);
      const statusAccess: ContentAccessRights = (roleAccess[status] ??= {});
      for (const access of contentAccesses[name] ?? []) {
        statusAccess[access] = true;
      }
    }
  }
  return access;
};

export const stockConfiguration: ContentStatusConfiguration = {
  authoringRoles: {
    Viewer: 'Viewer',
    Editor: 'Editor',
  },
  projectStatuses: {
    Dev: 'In Development',
    Live: 'Live',
  },
  contentStatuses: {
    Authoring: 'Authoring',
    Complete: 'Complete',
  },
  projectStatusMapping: {
    Dev: 'Authoring',
    Live: 'Complete',
  },
  projectStatusColor: {
    Dev: 'danger',
    Live: 'success',
  },
  accessNames: {
    View: 'View',
    Edit: 'Edit',
  },
  projectAccesses: {
    View: ['ViewObjectives', 'ViewGradebook', 'ViewTimeline', 'FeedbackApp', 'Preview'],
    Edit: [
      'ViewObjectives',
      'EditObjectives',
      'ViewGradebook',
      'EditGradebook',
      'ViewTimeline',
      'EditTimeline',
      'ViewProjectHistory',
      'FeedbackApp',
      'VaultApp',
      'Preview',
    ],
  },
  contentAccesses: {
    View: ['ViewContent'],
    Edit: [
      'ViewContent',
      'EditContent',
      'EditSettings',
      'AddRemoveContent',
      'AlignContent',
      'ViewFeedback',
      'AddFeedback',
    ],
  },
  projectAccessByRoleAndStatus: {
    Viewer: {
      '*': 'View',
    },
    Editor: {
      '*': 'View',
    },
  },
  contentAccessByRoleAndStatus: {
    Viewer: {
      '*': 'View',
    },
    Editor: {
      '*': 'Edit',
    },
  },
  domainRoleMapping: {},
};
