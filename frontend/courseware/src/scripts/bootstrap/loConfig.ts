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

import { StrictParamsObject } from '../api/templateTypes';
import { replace } from 'lodash';
import qs from 'qs';

/**
 Creates a URL that matches this pattern by substituting the specified values
 for the path and search parameters. Null values for path parameters are treated
 as empty strings.

 Example:
 createUrl('/user/{id}?q,z', { id:'bob', q:'yes' });
 // returns '/user/bob?q=yes'
 createUrl('/user/{id}?q,z', { id:'bob', q:'yes', z:'sure' });
 // returns '/user/bob?q=yes&z=sure'

 Currently used by UrlBuilder in lieu of a ui-router dependency.
 UrlBuilder also removes trailing slashes and adds matrix params not in the
 original pattern. All of these functions can be pulled out of Angular and
 combined into a single class.

 * */

export const createUrl = <T extends string = string>(
  url: T,
  parameters: StrictParamsObject<T>
): string => {
  const queryParams: Record<string, any> = {};
  let allowedParams: string[] = [];

  const [baseUrl, search, ...somethingsWrong] = url.split('?');
  if (somethingsWrong.length > 0) {
    console.error('The passed URL pattern has more than one "?" delimiter');
  }
  if (search) {
    allowedParams = search.split(',');
  }

  let interpolatedUrl = Object.keys(parameters).reduce((currentUrl, paramKey) => {
    const parameter = parameters[paramKey as unknown as keyof StrictParamsObject<T>];
    if (parameter === undefined || parameter === null || Number.isNaN(parameter)) {
      return currentUrl;
    }
    if (currentUrl.includes(`{${paramKey}}`)) {
      return currentUrl.replace(`{${paramKey}}`, parameter.toString());
    } else {
      queryParams[paramKey] = parameters[paramKey as unknown as keyof StrictParamsObject<T>];
      return currentUrl;
    }
  }, baseUrl);

  const paramsRemain = interpolatedUrl.match(/\{\w+\}/); // check for missing params
  if (paramsRemain) {
    interpolatedUrl = replace(interpolatedUrl, /\{\w+\}/g, '');
  }

  const filteredParams = allowedParams.reduce<Record<string, any>>((acc, param) => {
    if (queryParams.hasOwnProperty(param)) {
      acc[param] = queryParams[param];
      return acc;
    }
    return acc;
  }, {});

  /** manually set the array format so we get ?abc=123&abc=456 instead of ?abc[0]=123&abc[1]=456 */
  const queryParamsStringified = qs.stringify(filteredParams, { arrayFormat: 'repeat' });

  return queryParamsStringified.length > 0
    ? `${interpolatedUrl}?${queryParamsStringified}`
    : interpolatedUrl;
};

export const urlBase = window.location.protocol + '//' + window.location.host;

const encodeValue = (value: string) => {
  return encodeURIComponent(value).replace(/\)/g, '%29');
};

export type ActualMatrixFilter = {
  property: string;
  operator?: string;
  value?: string | number | boolean;
};

export type MatrixFilter = null | undefined | false | string | ActualMatrixFilter;

const isFiltery = (f: MatrixFilter): f is Exclude<MatrixFilter, null | undefined | false> => !!f;

export const isActualMatrixFilter = (f: MatrixFilter): f is ActualMatrixFilter =>
  isFiltery(f) && typeof f !== 'string';

const encodeFilter = (v: MatrixFilter | MatrixFilter[]) =>
  (Array.isArray(v) ? v : [v])
    .filter(isFiltery)
    .map(o =>
      typeof o === 'string'
        ? o
        : o.property +
          (o.operator ? ':' + o.operator : '') +
          '(' +
          encodeValue((o.value as string) ?? '') +
          ')'
    )
    .join(',');

export type MatrixOrder =
  | null
  | undefined
  | false
  | string
  | {
      property: string;
      direction: 'asc' | 'desc' | 'ascNullsFirst' | 'descNullsLast';
    };

const isOrdery = (o: MatrixOrder): o is Exclude<MatrixOrder, null | undefined | false> => !!o;

const encodeOrder = (v: MatrixOrder | MatrixOrder[]) =>
  (Array.isArray(v) ? v : [v])
    .filter(isOrdery)
    .map(o => (typeof o === 'string' ? o : o.property + ':' + o.direction))
    .join(',');

const matrixParamHandlers = {
  order: encodeOrder,
  filter: encodeFilter,
  prefilter: encodeFilter,
  embed: (v: any) => (Array.isArray(v) ? v.join(',') : v),
} as const;

export type MatrixQuery = {
  offset?: number;
  limit?: number;
  order?: MatrixOrder | MatrixOrder[];
  filter?: MatrixFilter | MatrixFilter[];
  prefilter?: MatrixFilter | MatrixFilter[];
};

export const encodeQuery = (matrix: MatrixQuery) => {
  return (
    ';' +
    Object.keys(matrix)
      .map(k => {
        const v = matrix[k as keyof MatrixQuery];
        const h = matrixParamHandlers[k as keyof typeof matrixParamHandlers];
        const encoded = v != null && h ? h(v) : v;
        return encoded != null && encoded !== '' ? k + '=' + encoded : null;
      })
      .filter(m => m != null)
      .join(';')
  );
};

/**
 * TODO: use https://ja.nsommer.dk/articles/type-checked-url-router.html as inspiration for prop-checking urls
 * */
export const loConfig = {
  noop: '/dev/null',
  control: '/control/component', // unused
  domain: '/api/v2/domain', // unused
  i18n: '/api/v2/i18n/{locale}/{component}',
  user: {
    // all of these are used in SessionService.js
    self: '/api/v2/users/self',
    profileImage: '/api/v2/users/self/image/view;size={size}',
    preferences: '/api/v2/users/self/preferences',
    logout: '/api/v2/sessions/logout',
    login: '/api/v2/sessions/login',
    exit: '/api/v2/sessions/exit',
    reset: '/api/v2/passwords/reset',
    recover: '/api/v2/passwords/recover',
    loginMechanisms: '/api/v2/domain/loginMechanisms',
  },
  session: '/api/v0/session',
  lti: {
    launchConfig: '/api/v2/lwc/{context}/lti/redirect/{path}?role',
    launchStyle: '/api/v2/lwc/{context}/lti/{path}/style',
  },
  courseLink: {
    launch: '/api/v2/lwc/{context}/course/{path}/launch?role',
  },
  //=============================
  enrollment: {
    users: '/api/v2/contexts/{contextId}/roster',
    user: '/api/v2/contexts/{contextId}/roster/{userId}',
    drop: '/api/v2/contexts/{contextId}/roster?userId',
  },
  //=============================
  discussionBoard: {
    list: '/api/v2/discussion/boards?userId',
    oneBoard: '/api/v2/discussion/boards/{discussion}?userId',
    visit: '/api/v2/discussion/boards/{discussion}/visit',
    jumpbar: '/api/v2/discussion/boards/{discussion}/jumpbarSummary',
    userPostCount: '/api/v2/discussion/boards/{discussion}/userPostCount',
    close: '/api/v2/discussion/boards/close',
  },
  discussionPost: {
    attachment: '/api/v2/discussion/posts/{postId}/attachments/{attachmentId}?download,direct,size',
    list: '/api/v2/discussion/posts',
    listUnresponded: '/api/v2/discussion/posts/unresponded',
    listNew: '/api/v2/discussion/posts/new',
    listUnread: '/api/v2/discussion/posts/unread',
    listBookmarked: '/api/v2/discussion/posts/bookmarked',
    listUserPosts: '/api/v2/discussion/posts/forUserHandle/{userHandle}',
    onePost: '/api/v2/discussion/posts/{postId}',

    toggleRemove: '/api/v2/discussion/posts/{postId}/remove',
    togglePin: '/api/v2/discussion/posts/{postId}/pin',
    toggleInappropriate: '/api/v2/discussion/posts/{postId}/inappropriate',
    reportInappropriate: '/api/v2/discussion/posts/{postId}/reportInappropriate',
    toggleBookmark: '/api/v2/discussion/posts/{postId}/bookmark',
    toggleViewed: '/api/v2/discussion/posts/{postId}/viewed',
  },
  //============================= // Notification Service
  notifications: {
    base: '/api/v2/notifications',
    single: '/api/v2/notifications/{notification}',
  },
  alerts: {
    base: '/api/v2/alerts',
    summary: '/api/v2/alerts/summary',
    markAsViewed: '/api/v2/alerts/{alertId}/view',
    viewed: '/api/v2/alerts/viewed',
  },
  announcements: {
    active: '/api/v2/lwc/{context}/announcements/active',
    hide: '/api/v2/lwc/{context}/announcements/{id}/hide',
  },
  //============================= QuizAPI QuizOverviewAPI
  quiz: {
    get: '/api/v2/lwQuiz/{contentId}',
    getQuestions: '/api/v2/lwQuiz/{contentId}/questions',
    attempts: '/api/v2/quizAttempt',
    attempt: '/api/v2/quizAttempt/{attemptId}',
    overview: '/api/v2/assessment/{quizId}/gradingOverview',
    score: '/api/v2/quizAttempt/{attemptId}/score',
    feedback: '/api/v2/quizAttempt/{attemptId}/feedback',
    invalidate: '/api/v2/quizAttempt/{attemptId}/invalidate',
    attachment: '/api/v2/quizAttempt/{attemptId}/attachments/{attachmentId}?download,direct,size',
  },
  scorm: {
    submit: '/api/v2/lwc/{contextId}/scorm/{contentId}/submit',
  },
  instructorNotification: {
    notify: '/api/v2/lwc/{context}/instructorNotification',
  },
  submissionAssessment: {
    oneAssessment: '/api/v2/submissionAssessment/{assessmentId}',
  },
  //============================= SubmissionActivityAPI
  submissionAssessmentAttempt: {
    attempts: '/api/v2/submissionAssessmentAttempt',
    oneAttempt: '/api/v2/submissionAssessmentAttempt/{attemptId}',
    submit: '/api/v2/submissionAssessmentAttempt/{attemptId}/submit',
    score: '/api/v2/submissionAssessmentAttempt/{attemptId}/score',
    feedback: '/api/v2/submissionAssessmentAttempt/{attemptId}/feedback',
    attachment:
      '/api/v2/submissionAssessmentAttempt/{attemptId}/attachments/{attachmentId}?download,direct,size',
    attachmentUrl: '/api/v2/submissionAssessmentAttempt/{attemptId}/attachments/{attachmentId}/url',
    invalidate: '/api/v2/submissionAssessmentAttempt/{attemptId}/invalidate',
  },
  //=============================
  authoring: {
    course: '/api/v2/courseAuthoring/section/{courseId}',
    content: '/api/v2/courseAuthoring/section/{courseId}/content/{contentId}',
  },
  course: {
    context: '/api/v2/contexts/{courseId}',
    banner: '/api/v2/lwc/{context}/banner',
  },
  gradebook: {
    grades: '/api/v2/lwgrade2/{courseId}/gradebook/grades',
    oneGrade: '/api/v2/lwgrade2/{courseId}/gradebook/grades/grade',
    removeOverride: '/api/v2/lwgrade2/{courseId}/gradebook/grades/removeOne',
    columns: '/api/v2/lwgrade2/{courseId}/gradebook/columns',
    oneColumn: '/api/v2/lwgrade2/{courseId}/gradebook/columns/{columnId}',
    categories: '/api/v2/lwgrade2/{courseId}/gradebook/categories',
    oneCategory: '/api/v2/lwgrade2/{courseId}/gradebook/categories/{categoryId}',
    settings: '/api/v2/lwgrade2/{courseId}/gradebook/settings',
    download: '/api/v2/lwgrade2/{courseId}/gradebook/export?config',
    downloadStudent: '/api/v2/lwgrade2/{courseId}/gradebook/{studentId}/export',
    syncHistory: '/api/v2/lwgrade2/{courseId}/gradebook/grades/syncHistory',
    syncStudentHistory:
      '/api/v2/lwgrade2/{courseId}/gradebook/grades/syncHistory/{edgePath}/{studentId}',
    syncEdgeHistory: '/api/v2/lwgrade2/{courseId}/gradebook/grades/syncHistory/{edgePath}',
    syncAllEdgeHistory: '/api/v2/lwgrade2/{courseId}/gradebook/grades/syncHistory/{edgePath}/all',
    syncAllHistory: '/api/v2/lwgrade2/{courseId}/gradebook/grades/syncHistory/all',
  },
  //=============================
  progress: {
    progress: '/api/v2/progress/{courseId}/report/{userId}',
    progressReport: '/api/v2/progress/{courseId}/report',
    progressExport: '/api/v2/progress/{courseId}/report/export',
    courseProgress: '/api/v2/progress/{courseId}/overall/{userId}',
    overallProgressReportForUsers: '/api/v2/progress/{courseId}/overallReport?user',
  },
  //=============================
  courseContents: {
    contents: '/api/v2/lwc/{context}/contents?user',
  },
  instructorCustomization: {
    customize: '/api/v2/lwc/{contextId}/customise/{edgePath}',
    customizePoints: '/api/v2/contentConfig/pointsPossible/{edgePath};context={contextId}',
    assessmentPolicies: '/api/v2/contentConfig/{courseId}/assessmentPolicies',
    dueDateAccommodation: '/api/v2/contentConfig/dueDateAccommodation;context=${courseId}',
    dueDate: '/api/v2/contentConfig/dueDate;context=${courseId}',
  },
  overview: {
    gradingQueue: '/api/v2/gradingQueue/{contextId}',
    attemptOverviews:
      '/api/v2/assessment/attemptOverview;context={contextId};paths={paths};userId={userId}',
    instructorAttemptsOverview:
      '/api/v2/assessment/instructorAttemptsOverview;context={contextId};paths={paths}',
  },
  lwGating: {
    // TODO: unused?
    overrides: '/api/v2/lwc/{context}/gateOverrides',
  },
  contentItem: {
    fileBundle: {
      serve: '/api/v2/authoring/{commit}/fileBundle.1/{name}/serve',
      files: '/api/v2/authoring/{commit}/fileBundle.1/{name}/files',
      render: '/api/v2/contexts/{courseId}/contents/{itemId}/render', //Specific to html resources
    },
    scorm: {
      serve: '/api/v2/authoring/{commit}/scorm.1/{name}/serve',
    },
    asset: {
      lessonPrint: '/api/v2/authoring/{branch}/{commit}/asset/{name}/renderedPrintFriendly',
    },
    assetRender: '/api/v2/authoring/{branch}/{commit}/asset/{name}/rendered?cdn',
    courseAssetRender: '/api/v2/lwc/{context}/asset/{path}/render?cdn',
    courseAssetInfo: '/api/v2/lwc/{context}/asset/{path}/info',
    survey: '/api/v2/lwc/{sectionId}/contents/{edgePath}/survey',
  },
  //=============================
  messaging: {
    list: '/api/v2/messages',
    send: '/api/v2/messages/send',
  },
  fileUpload: {
    upload: '/api/v2/uploads',
  },
  dean: {
    emit: 'api/v2/an/emit',
  },
  //=============================
  proficiencies: {
    status:
      '/api/v2/assetCourses/{courseId}/proficiencies/{proficiencySystemId}/status?assetId,userId',
  },
  //=============================
  presence: {
    sessions: '/api/v2/presence/sessions',
    session: '/api/v2/presence/sessions/{presenceId}',
    sessionEvents: '/api/v2/presence/sessions/{presenceId}/events?X-UserId',
    sessionPoll: '/api/v2/presence/sessions/{presenceId}/poll',
    sessionDelete: '/api/v2/presence/sessions/{presenceId}/delete',
    openChat: '/api/v2/chats',
    getChat: '/api/v2/chats/{id}',
    chatMessages: '/api/v2/chats/{id}/messages',
    profiles: '/api/v2/profiles',
    profile: '/api/v2/profiles/{handle}',
    thumbnail: '/api/v2/profiles/{handle}/thumbnail/{thumbnailId};size=medium',
  },
  //=============================
  //FERPA compliant
  cohort: {
    users: '/api/v2/contexts/{contextId}/users',
  },
  //=============================
  competencyStatus: {
    byContentIdentifier: '/api/v2/competencyStatus/{contentIdentifier}?viewAs',
    competencies: '/api/v2/competencyStatus/competencies;context={contextId}',
    mastery: '/api/v2/courseMastery;context={contextId}?viewAs',
  },
  //=============================
  tutorial: {
    status: `/api/v2/tutorials/{name}/status`,
  },
  //=============================
  qna: {
    addQuestion: '/api/v2/lwc/{context}/qna',
    addMessage: '/api/v2/lwc/{context}/qna/{id}/message',
    editMessage: '/api/v2/lwc/{context}/qna/{id}/message/{mid}',
    instructorClose: '/api/v2/lwc/{context}/qna/{id}/instructorClose',
    recategorize: '/api/v2/lwc/{context}/qna/{id}/recategorize',
    getAttachment: '/api/v2/lwc/{context}/qna/{qid}/message/{mid}/{aid}',
    getQuestion: '/api/v2/lwc/{context}/qna/{id}',
    getQuestions: '/api/v2/lwc/{context}/qna',
    getQuestionIds: '/api/v2/lwc/{context}/qna/ids',
    getSummary: '/api/v2/lwc/{context}/qna/summary',
    closeQuestion: '/api/v2/lwc/{context}/qna/{id}/close',
    reopenQuestion: '/api/v2/lwc/{context}/qna/{id}/reopen',
    multicast: '/api/v2/lwc/{context}/qna/multicast',
    multicastReply: '/api/v2/lwc/{context}/qna/multicast/{id}/reply',
  },
} as const;
