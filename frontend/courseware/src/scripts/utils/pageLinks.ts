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

import { UserInfo } from '../../loPlatform';
import { Content, ContentLite } from '../api/contentsApi';
import {
  FromApp,
  createCompetencyContentLink,
  createContentLink,
  createDashboardLink,
  createLink,
  createLinkWithRole,
  createPrintLink,
} from '../utils/linkUtils';
import { LocationDescriptorObject } from 'history';
import { match, matchPath } from 'react-router';

export type ParameterizedLoLinkBuilder<
  Params,
  FromParams extends { [K in keyof FromParams]?: string } = any,
> = {
  match: (path: string) => match<FromParams> | null;
  toLink: (params: Params) => LocationDescriptorObject<FromApp>;
};

export type LoLinkBuilder = {
  match: (path: string) => match<any> | null;
  toLink: () => LocationDescriptorObject<FromApp>;
};

export function isLinkBuilder(to: string | LoLinkBuilder): to is LoLinkBuilder {
  return typeof to === 'object' && 'toLink' in to;
}

/*
  Common
*/
export const SendMessagePageLink: ParameterizedLoLinkBuilder<any> = {
  match: path => matchPath(path, { path: '/send-message' }),
  toLink: searchParams => createLink('/send-message', searchParams),
};

export const CoursePageLink: LoLinkBuilder = {
  match: path =>
    matchPath(path, {
      path: '/(instructor|student)/content',
      exact: true,
      strict: true,
    }),
  toLink: () => createLinkWithRole('/content'),
};

export const CourseDashboardLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/(instructor|student)/dashboard', exact: true }),
  toLink: () => createDashboardLink(),
};

export const ContentPlayerPageLink: ParameterizedLoLinkBuilder<
  {
    content: { id: string };
    nav?: string;
    qna?: boolean;
  },
  {
    contentId: string;
  }
> = {
  match: path => matchPath(path, { path: '/(instructor|student)/content/:contentId' }),
  toLink: ({ content, ...searchParams }) => createContentLink(content, searchParams),
};

export const ContentPrinterPageLink: ParameterizedLoLinkBuilder<
  {
    content: { id: string };
    questions?: boolean; // false to omit questions from assessments
    answers?: boolean; // false to omit answers from assessments
  },
  {
    contentId: string;
  }
> = {
  match: path => matchPath(path, { path: '/(instructor|student)/print/:contentId' }),
  toLink: ({ content, ...searchParams }) => createPrintLink(content, searchParams),
};

export const DiscussionListPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/(instructor|student)/discussions' }),
  toLink: () => createLinkWithRole('/discussions'),
};

/*
  Preview
*/
export const InstructorPreviewEnterLink: ParameterizedLoLinkBuilder<{
  learner: UserInfo;
}> = {
  match: path => matchPath(path, '/student/dashboard'),
  toLink: ({ learner }) => createLink('/student/dashboard', { previewAsUserId: learner.id }),
};

export const InstructorPreviewExitLink: LoLinkBuilder = {
  match: path => matchPath(path, '/instructor/learners'),
  toLink: () => createLink('/instructor/learners', { previewAsUserId: void 0 }),
};

/*
  Learner
*/
export const LearnerDashboardPageLink: LoLinkBuilder = {
  match: path => matchPath(path, '/student/dashboard'),
  toLink: () => createLink('/student/dashboard'),
};

export const LearnerCompetenciesPageLink: LoLinkBuilder = {
  match: path =>
    matchPath(path, {
      path: '/student/competencies',
      exact: true,
      strict: true,
    }),
  toLink: () => createLink('/student/competencies'),
};

export const LearnerCompetencyDetailPageLink: ParameterizedLoLinkBuilder<{
  competency: {
    id: number;
  };
}> = {
  match: path => matchPath(path, { path: '/student/competencies/:competencyId' }),
  toLink: ({ competency }) => createLink(`/student/competencies/${competency.id}`),
};

export const LearnerCompetencyPlayerPageLink: ParameterizedLoLinkBuilder<
  {
    content: Content;
    competency: {
      id: number;
    };
  },
  {
    competencyId: string;
    contentId: string;
  }
> = {
  match: path =>
    matchPath(path, {
      path: '/student/competencies/:competencyId/content/:contentId',
    }),
  toLink: ({ competency, content }) => createCompetencyContentLink(content, competency.id),
};

export const LearnerAssignmentListPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/student/assignments' }),
  toLink: () => createLink('/student/assignments'),
};

/*
  Instructor
*/
export const InstructorDashboardPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/dashboard' }),
  toLink: () => createLink('/instructor/dashboard'),
};

export const InstructorCompetenciesPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/competencies' }),
  toLink: () => createLink('/instructor/competencies'),
};

export const InstructorLearnerListPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/learners' }),
  toLink: () => createLink('/instructor/learners'),
};

export const InstructorProgressReportPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/progress-report' }),
  toLink: ({
    contentId,
  }: {
    contentId?: number;
  } = {}) => createLink('/instructor/progress-report', { contentId }),
};

export const InstructorGradebookLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/gradebook/' }),
  toLink: () => createLink('/instructor/gradebook/'),
};

export const InstructorGradebookGradesPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/gradebook/grades' }),
  toLink: () => createLink('/instructor/gradebook/grades'),
};

export const InstructorGradebookSyncColumnPageLink: ParameterizedLoLinkBuilder<{
  columnId: number;
}> = {
  match: path =>
    matchPath(path, {
      path: '/instructor/gradebook/grades/syncHistory/:columnId',
    }),
  toLink: ({ columnId }) => createLink(`/instructor/gradebook/grades/syncHistory/${columnId}`),
};

export const InstructorGradebookSyncColumnLearnerPageLink: ParameterizedLoLinkBuilder<{
  columnId: number;
  learnerId: number;
}> = {
  match: path =>
    matchPath(path, {
      path: '/instructor/gradebook/grades/syncHistory/:columnId/:learnerId',
    }),
  toLink: ({ columnId, learnerId }) =>
    createLink(`/instructor/gradebook/grades/syncHistory/${columnId}/${learnerId}`),
};

export const InstructorLearnerAssignmentsPageLink: ParameterizedLoLinkBuilder<number> = {
  match: path => matchPath(path, { path: '/instructor/gradebook/learner-assignments' }),
  toLink: forLearnerId => createLink('/instructor/gradebook/learner-assignments', { forLearnerId }),
};

export const InstructorGradebookAssignmentsPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/gradebook/assignments' }),
  toLink: () => createLink('/instructor/gradebook/assignments'),
};

export const InstructorGradebookGatingPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/gradebook/gating' }),
  toLink: () => createLink('/instructor/gradebook/gating'),
};

export const InstructorGradebookAccommodationsPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/gradebook/accommodations' }),
  toLink: () => createLink('/instructor/gradebook/accommodations'),
};

export const InstructorAssignmentListPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/assignments', exact: true }),
  toLink: () => createLink('/instructor/assignments'),
};

export const InstructorAssignmentOverviewPageLink: ParameterizedLoLinkBuilder<{
  contentId: string;
}> = {
  match: path => matchPath(path, { path: '/instructor/assignments/:contentId' }),
  toLink: ({ contentId }) => createLink(`/instructor/assignments/${contentId}`),
};

export const InstructorGraderPageLink: ParameterizedLoLinkBuilder<{
  contentId: string;
  forLearnerId: number;
  attemptId?: number;
  questionIndex?: number;
}> = {
  match: path => matchPath(path, { path: '/instructor/assignments/:contentId/grader' }),
  toLink: ({ contentId, forLearnerId, attemptId, questionIndex }) =>
    createLink(`/instructor/assignments/${contentId}/grader`, {
      forLearnerId,
      attemptId,
      questionIndex,
      previewAsUserId: undefined,
    }),
};

export const InstructorControlsLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/controls/' }),
  toLink: () => createLink('/instructor/controls/'),
};

export const InstructorControlsHomeLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/controls/home' }),
  toLink: () => createLink('/instructor/controls/home'),
};

export const InstructorControlsCustomizePageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/controls/customize' }),
  toLink: () => createLink('/instructor/controls/customize'),
};

export const InstructorControlsPoliciesPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/controls/policies' }),
  toLink: () => createLink('/instructor/controls/policies'),
};

export const InstructorControlsLinkCheckerPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/controls/link-checker' }),
  toLink: () => createLink('/instructor/controls/link-checker'),
};

export const InstructorControlsDiscussionPurgePageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/controls/purge-posts' }),
  toLink: () => createLink('/instructor/controls/purge-posts'),
};
export const InstructorControlsContentSearchPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/controls/content-search' }),
  toLink: () => createLink('/instructor/controls/content-search'),
};

export const InstructorControlsContentFeedbackPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/controls/content-feedback' }),
  toLink: () => createLink('/instructor/controls/content-feedback'),
};

export const InstructorControlsCourseKeyLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/controls/course-key' }),
  toLink: () => createLink('/instructor/controls/course-key'),
};

export const BookmarksLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/(instructor|student)/bookmarks', exact: true }),
  toLink: () => createLinkWithRole('/bookmarks'),
};

export const SearchLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/(instructor|student)/search', exact: true }),
  toLink: () => createLinkWithRole('/search'),
};

export const AnalyticsPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/analytics' }),
  toLink: () => createLink('/instructor/analytics'),
};

export const InstructorLearnerAnalyticsPageLink: ParameterizedLoLinkBuilder<number> = {
  match: path => matchPath(path, { path: '/instructor/analytics' }),
  toLink: forLearnerId => createLink('/instructor/analytics', { forLearnerId }),
};

export const QnaPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/(instructor|student)/qna' }),
  toLink: () => createLinkWithRole('/qna'),
};

export const InstructorQnaPreviewPageLink: ParameterizedLoLinkBuilder<string> = {
  match: path => matchPath(path, { path: '/instructor/qna/question/:questionId' }),
  toLink: questionId => createLink(`/instructor/qna/question/${questionId}`),
};

export const InstructorQnaMulticastPageLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/instructor/qna/send' }),
  toLink: () => createLink('/instructor/qna/send'),
};

export const StudentQnaQuestionLink: ParameterizedLoLinkBuilder<number> = {
  match: path => matchPath(path, { path: '/student/qna/:questionId' }),
  toLink: questionId => createLink(`/student/qna/${questionId}`, { qna: true }),
};

export const StudentQnaNewQuestionLink: LoLinkBuilder = {
  match: path => matchPath(path, { path: '/student/qna' }),
  toLink: () => createLink('/student/qna', { qna: true }),
};
