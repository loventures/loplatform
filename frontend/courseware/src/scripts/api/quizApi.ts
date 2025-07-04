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
import Course from '../bootstrap/course';
import { Dayjs } from 'dayjs';
import { map, mapValues } from 'lodash';
import { useMemo } from 'react';

import { AssetTypeId, HtmlPart } from '../utilities/assetTypes';
import { ContentId } from './contentsApi';
import { StagedFile } from './fileUploadApi';

export type RubricLevel = {
  name: string;
  description: string;
  points: number;
};

export type RubricSection = {
  name: ContentId;
  title: string;
  description: string;
  competencies: any[];
  levels: RubricLevel[];
};

export type Rubric = {
  title: string;
  nodeName: ContentId;
  sections: RubricSection[];
};

export type Score = {
  pointsAwarded: number;
  pointsPossible: number;
};

export type RubricScore = {
  scoreType: 'rubric';
  pointsPossible: number;
  nullableCriterionScores: {
    [k: string]: Score;
  };
};

export type BasicScore = {
  scoreType: 'basic';
  pointsAwarded: number;
  pointsPossible: number;
};

export type BasicFeedback = {
  feedbackType: 'basic';
  comment: string;
  attachments: number[];
  uploads?: StagedFile[];
};

export function isBasicFeedback(feedback: InstructorFeedback): feedback is BasicFeedback {
  return feedback.feedbackType === 'basic';
}

export type RubricFeedback = {
  feedbackType: 'rubric';
  comment: string;
  sectionName: string;
};

export type InstructorFeedback = BasicFeedback | RubricFeedback;

export type RawAttachmentInfo = {
  id: number;
  fileName: string;
  mimeType: string;
  size: number;
  createDate: string;
};

export type AttachmentInfo = RawAttachmentInfo & {
  downloadUrl: string;
  thumbnailUrl: string;
};

export type AttemptOpen = 'Open';
export type AttemptSubmitted = 'Submitted';
export type AttemptFinalized = 'Finalized';
export type AttemptState = AttemptOpen | AttemptSubmitted | AttemptFinalized;

export type ResponseNotSubmitted = 'NotSubmitted';
export type ResponseSubmitted = 'ResponseSubmitted';
export type ResponseScored = 'ResponseScored';
export type ResponseScoreReleased = 'ResponseScoreReleased';

export type ResponseState =
  | ResponseNotSubmitted
  | ResponseSubmitted
  | ResponseScored
  | ResponseScoreReleased;

export type Attempt = {
  id: number;
  state: AttemptState;
  valid: boolean;
  createTime: string | null;
  submitTime: string | null;
  scoreTime: string | null;
  score: BasicScore | RubricScore | null;
  scorer: unknown;
  responseTime: string | null;
  remainingMillis: number | null;
  autoSubmitted: boolean;
};

export interface QuizAttempt extends Attempt {
  questions: Question[];
  responses: QuestionResponse[];
  attachments: { [k: string]: AttachmentInfo };
  deadline?: Dayjs;
}

export type QuestionResponse<TQuestion extends Question = Question> = {
  state: ResponseState;
  score: (TQuestion extends EssayQuestion ? RubricScore : BasicScore) | null;
  attachments: number[];
  instructorFeedback: InstructorFeedback[];
  selection: TQuestion extends MultipleChoiceQuestion
    ? ChoiceSelection | null
    : TQuestion extends TrueFalseQuestion
      ? ChoiceSelection | null
      : TQuestion extends MultipleSelectQuestion
        ? ChoiceSelection | null
        : TQuestion extends SequencingQuestion
          ? OrderingSelection | null
          : TQuestion extends BinDropQuestion
            ? GroupingSelection | null
            : TQuestion extends MatchingQuestion
              ? GroupingSelection | null
              : TQuestion extends FillInTheBlankQuestion
                ? BlankEntriesSelection | null
                : TQuestion extends EssayQuestion
                  ? FreeTextSelection | null
                  : never;
};
export type QuizSettings = {
  maxAttempts: number | null;
  maxMinutes: number | null;
  softAttemptLimit: number | null;
  softAttemptLimitMessage: string | null;
  navigationPolicy: {
    policyType: 'paged' | 'singlePage';
    backtrackingAllowed: boolean;
    skippingAllowed: boolean;
  };
  resultsPolicy: {
    resultReleaseTime: 'OnAttemptScore' | 'OnResponseScore';
    remediationReleaseCondition: 'AnyResponse' | 'OnCorrectResponse';
  };
  gradingPolicy: 'MostRecent' | 'Average' | 'First' | 'Highest';
  displayConfidenceIndicators: boolean;
  assessmentType: 'summative' | 'formative';
  hasCompetencies: boolean;
  isDiagnostic: boolean;
  isCheckpoint: boolean;
  testsOut: Record<string, number>;
};

export type QuizInstructions = {
  instructionType: string;
  block: {
    parts: [HtmlPart];
    renderedHtml: string;
    partType: string;
  };
};

export type QuizAssessment = {
  settings: QuizSettings;
  assetReference: {
    nodeName: string;
    commit: number;
  };
  contentId: string;
  title: string;
  instructions: QuizInstructions;
  questions: any;
  pastDeadline: boolean;
};

const contextId = Course.id;

const processQuizAttempt = (attempt: QuizAttempt): QuizAttempt => {
  return {
    ...attempt,
    attachments: mapValues(attempt.attachments, attachment => {
      // loConfig.quiz.attachment
      const viewUrl = `/api/v2/quizAttempt/${attempt.id}/attachments/${attachment.id};context=${contextId}`;
      return {
        ...attachment,
        downloadUrl: viewUrl + '?download=true',
        thumbnailUrl: viewUrl + '?download=false&size=medium',
      };
    }),
  };
};

export type QuizApi = {
  loadQuestions: (contentId: string) => Promise<Question[]>;
  loadAttemptsForUser: (contentId: string, userId: number) => Promise<QuizAttempt[]>;
  invalidateAttempt: (attemptId: number) => Promise<QuizAttempt>;
  saveQuestionScoreAndFeedback: (
    attemptId: number,
    questionIndex: number,
    isReleasing: boolean,
    score: RubricScore | BasicScore,
    feedback: InstructorFeedback[]
  ) => Promise<QuizAttempt>;
};

export const useQuizApi = (): QuizApi => {
  const gretchen = axios; //useContext(gretchen);
  return useMemo(
    () => ({
      loadQuestions: (contentId: string): Promise<Question[]> => {
        // loConfig.quiz.getQuestions
        return gretchen
          .get(`/api/v2/lwQuiz/${contextId}.${contentId}/questions;context=${contextId}`)
          .then(response => response.data.questions as Question[]);
      },
      loadAttemptsForUser: (contentId: string, userId: number): Promise<QuizAttempt[]> => {
        // loConfig.quiz.attempts
        return gretchen
          .get(
            `/api/v2/quizAttempt;quizId=${contextId}.${contentId};context=${contextId};userId=${userId}`
          )
          .then(response => {
            return map(response.data.objects, processQuizAttempt);
          });
      },
      invalidateAttempt: (attemptId: number): Promise<QuizAttempt> => {
        // loConfig.quiz.invalidate
        return gretchen
          .post(`/api/v2/quizAttempt/${attemptId}/invalidate;context=${contextId}`)
          .then(response => processQuizAttempt(response.data));
      },
      saveQuestionScoreAndFeedback: (
        attemptId: number,
        questionIndex: number,
        isReleasing: boolean,
        score: RubricScore | BasicScore,
        feedback: InstructorFeedback[]
      ): Promise<QuizAttempt> => {
        const scoresPayload = {
          questionIndex,
          submit: isReleasing,
          score,
        };
        const feedbackPayload = {
          questionIndex,
          submit: isReleasing,
          values: feedback,
        };
        // loConfig.quiz.score
        return gretchen
          .post(`/api/v2/quizAttempt/${attemptId}/score;context=${contextId}`, scoresPayload)
          .then(() => {
            // loConfig.quiz.feedback
            return gretchen.post(
              `/api/v2/quizAttempt/${attemptId}/feedback;context=${contextId}`,
              feedbackPayload
            );
          })
          .then(response => processQuizAttempt(response.data));
      },
    }),
    [gretchen]
  );
};

export type ResponseSelectionBase = {
  skip: boolean;
  confidence?: number;
  timeElapsed?: number;
};

export type GroupingSelection = ResponseSelectionBase & {
  responseType: 'loi.cp.quiz.attempt.selection.GroupingSelection';
  elementIndexesByGroupIndex: Record<string, number[]>;
};
export type ChoiceSelection = ResponseSelectionBase & {
  responseType: 'loi.cp.quiz.attempt.selection.ChoiceSelection';
  selectedIndexes: number[];
};
export type LegacySelection = ResponseSelectionBase & {
  responseType: 'loi.cp.quiz.attempt.selection.LegacySelection';
  response: string;
};
export type BlankEntriesSelection = ResponseSelectionBase & {
  responseType: 'loi.cp.quiz.attempt.selection.BlankEntriesSelection';
  entries: string[];
};
export type OrderingSelection = ResponseSelectionBase & {
  responseType: 'loi.cp.quiz.attempt.selection.OrderingSelection';
  order: number[];
};
export type FreeTextSelection = ResponseSelectionBase & {
  responseType: 'loi.cp.quiz.attempt.selection.OrderingSelection';
  response?: string;
};

export type ResponseSelection =
  | GroupingSelection
  | ChoiceSelection
  | LegacySelection
  | BlankEntriesSelection
  | OrderingSelection
  | FreeTextSelection;

export type VersionedAssetReference = {
  nodeName: string;
  commit: number;
};

export type Rationale = CorrectRationale | TextRemediation | AssetRemediation;
export type CorrectRationale = {
  _type: 'rationale';
  reason: string;
};
export type TextRemediation = {
  _type: 'textRemediation';
  reason: string;
};
export type AssetRemediation = {
  _type: 'assetRemediation';
  reference: VersionedAssetReference;
  assetType: AssetTypeId;
  title: string;
};

export type QuestionCompetency = {
  id: number;
  name: string;
  title: string;
};

export type QuestionScoringOption =
  | 'allOrNothing'
  | 'allowPartialCredit'
  | 'fullCreditForAnyCorrectChoice';

export type QuestionBase<QSO extends QuestionScoringOption = 'allOrNothing'> = {
  includesCorrect: boolean;
  questionText: string;
  scoringOption: QSO;
  pointsPossible: number;
  rationales: Rationale[];
  competencies: QuestionCompetency[];
  reference: VersionedAssetReference;
};

export type Choice = {
  choiceText: string;
  correct?: boolean;
  points?: number;
  rationales: Rationale[];
};
export type Blank = { offset: number; answers: string[] };
export type FillInTheBlankQuestion = QuestionBase & {
  _type: 'fillInTheBlank';
  blanks: Blank[];
};
export type EssayQuestion = QuestionBase<'allowPartialCredit'> & {
  _type: 'essay';
  rubric: Rubric | null;
};
export type TrueFalseQuestion = QuestionBase & {
  _type: 'trueFalse';
  choices: Choice[];
};
export type MultipleSelectQuestion = QuestionBase<QuestionScoringOption> & {
  _type: 'multipleSelect';
  choices: Choice[];
};
export type Bin = { text: string; correctOptionIndices: number[] };
export type BinOption = { text: string };
export type BinDropQuestion = QuestionBase<QuestionScoringOption> & {
  _type: 'binDrop';
  bins: Bin[];
  binOptions: BinOption[];
};
export type ShortAnswerQuestion = QuestionBase & {
  _type: 'shortAnswer';
};
export type MultipleChoiceQuestion = QuestionBase & {
  _type: 'multipleChoice';
  choices: Choice[];
};
export type MatchingQuestion = QuestionBase<QuestionScoringOption> & {
  _type: 'matching';
  terms: { text: string }[];
  definitions: { text: string }[];
  correctDefinitionForTerm: Record<string, number>;
};
export type SequencingQuestion = QuestionBase & {
  _type: 'sequencing';
  choices: { text: HtmlPart }[];
};

export type Question =
  | FillInTheBlankQuestion
  | EssayQuestion
  | TrueFalseQuestion
  | MultipleSelectQuestion
  | BinDropQuestion
  | ShortAnswerQuestion
  | MultipleChoiceQuestion
  | MatchingQuestion
  | SequencingQuestion;

export const testOutDiagnostic = (attemptId: number): Promise<ApiQueryResults<string>> => {
  return axios
    .post<ApiQueryResults<string>>(`/api/v2/quizAttempt/${attemptId}/testOut;context=${contextId}`)
    .then(response => response.data);
};
