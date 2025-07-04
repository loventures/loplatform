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

import { escapeRegExp } from 'lodash';

import QUESTION_TYPES from '../asset/constants/questionTypes.constants';
import SURVEY_QUESTION_TYPES from '../asset/constants/surveyQuestionTypes.constants';
import { ChoiceContent, ChoiceQuestionContent, HtmlPart, TypeId } from '../types/asset';
import {
  EssayQuestion,
  FillInTheBlankQuestion,
  MatchingQuestion,
  MultipleChoiceQuestion,
  MultipleSelectQuestion,
  TrueFalseQuestion,
} from '../types/typeIds';
import { computeNewGuid } from '../services/generators/surveyChoiceQuestion-generator';

export const ComplexQuestionText = 'AssessQuestion.complexQuestionText';
export const RichCorrectAnswerFeedback = 'AssessQuestion.richCorrectAnswerFeedback';
export const RichIncorrectAnswerFeedback = 'AssessQuestion.richIncorrectAnswerFeedback';
export const Choices = 'Assess_Question_Choice';
export const PointsPossible = 'AssessQuestion.pointsPossible';
export const DistractorRandomization =
  'AssessmentQuestionConfigurationType.allowDistractorRandomization';
export const ContentBlockQuestionText = 'AssessQuestion.contentBlockQuestionText';

export const ChoiceCorrect = 'AssessQuestionChoice.correct';
export const ChoiceComplexText = 'AssessQuestionChoice.complexText';
export const ChoiceLegacyText = 'AssessQuestionChoice.text';
export const ChoiceCorrectFeedback = 'AssessQuestionChoice.correctFeedback';
export const ChoiceIncorrectFeedback = 'AssessQuestionChoice.incorrectFeedback';
export const ChoicePointValue = 'AssessQuestionChoice.pointValue';
export const Terms = 'Assess_Question_Term';
export const TermText = 'AssessQuestionTerm.text';
export const Definitions = 'Assess_Question_Definition';
export const DefinitionText = 'AssessQuestionDefinition.text';

export type ChoicePartKey =
  | typeof ChoiceComplexText
  | typeof ChoiceCorrectFeedback
  | typeof ChoiceIncorrectFeedback;

export type ChoiceQuestionTypes =
  | MultipleChoiceQuestion
  | MultipleSelectQuestion
  | TrueFalseQuestion;

export type StoryQuestionTypes =
  | ChoiceQuestionTypes
  | EssayQuestion
  | FillInTheBlankQuestion
  | MatchingQuestion;

export const parseStringDefaultOne = (s: string): number => {
  const number = parseFloat(s);
  return isNaN(number) ? 1 : number;
};

export const newHtml = (html: string): HtmlPart => ({
  html,
  renderedHtml: '',
  partType: 'html',
});

export const emptyHtml = (): HtmlPart => newHtml('<p><br></p>');

export const newChoice = ({
  part = emptyHtml(),
  correct = false,
  points = 0,
  index = 0,
}: {
  part?: HtmlPart;
  correct?: boolean;
  points?: number;
  index?: number;
}): ChoiceContent => ({
  [ChoiceComplexText]: part,
  [ChoiceCorrect]: correct,
  [ChoiceCorrectFeedback]: emptyHtml(),
  [ChoiceIncorrectFeedback]: emptyHtml(),
  [ChoicePointValue]: points,
  [ChoiceLegacyText]: '',
  index,
});

// Not Implementing: bindrop matching ordering
// TODO: restrict crazy grading policies to checkpoints?

export const InitialHtml = `<p><br></p>`;

// See AssetGenerators but they have titling issues
export const newAssetData = (typeId: TypeId): any => {
  switch (typeId) {
    case 'multipleChoiceQuestion.1':
    case 'multipleSelectQuestion.1':
      return {
        questionContent: {
          [PointsPossible]: '1',
          [ComplexQuestionText]: emptyHtml(),
          [RichCorrectAnswerFeedback]: emptyHtml(),
          [RichIncorrectAnswerFeedback]: emptyHtml(),
          [DistractorRandomization]: true,
          [Choices]: [
            newChoice({ correct: true, points: 1, index: 0 }),
            newChoice({ correct: false, points: 0, index: 1 }),
            newChoice({ correct: false, points: 0, index: 2 }),
            newChoice({ correct: false, points: 0, index: 3 }),
          ],
        },
        ...(typeId === 'multipleSelectQuestion.1'
          ? {
              allowPartialCredit: false,
              scoringOption: 'allOrNothing',
            }
          : {}),
        title: 'Unsaved',
      };
    case 'trueFalseQuestion.1':
      return {
        questionContent: {
          [PointsPossible]: '1',
          [ComplexQuestionText]: emptyHtml(),
          [RichCorrectAnswerFeedback]: emptyHtml(),
          [RichIncorrectAnswerFeedback]: emptyHtml(),
          [DistractorRandomization]: false,
          [Choices]: [
            newChoice({ correct: true, points: 1, index: 0, part: newHtml('true') }),
            newChoice({ correct: false, points: 0, index: 1, part: newHtml('false') }),
          ],
        },
        title: 'Unsaved',
      };
    case 'essayQuestion.1':
      return {
        questionContent: {
          [PointsPossible]: '1',
        },
        title: 'Unsaved',
      };
    case 'fillInTheBlankQuestion.1':
      return {
        questionContent: {
          [PointsPossible]: '1',
          [ComplexQuestionText]: emptyHtml(),
          [RichCorrectAnswerFeedback]: emptyHtml(),
          [RichIncorrectAnswerFeedback]: emptyHtml(),
        },
        title: 'Unsaved',
      };
    case 'matchingQuestion.1':
      return {
        questionContent: {
          [PointsPossible]: '1',
          [ComplexQuestionText]: emptyHtml(),
          [RichCorrectAnswerFeedback]: emptyHtml(),
          [RichIncorrectAnswerFeedback]: emptyHtml(),
          [Terms]: [
            { [TermText]: '', correctDefinitionIndex: 0 },
            { [TermText]: '', correctDefinitionIndex: 1 },
          ],
          [Definitions]: [
            { [DefinitionText]: '', index: 0 },
            { [DefinitionText]: '', index: 1 },
          ],
        },
        allowPartialCredit: false,
        title: 'Unsaved',
      };
    case 'assessment.1':
      return {
        pointsPossible: 100,
        isForCredit: false,
        assessmentType: 'formative',
        scoringOption: 'mostRecentAttemptScore',
        randomizeQuestionOrder: false,
        displayConfidenceIndicators: true,
        maxAttempts: null,
        singlePage: true,
      };
    case 'assignment.1':
      return {
        pointsPossible: 100,
        isForCredit: false,
        assessmentType: 'formative',
        scoringOption: 'mostRecentAttemptScore',
        maxAttempts: null,
      };
    case 'checkpoint.1':
      return {};
    case 'diagnostic.1':
      return {
        pointsPossible: 100,
        isForCredit: false,
        assessmentType: 'formative',
        scoringOption: 'firstAttemptScore',
        maxAttempts: 1,
        displayConfidenceIndicators: true,
        singlePage: true,
      };
    case 'discussion.1':
      return {};
    case 'html.1':
      return {
        source: null,
      };
    case 'observationAssessment.1':
      return {
        pointsPossible: 100,
        isForCredit: false,
        assessmentType: 'formative',
        scoringOption: 'mostRecentAttemptScore',
        maxAttempts: null,
      };
    case 'lti.1': {
      return {
        pointsPossible: 100,
        isForCredit: false,
        lti: {
          toolId: '',
          name: '',
          toolConfiguration: {
            // url,key,secret,...
            customParameters: {},
          },
        },
      };
    }
    case 'courseLink.1': {
      return {
        gradable: false,
        pointsPossible: 100,
        isForCredit: false,
        sectionPolicy: 'MostRecent',
      };
    }
    case 'poolAssessment.1':
      return {
        pointsPossible: 100,
        isForCredit: false,
        assessmentType: 'formative',
        scoringOption: 'mostRecentAttemptScore',
        maxAttempts: null,
        numberOfQuestionsForAssessment: 0,
        useAllQuestions: true,
        displayConfidenceIndicators: true,
        singlePage: true,
      };
    case 'level1Competency.1':
    case 'level2Competency.1':
    case 'level3Competency.1':
    case 'rubric.1':
      return {
        title: 'Untitled',
      };
    case 'rubricCriterion.1':
      return {
        title: 'Untitled',
        description: '',
        levels: [
          { points: 10, name: '', description: '' },
          { points: 0, name: '', description: '' },
        ],
      };
    case 'survey.1':
      return {
        title: 'Untitled',
        inline: true,
        disabled: false,
        programmatic: false,
      };
    case 'likertScaleQuestion.1':
      return {
        title: 'Untitled',
      };
    case 'ratingScaleQuestion.1':
      return {
        title: 'Untitled',
        max: 10,
        lowRatingText: 'Not at all likely',
        highRatingText: 'Very likely',
      };
    case 'surveyEssayQuestion.1':
      return {
        prompt: emptyHtml(),
      };
    case 'surveyChoiceQuestion.1':
      return {
        prompt: emptyHtml(),
        choices: Array(4)
          .fill(null)
          .map(() => ({ value: computeNewGuid(), label: emptyHtml() })),
      };
    case 'scorm.1':
      return {
        pointsPossible: 100,
        isForCredit: false,
        contentWidth: 1024,
        contentHeight: 768,
        launchNewWindow: false,
        zipPaths: [],
        scormTitle: null,
        resourcePath: '',
        allRefs: {},
        passingScore: null,
        objectiveIds: [],
        sharedDataIds: [],
        source: null,
      };
    default:
      return {};
  }
};

// reindexes choices and distributes points
export const cleanChoices = (typeId: TypeId, content: ChoiceQuestionContent) => {
  const multipleSelect = typeId === MultipleSelectQuestion;
  const choices = content[Choices];
  const correctCount = choices.filter(c => c[ChoiceCorrect]).length ?? 0;
  const incorrectCount = choices.length - correctCount;
  const pointsPossible = parseStringDefaultOne(content[PointsPossible]);
  const pointsForCorrect = pointsPossible / correctCount;
  const pointsForIncorrect = multipleSelect ? -(pointsPossible / incorrectCount) : 0;
  return choices.map((choice, index) => ({
    ...choice,
    [ChoicePointValue]: choice[ChoiceCorrect] ? pointsForCorrect : pointsForIncorrect,
    [choice[ChoiceCorrect] ? ChoiceIncorrectFeedback : ChoiceCorrectFeedback]: emptyHtml(),
    index,
  }));
};

const AllQuestionTypes = new Set([...QUESTION_TYPES, ...SURVEY_QUESTION_TYPES]);

export const isQuestion = (typeId: TypeId | undefined): boolean => AllQuestionTypes.has(typeId);

export const isDistractable = (typeId: TypeId | undefined): boolean =>
  typeId === 'multipleChoiceQuestion.1' ||
  typeId === 'multipleSelectQuestion.1' ||
  typeId === 'trueFalseQuestion.1';

// while true false is randomizable, it's not a done thing. matching has randomizable
// as a config but it is ignored
export const isRandomizable = (typeId: TypeId | undefined): boolean =>
  typeId === 'multipleChoiceQuestion.1' || typeId === 'multipleSelectQuestion.1';

/* If the search is all blank, a case-sensitive match-all regex is returned allowing
 * the caller to test the ignoreCase flag for whether a search was actually specified. */
export const toMultiWordRegex = (search: string) =>
  !search.trim()
    ? new RegExp('')
    : search[0] === '"' && search[search.length - 1] === '"'
      ? new RegExp(escapeRegExp(search.slice(1, -1)), 'i')
      : new RegExp(
          search
            .split(/\W+/)
            .filter(s => !!s)
            .map(s => '\\b' + escapeRegExp(s))
            .join('.*'),
          'i'
        );
