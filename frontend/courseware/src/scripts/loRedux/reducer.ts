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

import discussionBoardStateReducer, {
  discussionsSlice,
  postsDataSlice,
  threadsDataReducer,
  threadsDataSlice,
} from '../discussion/reducer';
import { connectRouter, RouterState } from 'connected-react-router';
import { Course, UserInfo } from '../../loPlatform';
import { Grade, Progress } from '../api/contentsApi';
import { GradebookColumn } from '../api/gradebookApi';
import { QuizAssessment } from '../api/quizApi';
import { bookmarksReducer } from '../components/bookmarks/bookmarksReducer';
import { feedbackReducer } from '../feedback/feedbackReducer';
import gatingInformationReducer from '../instructorPages/gatingEditorPage/gatingInformationReducer';
import accessCodeReducer from '../loRedux/accessCodeReducer';
import { actualUserReducer } from '../loRedux/actualUserSlice';
import qnaReducer from '../qna/qnaReducer';
import submissionInEditReducer from '../contentPlayerComponents/activityViews/submission/redux/submissionInEditReducer';
import contentActivityReducer from '../courseActivityModule/reducers/contentActivityReducer';
import fullscreenReducer from '../courseContentModule/reducers/fullscreenReducer';
import toastsReducer, { toastSlice } from '../directives/toast/reducer';
import { enrollmentsData, enrollmentsState } from '../enrollments/reducer';
import listReducer from '../list/listReducer';
import quizPlayerReducer from '../quizPlayerModule/reducers/quizPlayerReducer';
import { activeValueReducer } from '../utilities/activeValueReduxUtils';
import apiDataReducer, { DLR } from '../utilities/apiDataReducer';
import { ContentRelations, ContentThinnedWithLearningIndex } from '../utilities/contentResponse';
import { history } from '../utilities/history';
import { LoadingState, loadingStateReducer } from '../utilities/loadingStateUtils';
import { createIdMapReducer, createNamedReducer } from '../utilities/reduxify';
import { getStatusFlagReducer, getStatusValueReducer } from '../utilities/statusFlagReducer';
import { combineReducers, Reducer } from 'redux';

import { surveyReducer } from '../components/survey/contentSurveyReducer';
import { eventsReducer } from '../events/eventsReducer';
import messageModalReducer from '../instructorPages/activityOverview/services/messageModalReducer';
import { dueDateExemptionsReducer } from '../instructorPages/controls/dueDateAccommodationsReducer';
import { customizationsReducer } from '../instructorPages/customization/customizationsReducer';
import gradebookTableOptionsReducer from '../instructorPages/gradebookGradesPage/reducers/gradebookTableOptionsReducer';
import { chatReducer } from '../landmarks/chat/chatReducer';
import { scormReducer } from '../scorm/reducer';
import { tutorialPlayerReducer } from '../tutorial/tutorialSlice';
import { columnsReducer } from './columnsReducer';
import { contentOverlayUpdateReducer } from './contentOverlayUpdateReducer';
import { contentsReducer } from './contentsReducer';
import { enrollmentCountReducer } from './enrollmentCountReducer';
import { overallGradeReducer } from './overallGradeByUser';
import { LocationState } from 'history';

const apiReducer = combineReducers({
  dueDateExemptions: dueDateExemptionsReducer,
  contents: contentsReducer,
  columns: columnsReducer,
  enrollmentCount: enrollmentCountReducer,
  contentOverlayUpdates: contentOverlayUpdateReducer,
  users: createNamedReducer('users', apiDataReducer as DLR<UserInfo>),
  contentItems: createNamedReducer(
    'contentItems',
    apiDataReducer as DLR<ContentThinnedWithLearningIndex>
  ),
  contentRelations: createNamedReducer('contentRelations', apiDataReducer as DLR<ContentRelations>),
  contentRelationsByCompetency: createNamedReducer('contentRelationsByCompetency', apiDataReducer),
  gatingInformationByContentByUser: gatingInformationReducer,
  activityByContentByUser: contentActivityReducer,
  customizableContents: createNamedReducer('users', apiDataReducer),
  assignments: createNamedReducer('assignments', apiDataReducer),
  competencies: createNamedReducer('competencies', apiDataReducer),
  competenciesMasteryStatus: createNamedReducer('competenciesMasteryStatus', apiDataReducer),
  competenciesMasteryStatusByUser: createNamedReducer(
    'competenciesMasteryStatusByUser',
    apiDataReducer
  ),
  competenciesLinkedContentItems: createNamedReducer(
    'competenciesLinkedContentItems',
    apiDataReducer
  ),
  competencyBreakdownByContent: createNamedReducer('competencyBreakdownByContent', apiDataReducer),
  competenciesByContent: createNamedReducer('competenciesByContent', apiDataReducer),
  displayFilesByContent: createNamedReducer('displayFilesByContent', apiDataReducer),
  assetInfoByContent: createNamedReducer('assetInfoByContent', apiDataReducer),
  legacyAssessments: createNamedReducer('legacyAssessments', apiDataReducer),
  legacyAssessmentQuestions: createNamedReducer('legacyAssessmentQuestions', apiDataReducer),
  legacyAssessmentAttemptsByUser: createNamedReducer(
    'legacyAssessmentAttemptsByUser',
    apiDataReducer
  ),
  quizzes: createNamedReducer('quizzes', apiDataReducer as DLR<QuizAssessment>),
  quizQuestions: createNamedReducer('quizQuestions', apiDataReducer),
  quizEditableQuestions: createNamedReducer('quizEditableQuestions', apiDataReducer),
  quizAttemptsByUser: createNamedReducer('quizAttemptsByUser', apiDataReducer),
  progressByContentByUser: createNamedReducer(
    'progressByContentByUser',
    apiDataReducer as DLR<Record<string, Progress>>
  ),
  dueDateExemptByContentByUser: createNamedReducer('dueDateExemptByContentByUser', apiDataReducer),
  overallProgressByUser: createNamedReducer(
    'overallProgressByUser',
    apiDataReducer as DLR<Progress>
  ),
  progressLastActivityTimeByUser: createNamedReducer(
    'progressLastActivityTimeByUser',
    apiDataReducer as DLR<Date>
  ),
  //TODO dedupe
  submissionDataByAssignmentByStudent: createNamedReducer(
    'submissionDataByAssignmentByStudent',
    apiDataReducer
  ),
  submissionDataByUserByContent: createNamedReducer(
    'submissionDataByUserByContent',
    apiDataReducer
  ),
  rubricScoresByUserByContent: createNamedReducer('rubricScoresByUserByContent', apiDataReducer),
  overallGradeByUser: overallGradeReducer,
  gradebookColumns: createNamedReducer('gradebookColumns', apiDataReducer as DLR<GradebookColumn>),
  //TODO dedupe
  gradebookGrades: createNamedReducer('gradebookGrades', apiDataReducer),
  gradeByContentByUser: createNamedReducer(
    'gradeByContentByUser',
    apiDataReducer as DLR<Record<string, Grade>>
  ),
  discussionSummaryByContentByUser: createNamedReducer(
    'discussionSummaryByContentByUser',
    apiDataReducer
  ),
  [enrollmentsData]: createNamedReducer(enrollmentsData, apiDataReducer),
  discussions: createNamedReducer('discussions', apiDataReducer),
  [postsDataSlice]: createNamedReducer(postsDataSlice, apiDataReducer),
  [threadsDataSlice]: threadsDataReducer,
  LTILaunchConfig: createNamedReducer('LTILaunchConfig', apiDataReducer),
  submissionAssessmentByContent: createNamedReducer(
    'submissionAssessmentByContent',
    apiDataReducer
  ),
  submissionAttemptsByContentByUser: createNamedReducer(
    'submissionAttemptsByContentByUser',
    apiDataReducer
  ),
  activityOverviewByUserByContent: createNamedReducer(
    'activityOverviewByUserByContent',
    apiDataReducer as DLR<UserInfo>
  ),
});

/**
 * UI Reducer
 */
const uiReducer = combineReducers({
  StudentPreviewUserPicker: createNamedReducer('StudentPreviewUserPicker', listReducer),
  learnerPreviewUserPickerModalState: createNamedReducer(
    'learnerPreviewUserPickerModalState',
    getStatusFlagReducer()
  ),

  [enrollmentsState]: createNamedReducer(enrollmentsState, listReducer),
  [discussionsSlice]: discussionBoardStateReducer,
  [toastSlice]: toastsReducer,

  //TODO dedupe, keep the second when we delete old code
  contentPlaylistNavOpen: createNamedReducer('contentPlaylistNavOpen', getStatusFlagReducer()),
  moduleNavigationPanelOpen: createNamedReducer(
    'moduleNavigationPanelOpen',
    getStatusFlagReducer()
  ),
  moduleNavigationLoadingStatus: createNamedReducer(
    'moduleNavigationLoadingStatus',
    createIdMapReducer(loadingStateReducer)
  ),

  moduleLessonManuallyExpanded: createNamedReducer(
    'moduleLessonManuallyExpanded',
    createIdMapReducer(getStatusFlagReducer())
  ),

  presentUsersPanelOpen: createNamedReducer('presentUsersPanelOpen', getStatusFlagReducer()),
  pageHeaderSearchOpen: createNamedReducer('pageHeaderSearchOpen', getStatusFlagReducer()),

  customizableContentsLoadingState: createNamedReducer(
    'customizableContentsLoadingState',
    loadingStateReducer
  ),

  contentPlayerLoadingState: createNamedReducer(
    'contentPlayerLoadingState',
    createIdMapReducer(loadingStateReducer)
  ),
  contentActivityLoadingState: createNamedReducer(
    'contentActivityLoadingState',
    createIdMapReducer(loadingStateReducer)
  ),

  quizActivityOpenAttemptState: createNamedReducer(
    'quizActivityOpenAttemptState',
    createIdMapReducer(loadingStateReducer)
  ),

  competenciesMasteryStatusState: createNamedReducer(
    'competenciesMasteryStatusState',
    createIdMapReducer(loadingStateReducer)
  ),

  linkedContentStateByCompetency: createNamedReducer(
    'linkedContentStateByCompetency',
    createIdMapReducer(loadingStateReducer)
  ),
  linkedContentGradeStateByCompetency: createNamedReducer(
    'linkedContentGradeStateByCompetency',
    createIdMapReducer(loadingStateReducer)
  ),
  linkedContentProgressStateByCompetency: createNamedReducer(
    'linkedContentProgressStateByCompetency',
    createIdMapReducer(loadingStateReducer)
  ),

  legacyAssessmentsState: createNamedReducer(
    'legacyAssessmentsState',
    createIdMapReducer(loadingStateReducer)
  ),
  legacyAssessmentQuestionsState: createNamedReducer(
    'legacyAssessmentQuestionsState',
    createIdMapReducer(loadingStateReducer)
  ),
  legacyAssessmentAttemptsByUserState: createNamedReducer(
    'legacyAssessmentAttemptsByUserState',
    createIdMapReducer(loadingStateReducer)
  ),

  quizzesState: createNamedReducer('quizzesState', createIdMapReducer(loadingStateReducer)),
  quizQuestionsState: createNamedReducer(
    'quizQuestionsState',
    createIdMapReducer(loadingStateReducer)
  ),
  editQuestionState: createNamedReducer(
    'editQuestionState',
    createIdMapReducer(loadingStateReducer)
  ),
  quizAttemptsByUserState: createNamedReducer(
    'quizAttemptsByUserState',
    createIdMapReducer(loadingStateReducer)
  ),

  quizAttemptDetailsState: createNamedReducer(
    'quizAttemptDetailsState',
    createIdMapReducer(loadingStateReducer)
  ),
  quizAttemptSubmissionState: createNamedReducer(
    'quizAttemptSubmissionState',
    createIdMapReducer(loadingStateReducer)
  ),
  quizAttemptSaveState: createNamedReducer(
    'quizAttemptSaveState',
    createIdMapReducer(loadingStateReducer)
  ),
  quizAttemptAutoSaveState: createNamedReducer(
    'quizAttemptAutoSaveState',
    createIdMapReducer(loadingStateReducer)
  ),
  quizQuestionSubmissionState: createNamedReducer(
    'quizQuestionSubmissionState',
    createIdMapReducer(loadingStateReducer)
  ),
  quizPlayerState: createNamedReducer('quizPlayerState', createIdMapReducer(quizPlayerReducer)),
  LTIConfigLoadingState: createNamedReducer(
    'LTIConfigLoadingState',
    createIdMapReducer(loadingStateReducer)
  ),

  submissionInEditByContentByUser: createIdMapReducer(
    createIdMapReducer(submissionInEditReducer, 'contentId'),
    'userId'
  ),

  submissionOpenAttemptLoadingStateByContent: createNamedReducer(
    'submissionOpenAttemptLoadingStateByContent',
    createIdMapReducer(loadingStateReducer)
  ),
  submissionSaveState: createNamedReducer(
    'submissionSaveState',
    createIdMapReducer(loadingStateReducer)
  ),
  submissionSubmitState: createNamedReducer(
    'submissionSubmitState',
    createIdMapReducer(loadingStateReducer)
  ),

  activityOverviewLoadingStateByContent: createNamedReducer(
    'activityOverviewLoadingStateByContent',
    createIdMapReducer(loadingStateReducer, 'contentId')
  ),
  activityOverviewMessageModalState: messageModalReducer,
  activityOverviewSendMessageLoadingState: createNamedReducer(
    'activityOverviewSendMessageLoadingState',
    loadingStateReducer
  ),
  activityOverviewWithWorkListStateByContent: createNamedReducer(
    'activityOverviewWithWorkListStateByContent',
    createIdMapReducer(listReducer, 'contentId')
  ),
  activityOverviewWithoutWorkListStateByContent: createNamedReducer(
    'activityOverviewWithoutWorkListStateByContent',
    createIdMapReducer(listReducer, 'contentId')
  ),

  progressReport: createNamedReducer(
    'progressReport',
    combineReducers({
      listState: listReducer,
      compactHeaders: getStatusFlagReducer(),
    })
  ),
  previewAsUserLoadingState: createNamedReducer('previewAsUserLoadingState', loadingStateReducer),

  learnerTableModal: createNamedReducer<{
    modalState: { status: boolean };
    activeLearner: { value: null | UserInfo };
    messageState: LoadingState;
  }>(
    'learnerTableModal',
    combineReducers({
      modalState: getStatusFlagReducer(undefined),
      activeLearner: activeValueReducer,
      messageState: loadingStateReducer,
    })
  ),

  studentCourseCompetenciesPage: createNamedReducer(
    'studentCourseCompetenciesPage',
    combineReducers({
      listState: listReducer,
    })
  ),

  manageDiscussionsModal: createNamedReducer(
    'manageDiscussionsModal',
    combineReducers({
      modalState: getStatusFlagReducer(),
      closedSavingState: loadingStateReducer,
    })
  ),

  learnerAssignmentsPage: createNamedReducer(
    'learnerAssignmentsPage',
    combineReducers({
      loadingState: createIdMapReducer(loadingStateReducer, 'userId'),
      listState: createIdMapReducer(listReducer, 'userId'),
      forCreditOnly: getStatusFlagReducer(false),
    })
  ),

  gradebookGradesTableListState: createNamedReducer('gradebookGradesTableListState', listReducer),
  gradebookTableOptions: gradebookTableOptionsReducer,

  gradebookColumnsLoadingState: createNamedReducer(
    'gradebookColumnsLoadingState',
    loadingStateReducer
  ),

  sideNavOpenState: createNamedReducer('sideNavOpenState', getStatusFlagReducer(true)),

  feedbackOpenState: createNamedReducer('feedbackOpenState', getStatusFlagReducer(false)),

  feedbackAddState: createNamedReducer('feedbackAddState', getStatusFlagReducer(false)),

  feedbackEnabledState: createNamedReducer('feedbackEnabledState', getStatusFlagReducer(false)),

  graderOpenState: createNamedReducer('graderOpenState', getStatusFlagReducer(false)),

  // Instructions to show in the header instructions dropdown
  instructionsState: createNamedReducer('instructionsState', getStatusValueReducer(undefined)),

  // Is the header instructions dropdown open
  instructingState: createNamedReducer('instructingState', getStatusFlagReducer(false)),

  discussionListLoadingState: createNamedReducer('discussionListLoadingState', loadingStateReducer),

  chat: chatReducer,

  feedback: feedbackReducer,

  fullscreenState: createNamedReducer('fullscreenState', fullscreenReducer),

  tutorialPlayer: tutorialPlayerReducer,

  qna: qnaReducer,

  accessCode: accessCodeReducer,
});

const courseReducer = (state: Course = window.lo_platform.course) => state;
const actualUserRightsReducer = (state: string[] = []) => state;

const reducer = combineReducers({
  course: courseReducer,
  settings: (a = {}) => a,
  preferences: (a: any = {}): any => a,
  actualUser: actualUserReducer,
  actualUserRights: actualUserRightsReducer,
  api: apiReducer,
  ui: uiReducer,
  courseCustomizations: customizationsReducer,
  router: connectRouter(history) as Reducer<RouterState<LocationState>>,
  events: eventsReducer,
  scorm: scormReducer,
  survey: surveyReducer,
  bookmarks: bookmarksReducer,
});

export default reducer;
