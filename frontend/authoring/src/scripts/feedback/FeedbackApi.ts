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

import gretchen from '../grfetchen/';

import { ApiQueryResults, encodeQuery } from '../srs/apiQuery';
import { ProjectResponse } from '../story/NarrativeMultiverse';

export const FeedbackStatusFilters = [
  'Open',
  'New',
  'Vendor Triage',
  'In Progress',
  'Review',
  'Closed',
] as const;

export type FeedbackStatusFilter = (typeof FeedbackStatusFilters)[number];

export const FeedbackStatuses = [
  'New',
  'Vendor Triage',
  'In Progress',
  'Review',
  'Done',
  'Denied',
] as const;

export type FeedbackStatus = (typeof FeedbackStatuses)[number];

export type FeedbackProfileDto = {
  id: number;
  handle: string;
  givenName: string;
  fullName: string;
  thumbnailId: number | null;
};

export type FeedbackSectionDto = {
  id: number;
  code: string;
  title: string;
  url: string;
  type: 'CourseSection' | 'TestSection' | 'PreviewSection';
};

export type FeedbackDto = {
  id: number;
  project: number;
  branch: number;
  remotes: number[];
  assetName: string;
  contentName: string | null;
  lessonName: string | null;
  moduleName: string | null;
  unitName: string | null;
  identifier: string | null;
  section: FeedbackSectionDto | null;
  created: string;
  creator: FeedbackProfileDto;
  modified: string | null;
  assignee: FeedbackProfileDto | null;
  status: FeedbackStatus | null;
  quote: string | null;
  feedback: string;
  role: string;
  attachments: number[];
  closed: boolean;
  replies: number;
};

export type FeedbackActivityDto = {
  id: number;
  created: string;
  edited: string | null;
  creator: FeedbackProfileDto;
  event: 'Status' | 'Assign' | 'Reply';
  value: null | string | FeedbackProfileDto; // should key off event but lazy
  attachments: number[];
};

export type FeedbackSummary = {
  unitName: string | null;
  moduleName: string | null;
  lessonName: string | null;
  contentName: string | null;
  assetName: string;
  count: number;
};

type FeedbackParams = {
  name?: string | string[];
  offset?: number;
  unit?: string;
  module?: string;
  status?: FeedbackStatusFilter;
  assignee?: number | null;
  person?: number; // assignee or creator
  closed?: boolean;
  remotes?: number;
  limit?: number;
  order?: 'asc' | 'desc' | null;
};

type UnpagedFeedbackParams = Omit<FeedbackParams, 'offset' | 'limit'>;

export type NewReply = {
  value: string;
  attachments: string[];
};

export type EditReply = {
  value: string;
};

export const MaxFeedbackLength = 4096; // just to stop people pasting in chapters

export const InitialStatus = FeedbackStatuses[0];
export const isInitialStatus = (status: FeedbackStatus) => status === InitialStatus;
export const isClosedStatus = (status: FeedbackStatus) => status === 'Done' || status === 'Denied';
export const nullInitialStatus = (status: FeedbackStatus) =>
  isInitialStatus(status) ? null : status;

export const feedbackColor = (status: FeedbackStatus | undefined) => {
  switch (status) {
    case 'Vendor Triage':
    case 'In Progress':
      return 'success';
    case 'Review':
      return 'warning';
    case 'Done':
      return 'dark';
    case 'Denied':
      return 'danger';
    default: // 'New'
      return 'primary';
  }
};

const feedbackQuery = (branchId: number | undefined, params: Partial<FeedbackParams>): string =>
  encodeQuery({
    offset: params.offset,
    limit: params.limit,
    order:
      params.order === null
        ? undefined
        : { property: 'created', direction: params.order ?? 'desc' },
    prefilter: [
      branchId && {
        property: 'branch',
        operator: 'eq',
        value: branchId,
      },
      params.remotes && {
        property: 'remotes',
        operator: 'intersects',
        value: params.remotes,
      },
      {
        property: 'archived',
        operator: 'eq',
        value: false,
      },
      params.name && {
        property: 'assetName',
        operator: Array.isArray(params.name) ? 'in' : 'eq',
        value: Array.isArray(params.name) ? params.name.join(',') : params.name,
      },
      {
        property: 'closed',
        operator: 'eq',
        value: params.closed ?? params.status === 'Closed',
      },
    ],
    filter: [
      params.unit ? { property: 'unitName', operator: 'eq', value: params.unit } : null,
      params.module ? { property: 'moduleName', operator: 'eq', value: params.module } : null,
      params.status === 'Open' || params.status === 'Closed' || !params.status
        ? null
        : params.status === InitialStatus
          ? { property: 'status', operator: 'isNull' }
          : { property: 'status', operator: 'eq', value: params.status },
      params.assignee === null
        ? { property: 'assignee', operator: 'isNull' }
        : params.assignee && { property: 'assignee', operator: 'eq', value: params.assignee },
    ],
  });

export const loadFeedback = (id: number): Promise<FeedbackDto> =>
  gretchen.get('/api/v2/feedback/:id').params({ id }).exec();

export const loadFeedbackActivity = (id: number): Promise<ApiQueryResults<FeedbackActivityDto>> =>
  gretchen.get('/api/v2/feedback/:id/activity;order=created:asc').params({ id }).exec();

export const downloadFeedbackUrl = (branchId: number, params: UnpagedFeedbackParams) =>
  `/api/v2/feedback/download.csv` + feedbackQuery(branchId, params);

export const loadFeedbacks = (
  branchId: number,
  params: FeedbackParams
): Promise<ApiQueryResults<FeedbackDto>> =>
  gretchen.get('/api/v2/feedback' + feedbackQuery(branchId, params)).exec();

export const archiveFeedback = (branchId: number): Promise<void> =>
  gretchen.post(`/api/v2/feedback/branches/${branchId}/archive`).exec();

export const countFeedbacks = (branchId: number, params: UnpagedFeedbackParams): Promise<number> =>
  gretchen.get('/api/v2/feedback/count' + feedbackQuery(branchId, params)).exec();

export const loadFeedbackIds = (
  branchId: number,
  params: UnpagedFeedbackParams
): Promise<number[]> =>
  gretchen
    .get('/api/v2/feedback/ids' + feedbackQuery(branchId, params))
    .exec()
    .then(result => result.objects);

export const loadFeedbackAssignees = (
  branchId: number,
  params: UnpagedFeedbackParams
): Promise<ApiQueryResults<FeedbackProfileDto>> =>
  gretchen
    .get('/api/v2/feedback/assignees' + feedbackQuery(branchId, { ...params, order: null }))
    .exec();

export const loadUpstreamFeedbackProjects = (branchId: number): Promise<ProjectResponse[]> =>
  gretchen
    .get(
      '/api/v2/feedback/upstreamProjects' +
        feedbackQuery(undefined, { order: null, remotes: branchId })
    )
    .exec()
    .then(({ projects }: { projects: ProjectResponse[] }) => projects);

export const profileImage = (profile: FeedbackProfileDto) =>
  profile.thumbnailId
    ? `/api/v2/profiles/${profile.handle}/thumbnail/${profile.thumbnailId};size=medium`
    : null;

export const editFeedback = (id: number, feedback: string): Promise<void> =>
  gretchen.put('/api/v2/feedback/:id').params({ id }).data({ feedback }).exec();

export const deleteFeedback = (id: number): Promise<void> =>
  gretchen.delete('/api/v2/feedback/:id').params({ id }).exec();

export const transitionFeedback = (
  id: number,
  status: string | null,
  closed: boolean
): Promise<void> =>
  gretchen.post('/api/v2/feedback/:id/transition').params({ id }).data({ status, closed }).exec();

export const assignFeedback = (id: number, assignee: number | null): Promise<void> =>
  gretchen.post('/api/v2/feedback/:id/assign').params({ id }).data({ assignee }).exec();

export const loadFeedbackSummary = (
  branchId: number,
  params: UnpagedFeedbackParams
): Promise<FeedbackSummary[]> =>
  gretchen
    .get('/api/v2/feedback/summary' + feedbackQuery(branchId, { ...params, order: null }))
    .exec()
    .then(res => res.objects);

export const postFeedbackReply = (id: number, reply: NewReply): Promise<FeedbackActivityDto> =>
  gretchen.post(`/api/v2/feedback/:id/reply`).params({ id }).data(reply).exec();

export const editFeedbackReply = (
  id: number,
  rid: number,
  reply: EditReply
): Promise<FeedbackActivityDto> =>
  gretchen.put(`/api/v2/feedback/:id/reply/:rid`).params({ id, rid }).data(reply).exec();

export const deleteFeedbackReply = (id: number, rid: number): Promise<void> =>
  gretchen.delete(`/api/v2/feedback/:id/reply/:rid`).params({ id, rid }).exec();

export const loadAssignees = (branchId: number): Promise<ApiQueryResults<FeedbackProfileDto>> =>
  gretchen.get('/api/v2/metaFeedback/branches/:branchId/assignees').params({ branchId }).exec();

export const postFeedback = ({
  project,
  branch,
  assetName,
  contentName,
  lessonName,
  moduleName,
  unitName,
  identifier,
  quote,
  feedback,
  attachments,
  assignee,
}: {
  project: number;
  branch: number;
  assetName: string;
  contentName?: string;
  lessonName?: string;
  moduleName?: string;
  unitName?: string;
  identifier?: string;
  quote?: string;
  feedback: string;
  attachments: {
    guid: string;
  }[];
  assignee?: number;
}): Promise<FeedbackDto> =>
  gretchen
    .post('/api/v2/feedback')
    .data({
      project,
      branch,
      assetName,
      contentName,
      lessonName,
      moduleName,
      unitName,
      identifier,
      assignee,
      quote,
      feedback,
      attachments: attachments.map(a => a.guid),
    })
    .exec();

export type FeedbackRelocate = {
  contentName?: string;
  lessonName?: string;
  moduleName?: string;
  unitName?: string;
};
export const relocateFeedback = (
  id: number,
  { contentName, lessonName, moduleName, unitName }: FeedbackRelocate
): Promise<FeedbackDto> =>
  gretchen
    .post(`/api/v2/feedback/:id/relocate`)
    .params({ id })
    .data({
      contentName,
      lessonName,
      moduleName,
      unitName,
    })
    .exec();
