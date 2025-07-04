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

import axios, { AxiosResponse } from 'axios';
import { ApiQueryResults, EdgePath, SrsList } from '../api/commonTypes';
import { ContentLite } from '../api/contentsApi';
import Course from '../bootstrap/course';
import { createUrl, encodeQuery, loConfig, MatrixQuery } from '../bootstrap/loConfig';

import { COURSE_ROOT } from '../utilities/courseRootType';

export type NewQuestionDto = {
  edgePath: EdgePath;
  html: string;
  attachments: string[];
};

export type NewMessageDto = {
  html: string;
  attachments: string[];
  category?: string;
  subcategory?: string;
};

export type MulticastDto = {
  html: string;
  subject: string;
  attachments: string[];
  recipients: number[];
};

export type RecategorizeDto = {
  category?: string;
  subcategory?: string;
};

export type QnaQuestionDto = {
  id: number;
  sectionId: number;
  edgePath: EdgePath;
  created: Date;
  creator: UserDto;
  modified: Date;
  subject?: string;
  open: boolean;
  closed: boolean;
  messages: QnaMessageDto[];
  reopened?: boolean; // Front-End Fakery to allow you to reopen a question...
  category?: string;
  subcategory?: string;
  instructorMessage?: boolean;
  recipients?: UserDto[];
};

export type QnaMessageDto = {
  id: number;
  created: Date;
  edited?: Date;
  creator: UserDto;
  html: string;
  attachments: QnaAttachment[];
};

export type QnaAttachment = {
  id: number;
  fileName: string;
  size: number;
};

export type QnaSummary = {
  edgePath: EdgePath;
  count: number;
  open: number;
  answered: number;
};

type UserDto = {
  id: number;
  fullName: string;
  externalId: string;
};

export const fetchQnaQuestion = (questionId: number) =>
  axios
    .get<QnaQuestionDto>(
      createUrl(loConfig.qna.getQuestion, { context: Course.id, id: questionId })
    )
    .then(resp => resp.data);

export const fetchQnaQuestions = (query: MatrixQuery) => {
  return axios
    .get<ApiQueryResults<QnaQuestionDto>>(
      createUrl(loConfig.qna.getQuestions, {
        context: Course.id,
      }) + encodeQuery(query)
    )
    .then(resp => resp.data);
};

export const fetchQnaQuestionIds = (query: MatrixQuery) => {
  return axios
    .get<ApiQueryResults<number>>(
      createUrl(loConfig.qna.getQuestionIds, {
        context: Course.id,
      }) + encodeQuery(query)
    )
    .then(resp => resp.data);
};

export const fetchQnaSummaries = (query: MatrixQuery) => {
  return axios
    .get<SrsList<QnaSummary>>(
      createUrl(loConfig.qna.getSummary, {
        context: Course.id,
      }) + encodeQuery(query)
    )
    .then(resp => resp.data.objects);
};

export const postNewQuestion = (
  content: ContentLite,
  questionText: string,
  attachments: { guid: string }[]
): Promise<QnaQuestionDto> => {
  return axios
    .post<QnaQuestionDto, AxiosResponse<QnaQuestionDto>, NewQuestionDto>(
      createUrl(loConfig.qna.addQuestion, { context: Course.id }),
      {
        edgePath: content.id == Course.id.toString() ? COURSE_ROOT : content.id,
        html: questionText,
        attachments: attachments.map(a => a.guid),
      }
    )
    .then(resp => resp.data);
};

export const addNewMessage = (
  questionId: number,
  messageText: string,
  attachments: { guid: string }[],
  category?: string,
  subcategory?: string
): Promise<QnaQuestionDto> => {
  return axios
    .post<QnaQuestionDto, AxiosResponse<QnaQuestionDto>, NewMessageDto>(
      createUrl(loConfig.qna.addMessage, { context: Course.id, id: questionId }),
      {
        html: messageText,
        attachments: attachments.map(a => a.guid),
        category,
        subcategory,
      }
    )
    .then(resp => resp.data);
};

export const instructorCloseMessage = (
  questionId: number,
  category?: string,
  subcategory?: string
): Promise<QnaQuestionDto> => {
  return axios
    .post<QnaQuestionDto, AxiosResponse<QnaQuestionDto>, RecategorizeDto>(
      createUrl(loConfig.qna.instructorClose, { context: Course.id, id: questionId }),
      {
        category,
        subcategory,
      }
    )
    .then(resp => resp.data);
};

export const putCategorization = (
  questionId: number,
  category?: string,
  subcategory?: string
): Promise<QnaQuestionDto> => {
  return axios
    .post<QnaQuestionDto, AxiosResponse<QnaQuestionDto>, RecategorizeDto>(
      createUrl(loConfig.qna.recategorize, { context: Course.id, id: questionId }),
      {
        category,
        subcategory,
      }
    )
    .then(resp => resp.data);
};

export const closeQuestion = (questionId: number) => {
  return axios
    .post<QnaQuestionDto>(
      createUrl(loConfig.qna.closeQuestion, { context: Course.id, id: questionId })
    )
    .then(resp => resp.data);
};

export const getMessageAttachmentUrl = (
  question: number,
  message: number,
  attachment: number,
  download?: boolean
) =>
  `/api/v2/lwc/${Course.id}/qna/${question}/message/${message}/${attachment}${
    download ? '?download=true' : ''
  }`;

export const getMessageAttachmentSignedUrl = (
  question: number,
  message: number,
  attachment: number
) => `/api/v2/lwc/${Course.id}/qna/${question}/message/${message}/${attachment}/url`;

export const multicast = (
  recipients: number[],
  subject: string,
  messageText: string,
  attachments: { guid: string }[]
): Promise<number> => {
  return axios
    .post<QnaQuestionDto, AxiosResponse<number>, MulticastDto>(
      createUrl(loConfig.qna.multicast, { context: Course.id }),
      {
        recipients,
        subject,
        html: messageText,
        attachments: attachments.map(a => a.guid),
      }
    )
    .then(resp => resp.data);
};

export const multicastReply = (
  messageId: number,
  messageText: string,
  attachments: { guid: string }[]
): Promise<QnaQuestionDto> => {
  return axios
    .post<QnaQuestionDto, AxiosResponse<QnaQuestionDto>, NewMessageDto>(
      createUrl(loConfig.qna.multicastReply, { context: Course.id, id: messageId }),
      {
        html: messageText,
        attachments: attachments.map(a => a.guid),
      }
    )
    .then(resp => resp.data);
};
