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

import axios from 'axios';
import { createUrl, loConfig } from '../bootstrap/loConfig';
import { find, groupBy, isEmpty, map, orderBy } from 'lodash';
import { Option } from '../types/option';

import { Tree, leaf, tree } from '../instructorPages/customization/Tree';
import { Competency } from '../resources/CompetencyResource';
import { SrsList } from './commonTypes';

export type ContentId = string;

export enum GateStatus {
  Open = 'OPEN',
  ReadOnly = 'READ_ONLY',
  Locked = 'LOCKED',
}

export type SingleActivityGate = {
  assignmentId: string;
  threshold: number;
  disabled: boolean;
};

export type ActivityGateInfo = {
  gates: SingleActivityGate[];
};

export type TemporalGateInfo = {
  lockDate: string;
};

export type RightsGatingInfo = {
  policyType: 'TRIAL_LEARNER' | 'RESTRICTED_LEARNER';
};

export type ContentGate = {
  enabled: boolean;
  activityGatingPolicy: Option<ActivityGateInfo>;
  temporalGatingPolicy: Option<TemporalGateInfo>;
  rightsGatingPolicy: Option<RightsGatingInfo>;
};

export type GatingInformation = {
  gate: Option<ContentGate>;
  gateStatus: GateStatus;
};

export type IncrementType = 'VISITED' | 'TESTEDOUT' | 'SKIPPED';

export type Progress = {
  completions: number;
  total: number;
  progressTypes: IncrementType[];
  weightedPercentage: number;
};

export type Graded = {
  grade: number;
  max: number;
  date: string;
};

export type Ungraded = {
  max: number;
};

export type Pending = {
  max: number;
  date: string;
};

export type Rollup = {
  grade: number;
  max: number;
  latestChange: string;
};

export type NoCredit = {
  grade: number;
  max: number;
  date: string;
};

export type ExtraCredit = {
  grade: number;
  date: string;
};

export function isUngraded(grade: Grade): grade is Ungraded {
  // if there is no 'grade' prop, it's ungraded
  return !('grade' in grade);
}

export type Grade = Graded | Ungraded | Pending | Rollup | NoCredit | ExtraCredit;

export type Content = {
  id: string;
  parent_id: Option<string>;
  name: string; // TODO: rename to title
  assetId: number;
  description: Option<string>;
  contentType: string; // todo: kill
  index: number;
  path: string;
  logicalGroup: 'elements'; // todo: kill
  depth: number;
  typeId: string; // todo: enumerate
  iconClass: string;
  dueDate: string | null;
  dueDateExempt: boolean | null;
  maxMinutes: Option<number>;
  duration: Option<number>;
  hasGradebookEntry: boolean;
  isForCredit?: boolean;
  node_name: string;
  gatingInformation: GatingInformation;
  progress?: Progress; // todo: is this really optional?
  grade?: Grade;
  contentId: string;
  contentHeight: Option<number>;
  resourcePath: Option<string>;
  competencies: Competency[];
  metadata: null;
  hasSurvey: boolean;
  subType: Option<string>;
  accessControlled: Option<boolean>;
  gradebookCategory: Option<string>;
  hyperlinks: Record<string, string>; // edge-id -> edge-path
  bannerImage: Option<string>;
};

export type ContentTypeAndName<T extends string> = {
  typeId: T;
  node_name: string;
};

export type ContentLite<T extends string = string> = ContentTypeAndName<T> & {
  id: string;
  name: string;
  path: string;
};

export const buildContentTree = (contents: Content[]): Tree<Content> => {
  const root = find(contents, { depth: 0 })!;
  const contentByParentId = groupBy(contents, 'parent_id');
  const buildNode = (content: Content): Tree<Content> => {
    const children = orderBy(contentByParentId[content.id], 'index');
    if (isEmpty(children)) {
      return leaf(content);
    } else {
      const childNodes = map(children, child => buildNode(child));
      return tree(content, ...childNodes);
    }
  };

  return buildNode(root);
};

export function fetchContents(courseId: number, userId?: number): Promise<SrsList<Content>> {
  const url = createUrl(loConfig.courseContents.contents, { context: courseId, user: userId });

  return axios.get(url).then(({ data }) => data);
}
