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
import { Option } from '../types/option';

import { CreditType } from '../utilities/creditTypes';
import { ApiQueryResults } from './commonTypes';
import { ContentId } from './contentsApi';

export type GradebookColumn = {
  id: ContentId;
  name: string;
  maximumPoints: number;
  type: string; //@TODO enum
  credit: CreditType;
  category_id: ContentId;
  Category: GradebookCategory;
  syncHistory: any; //@TODO make this happen
};

export type GradebookCategory = {
  id: ContentId;
  name: string;
  weight: number;
  displayOrder: number;
  isUncategorized: boolean;
  dropLowest: boolean;
  gradeDisplay: string;
  gradeAggregationStrategy: string;
};

type LtiItemSyncStatus<T> = Queued | Attempted | Failed | Synced<T>;

type Queued = {
  type: 'Queued';
  time: string;
};
type Synced<T> = {
  type: 'Synced';
  syncedValue: T;
};
type Attempted = {
  type: 'Attempted';
  time: string;
  error: LtiItemSyncError;
};
type Failed = {
  attempt: string;
  error: LtiItemSyncError;
};

type LtiItemSyncError = InternalError | HttpError;

type InternalError = {
  errorMessage: string;
};
type HttpError = {
  req: Request;
  resp: { left: Response } | { right: string }; // todo: is this right?
};

type Request = {
  url: string;
  body: string;
  method: string;
};

type Response = {
  body: Option<string>;
  contentType: string;
  status: number;
};

export type ColumnSyncHistory = {
  ags: Option<LtiItemSyncStatus<any>[]>;
};

export type Column = {
  id: string;
  name: string;
  credit: CreditType;
  weight: number;
  hideGradeFromStudents: boolean;
  type: string;
  dueDate: Option<string>;
  displayOrder: number;
  externalId: string;
  category_id: string;
  maximumPoints: number;
  Category: GradebookCategory;
  contentItemId: number;
  gradeTransformationStrategy: string;
  syncHistory: Option<ColumnSyncHistory>;
};

export function fetchColumns(courseId: number): Promise<ApiQueryResults<Column>> {
  // loConfig.gradebook.columns
  return axios.get(`/api/v2/lwgrade2/${courseId}/gradebook/columns`).then(({ data }) => data);
}

export function getLastManualSync(courseId: number): Promise<string | null> {
  // loConfig.gradebook.syncHistory
  return axios
    .get(`/api/v2/lwgrade2/${courseId}/gradebook/grades/syncHistory`)
    .then(({ data }) => data.lastManualSync); // null means never manually synced
}

export function syncEntireGradebook(courseId: number): Promise<string> {
  // loConfig.gradebook.syncHistory
  return axios
    .post(`/api/v2/lwgrade2/${courseId}/gradebook/grades/syncHistory`)
    .then(({ data }) => data.lastManualSync);
}
