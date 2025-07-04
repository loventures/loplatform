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
import { ApiQueryResults } from '../api/commonTypes';
import { ContentLite } from '../api/contentsApi';
import Course from '../bootstrap/course';

export type AssigneeDto = {
  id: number;
  fullName: string;
};

export type FeedbackDto = {
  id: number;
  assetName: string;
  edgePath?: string;
  created: string;
  modified: string;
  edited?: string;
  status?: string;
  assignee?: AssigneeDto;
  quote?: string;
  feedback: string;
  attachments: number[];
  closed: boolean;
  replies: FeedbackReplyDto[];
};

export type FeedbackReplyDto = {
  id: number;
  created: string;
  creator: AssigneeDto;
  reply: string;
  attachments: number[];
};

export const loadAssignees = (): Promise<AssigneeDto[]> =>
  axios
    .get<ApiQueryResults<AssigneeDto>>(`/api/v2/lwc/${Course.id}/feedback/assignees`, {
      headers: { 'X-UserId': '' }, // feedback should always come as the authentic user
    })
    .then(res => res.data.objects);

export const postFeedback = (
  content: ContentLite,
  assetName: string | undefined,
  quote: string | undefined,
  id: string | undefined,
  feedback: string,
  attachments: { guid: string }[],
  assignee: number | undefined
): Promise<void> =>
  axios.post(
    `/api/v2/lwc/${Course.id}/feedback/${content.id}`,
    {
      assetName,
      quote,
      id,
      feedback,
      attachments: attachments.map(a => a.guid),
      assignee,
    },
    {
      headers: { 'X-UserId': '' }, // feedback should always come as the authentic user
    }
  );

export const getFeedback = (content: ContentLite): Promise<FeedbackDto[]> =>
  axios
    .get(`/api/v2/lwc/${Course.id}/feedback/${content.id}`, {
      headers: { 'X-UserId': '' }, // feedback should always come as the authentic user
    })
    .then(res => res.data.objects);

export const getFeedbackAttachmentUrl = (feedback: number, attachment: number) =>
  `/api/v2/lwc/${Course.id}/feedback/${feedback}/attachments/${attachment}`;

export const postFeedbackReply = (feedback: number, reply: string): Promise<void> =>
  axios.post(
    `/api/v2/lwc/${Course.id}/feedback/${feedback}/reply`,
    {
      reply,
    },
    {
      headers: { 'X-UserId': '' }, // feedback should always come as the authentic user
    }
  );

export const closeFeedback = (feedback: number): Promise<void> =>
  axios.post(
    `/api/v2/lwc/${Course.id}/feedback/${feedback}/close`,
    {},
    {
      headers: { 'X-UserId': '' }, // feedback should always come as the authentic user
    }
  );

export const reopenFeedback = (feedback: number): Promise<void> =>
  axios.post(
    `/api/v2/lwc/${Course.id}/feedback/${feedback}/reopen`,
    {},
    {
      headers: { 'X-UserId': '' }, // feedback should always come as the authentic user
    }
  );
