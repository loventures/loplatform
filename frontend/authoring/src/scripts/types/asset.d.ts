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

import { LtiTool } from './lti';
import Option from './option';
import * as T from './typeIds';
import User from './user';

/** These aliases just help distinguish between generic strings and uuids */
export type NodeName = string;
export type EdgeName = string;

export type TypeId =
  | T.Root
  | T.Course
  | T.Unit
  | T.Module
  | T.Lesson
  | T.Resource1
  | T.Lti
  | T.Assessment
  | T.Checkpoint
  | T.Discussion
  | T.Assignment
  | T.ObservationAssessment
  | T.PoolAssessment
  | T.Diagnostic
  | T.Html
  | T.Scorm
  | T.FileBundle
  | 'binDropQuestion.1'
  | T.EssayQuestion
  | 'fillInTheBlankQuestion.1'
  | T.TrueFalseQuestion
  | T.MatchingQuestion
  | T.MultipleSelectQuestion
  | T.MultipleChoiceQuestion
  | 'orderingQuestion.1'
  | 'likertScaleQuestion.1'
  | 'ratingScaleQuestion.1'
  | T.SurveyEssayQuestion
  | T.SurveyChoiceQuestion
  | T.Rubric
  | T.File
  | T.WebDependency
  | 'js.1'
  | 'css.1'
  | T.Image
  | T.Audio
  | T.Video
  | 'videoCaption.1'
  | T.Pdf
  | T.CompetencySet
  | T.Level1Competency
  | T.Level2Competency
  | T.Level3Competency
  | T.RubricCriterion
  | T.Survey
  | T.GradebookCategory
  | T.CourseLink
  | '';

// type Guarded<T extends TypeIds> = T extends keyof AssetDataMap ? Asset<T> : AssetUnion;
// // This sorta works when the overload with types exists.
// function typeIt<T extends AssetNodeTypeId>(asset: Asset<T>): AssetNode;
// function typeIt(asset: AssetUnion) {
//   switch(asset.typeId) {
//     case 'survey.1':
//       return asset as Asset<'survey.1'>;
//     case 'ratingScaleQuestion.1':
//       return asset as Asset<TypeIds.RatingQuestion>
//     case 'likertScaleQuestion.1':
//       return asset as Asset<TypeIds.LikertQuestion>;
//     default:
//       return asset;
//   }
// }

export type UnionToIntersection<U> = (U extends any ? (k: U) => void : never) extends (
  k: infer I
) => void
  ? I
  : never;

export type AssetDataMap = {
  [T.Survey]: SurveyData;
  [T.LikertQuestion]: LikertScaleQuestionData;
  [T.RatingQuestion]: RatingScaleQuestionData;
  [T.SurveyEssayQuestion]: SurveyEssayQuestionData;
  [T.SurveyChoiceQuestion]: SurveyChoiceQuestionData;
  [T.Course]: CourseData;
  [T.Unit]: UnitData;
  [T.Module]: ModuleData;
  [T.Lesson]: LessonData;
  [T.Assessment]: AssessmentData;
  [T.Checkpoint]: AssessmentData;
  [T.PoolAssessment]: PoolAssessmentData;
  [T.Diagnostic]: DiagnosticData;
  [T.Assignment]: AssignmentData;
  [T.ObservationAssessment]: ObservationAssessmentData;
  'videoCaption.1': VideoCaptionData;
  [T.MultipleSelectQuestion]: MultipleSelectQuestionData;
  [T.MultipleChoiceQuestion]: MultipleChoiceQuestionData;
  [T.TrueFalseQuestion]: TrueFalseQuestionData;
  [T.EssayQuestion]: EssayQuestionData;
  [T.FillInTheBlankQuestion]: FillInTheBlankQuestionData;
  [T.MatchingQuestion]: MatchingQuestionData;
  [T.File]: FileData;
  [T.Pdf]: FileData;
  [T.Audio]: FileData;
  [T.Video]: FileData;
  [T.Image]: ImageData;
  [T.Scorm]: ScormData;
  [T.Html]: HtmlData;
  [T.FileBundle]: FileBundleData;
  [T.WebDependency]: WebDependencyData;
  [T.CompetencySet]: CompetencySetData;
  [T.Level1Competency]: CompetencyData;
  [T.Level2Competency]: CompetencyData;
  [T.Level3Competency]: CompetencyData;
  [T.Discussion]: DiscussionData;
  [T.Lti]: LtiData;
  [T.GradebookCategory]: GradebookCategoryData;
  [T.Rubric]: RubricData;
  [T.RubricCriterion]: RubricCriterionData;
  [T.Resource1]: Resource1Data;

  [T.CourseLink]: CourseLinkData;
  [T.Root]: RootData;
};

// export type NodeDataUnion = AssetDataMap[keyof AssetDataMap]
export type AssetNodeData = Partial<UnionToIntersection<AssetDataMap[keyof AssetDataMap]>>;
export type AssetNodeTypeId = keyof AssetDataMap;

/**
 * There are two types of Asset types: AssetNode and Asset<T>
 *
 * AssetNode is a union of all asset types. It extends Asset and can be used whenever the specific asset type does not
 *   matter. If you need to access `data` you should use `Asset<T>`.
 *
 * Asset<T> is a parameterized interface for the Asset type. Type `T` must be a typeId string. I am experimenting with
 *   ways to enumerate the string and avoid usages of `Asset<'someAsset.1'>`.
 *
 * I'm still working on a pattern for narrowing types from AssetNode to Asset<T>. Simple isEssay methods work well in
 *   limited places (if and switch statements). We may never have a fully generic type for Asset. When accessing `data`
 *   outside of those boundaries, a type assertion is necessary: `(asset as Asset<'lesson.1'>).data`
 *
 * Which is better the assetNode interface or the AssetUnion
 * according to the interwebs, AssetUnion is better because it can be
 * used more easily as a discriminated union. https://stackoverflow.com/questions/50870423/discriminated-union-of-generic-type
 * https://stackoverflow.com/questions/46312206/narrowing-a-return-type-from-a-generic-discriminated-union-in-typescript
 *
 * */
// export type AssetUnion = Asset<keyof AssetDataMap>
export interface AssetNode extends Asset<AssetNodeTypeId> {
  data: AssetNodeData;
  typeId: AssetNodeTypeId;
}

export interface Asset<T extends TypeId> {
  created: string;
  createdBy: User;
  data: AssetDataMap[T];
  id: number;
  modified: string;
  name: string;
  typeId: T;
}

export type NewAsset<T extends TypeId> = Pick<Asset<T>, 'name' | 'typeId' | 'data'>;

interface RootData {
  title: string;
  subtitle: string;
  description: Option<string>;
  keywords: string;
  archived: boolean;
  license: Option<string>;
  author: Option<string>;
  attribution: Option<string>;
  projectStatus: Option<string>;
}

interface CourseData {
  title: string;
  subtitle: string;
  keywords: string;
  archived: boolean;
  iconCls: string;
  license: Option<string>;
  author: Option<string>;
  attribution: Option<string>;
  defaultCss: string[];
  defaultJs: string[];
  contentStatus: Option<string>; // shared with most assets...
}

interface UnitData {
  title: string;
  iconAlt: string;
  description: string;
  keywords: string;
  archived: boolean;
  iconCls: string;
  license: Option<string>;
  author: Option<string>;
  attribution: Option<string>;
  duration: Option<number>;
  accessRight: Option<string>;
}

// eslint-disable-next-line @typescript-eslint/no-empty-interface
interface ModuleData extends UnitData {}

interface LessonData {
  title: string;
  archived: boolean;
  description: string;
  keywords: string;
  iconAlt: string;
  iconCls: string;
  license: Option<string>;
  author: Option<string>;
  attribution: Option<string>;
  accessRight: Option<string>;
}

type AssessmentType = 'summative' | 'formative';
type AssessmentSubtype = null | undefined | 'checkpoint';
type ScoringOption =
  | 'firstAttemptScore'
  | 'mostRecentAttemptScore'
  | 'highestScore'
  | 'averageScore'
  | 'fullCreditOnAnyCompletion';

interface AssessmentData {
  archived: boolean;
  assessmentType: AssessmentType;
  attribution: Option<string>;
  author: Option<string>;
  displayConfidenceIndicators: boolean;
  hideAnswerIfIncorrect: boolean;
  iconAlt: string;
  iconCls: string;
  immediateFeedback: boolean;
  instructions: {
    parts: HtmlPart[];
  };
  isForCredit: boolean;
  keywords: string;
  license: Option<string>;
  maxAttempts: number;
  unlimitedAttempts: boolean;
  maxMinutes: Option<number>;
  pointsPossible: number;
  randomizeQuestionOrder: boolean;
  scoringOption: ScoringOption | null;
  singlePage: boolean;
  softAttemptLimit: number;
  softLimitMessage: string;
  subtitle: string;
  title: string;
  duration: Option<number>;
}

interface PoolAssessmentData {
  archived: boolean;
  author: Option<string>;
  attribution: Option<string>;
  subtitle: string;
  assessmentType: AssessmentType;
  immediateFeedback: boolean;
  isForCredit: boolean;
  instructions: {
    parts: HtmlPart[];
  };
  keywords: string;
  license: Option<string>;
  maxAttempts: number;
  unlimitedAttempts: boolean;
  maxMinutes: Option<number>;
  numberOfQuestionsForAssessment: number;
  displayConfidenceIndicators: boolean;
  pointsPossible: number;
  hideAnswerIfIncorrect: boolean;
  scoringOption: ScoringOption | null;
  singlePage: boolean;
  title: string;
  useAllQuestions: boolean;
  duration: Option<number>;
}

interface DiagnosticData {
  subtitle: string;
  author: Option<string>;
  displayConfidenceIndicators: boolean;
  immediateFeedback: boolean;
  archived: boolean;
  pointsPossible: number;
  assessmentType: AssessmentType;
  hideAnswerIfIncorrect: boolean;
  scoringOption: ScoringOption | null;
  attribution: Option<string>;
  license: Option<License>;
  maxAttempts: number;
  keywords: string;
  maxMinutes: Option<number>;
  isForCredit: boolean;
  instructions: {
    parts: HtmlPart[];
  };
  title: string;
  singlePage: boolean;
  randomizeQuestionOrder: boolean;
  duration: Option<number>;
}

interface AssignmentData {
  author: Option<string>;
  archived: boolean;
  pointsPossible: number;
  assessmentType: AssessmentType;
  scoringOption: Option<ScoringOption>;
  attribution: Option<string>;
  license: Option<License>;
  maxAttempts: Option<number /* guessed from name: maxAttempts */>;
  keywords: string;
  isForCredit: boolean;
  unlimitedAttempts: boolean;
  instructions: BlockPart;
  title: string;
  duration: Option<number>;
}

interface ObservationAssessmentData {
  author: Option<string>;
  archived: boolean;
  pointsPossible: number;
  assessmentType: AssessmentType;
  scoringOption: Option<ScoringOption>;
  attribution: Option<string>;
  license: Option<License>;
  maxAttempts: Option<number /* guessed from name: maxAttempts */>;
  keywords: string;
  isForCredit: boolean;
  unlimitedAttempts: boolean;
  instructions: BlockPart;
  title: string;
  duration: Option<number>;
}

interface SurveyData {
  title: string;
  archived: boolean;
  description: string;
  keywords: string;
  disabled: boolean;
  inline: boolean;
  programmatic: boolean;
}

interface LikertScaleQuestionData {
  title: string;
  archived: boolean;
}

interface RatingScaleQuestionData {
  title: string;
  archived: boolean;
  max: number;
  lowRatingText: string;
  highRatingText: string;
}

interface VideoCaptionData {
  title: string;
  subtitle: string;
  keywords: string;
  archived: boolean;
  mimeType: string;
  language: string;
  label: Option<string>;
  license: Option<string>;
  author: Option<string>;
  attribution: Option<string>;
  source: Record<BlobRef>;
}

interface MultipleChoiceQuestionData {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
  questionContent: ChoiceQuestionContent;
}

interface TrueFalseQuestionData {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
  questionContent: ChoiceQuestionContent;
}

interface MultipleSelectQuestionData {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  allowPartialCredit: boolean;
  scoringOption: Option<MultipleSelectScoringOption>;
  keywords: string;
  title: string;
  questionContent: ChoiceQuestionContent;
}

export type MultipleSelectScoringOption =
  | 'allOrNothing'
  | 'allowPartialCredit'
  | 'fullCreditForAnyCorrectChoice';

interface EssayQuestionData {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
  questionContent: ChoiceQuestionContent; // well... some of it
}

interface FillInTheBlankQuestionData {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
  questionContent: ChoiceQuestionContent; // well... some of it
}

interface MatchingQuestionData {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
  allowPartialCredit: boolean;
  questionContent: MatchingQuestionContent; // well...
}

export type QuestionData =
  | MultipleChoiceQuestionData
  | TrueFalseQuestionData
  | MultipleSelectQuestionData
  | EssayQuestionData
  | FillInTheBlankQuestionData
  | MatchingQuestionData;

export type ChoiceQuestionContent = {
  'AssessMultiChoiceQuestion.choiceListingType': any;
  'AssessMultiChoiceQuestion.feedbackInlineFlag': boolean;
  'AssessQuestion.complexQuestionText': HtmlPart;
  'AssessQuestion.contentBlockQuestionText': BlockPart;
  'AssessQuestion.pointsPossible': string;
  'AssessQuestion.questionText': string;
  'AssessQuestion.questionTitle': string;
  'AssessQuestion.richCorrectAnswerFeedback': HtmlPart;
  'AssessQuestion.richIncorrectAnswerFeedback': HtmlPart;
  Assess_Question_Choice: ChoiceContent[];
  'AssessmentQuestionConfigurationType.allowDistractorRandomization': boolean;
  caseSensitive?: boolean; // FitB but we're crap on our types
};

export type MatchingQuestionContent = ChoiceQuestionContent & {
  // these aren't optional but this makes the types easier
  'AssessMatchingQuestion.multipleDefinitionsPerTermFlag'?: boolean;
  Assess_Question_Term?: TermContent[];
  Assess_Question_Definition?: DefinitionContent[];
};

export type TermContent = {
  'AssessQuestionTerm.identifier'?: string; // unused
  'AssessQuestionTerm.text': string; // the matching term
  'AssessQuestionTerm.pointValue'?: string; // unused
  'AssessQuestionTerm.feedbackInline'?: string; // unused
  index?: number; // unused
  correctDefinitionIndex: number; // the index
};

export type DefinitionContent = {
  'AssessQuestionDefinition.text': string;
  index: number;
};

// delivery requires 1:1 definitions and terms
export type MatchTuple = {
  definition: DefinitionContent;
  term: TermContent;
};

export type HtmlPart = {
  html: string;
  renderedHtml?: string;
  partType: 'html';
};

export type BlockPart = {
  parts: HtmlPart[];
  renderedHml?: string;
  partType?: 'block'; // non optional but malfeasant types elsewhere
};

export type ChoiceContent = {
  'AssessQuestionChoice.complexText': HtmlPart;
  'AssessQuestionChoice.correct': boolean;
  'AssessQuestionChoice.correctFeedback': HtmlPart;
  'AssessQuestionChoice.incorrectFeedback': HtmlPart;
  'AssessQuestionChoice.pointValue': number;
  'AssessQuestionChoice.text': string;
  index: number;
};

export interface SurveyEssayQuestionData {
  prompt: HtmlPart;
  archived?: boolean;
}

export type SurveyChoice = {
  value: string;
  label: HtmlPart;
};

export interface SurveyChoiceQuestionData {
  prompt: HtmlPart;
  choices: SurveyChoice[];
  keywords: string;
  archived: boolean;
}

export interface ScormData {
  launchNewWindow: boolean;
  zipPaths: any;
  scormTitle: string;
  resourcePath: string;
  allRefs: any;
  passingScore: Option<number>;
  objectiveIds: string[];
  sharedDataIds: string[];
  archived: boolean;
  source: Record<BlobRef>;
  isForCredit: boolean;
  pointsPossible: number;
  duration: Option<number>;
  contentWidth: Option<number>;
  contentHeight: Option<number>;
}

interface HtmlData {
  duration: Option<number>;
  author: Option<string>;
  source: Option<BlobRef>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
  accessRight: Option<string>;
}

export type DisplayFile = {
  path: string;
  mimeType: Option<string>;
  displayName: string;
  renderChoice: 'embed' | 'tab';
};

export type ZipFileTree = {
  title: string;
  path: string;
  nodes: ZipFileTree[];
};

interface FileBundleData {
  title: string;
  subtitle: string;
  iconAlt: string;
  keywords: string;
  archived: boolean;
  iconCls: string;
  duration: number;
  displayFiles: DisplayFile[];
  zipFileTree: ZipFileTree[];
  license: Option<License>;
  author: Option<string>;
  attribution: Option<string>;
  source: Option<BlobRef>;
}

interface WebDependencyData {
  title: string;
  subtitle: string;
  keywords: string;
  archived: boolean;
}

interface CompetencySetData {
  title: string;
  description: string;
  archived: boolean;
  license: Option<License>;
  author: Option<string>;
  attribution: Option<string>;
}

interface CompetencyData {
  title: string;
  description: string;
  keywords: string;
  archived: boolean;
  license: Option<string>;
  author: Option<string>;
  attribution: Option<string>;
}

export type CompetencyAsset = Asset<T.Level1Competency | T.Level2Competency | T.Level3Competency>;

interface DiscussionData {
  title: string;
  iconAlt: string;
  keywords: string;
  archived: boolean;
  iconCls: string;
  instructionsBlock: {
    parts: HtmlPart[];
  };
  instructions: BlockPart;
  viewBeforePosting: boolean;
  allowMultiplePosting: boolean;
  gradable: boolean;
  anonymized: boolean;
  duration: Option<number>;
  assessmentType: AssessmentType;
  license: Option<string>;
  author: Option<string>;
  attribution: Option<string>;
  isForCredit: boolean;
  pointsPossible: number;
}

interface LtiData {
  title: string;
  iconAlt: string;
  duration: Option<number>;
  keywords: string;
  archived: boolean;
  iconCls: string;
  lti: LtiTool;
  instructions: {
    parts: HtmlPart[];
  };
  assessmentType: AssessmentType;
  license: Option<string>;
  author: Option<string>;
  attribution: Option<string>;
  isForCredit: boolean;
  pointsPossible: number;
}

interface GradebookCategoryData {
  title: string;
  weight: number;
  archived: boolean;
}

export type GradebookCategoryAsset = Asset<T.GradebookCategory>;

export type RubricData = {
  title: string;
  keywords: string;
  archived: boolean;
  license: Option<string>;
  author: Option<string>;
  attribution: Option<string>;
};

export type RubricCriterionData = {
  title: string;
  description: string;
  archived: boolean;
  levels: RubricCriterionLevel[];
  license: Option<string>;
  author: Option<string>;
  attribution: Option<string>;
};

type RubricCriterionLevel = {
  points: number;
  name: string;
  description: string;
};

type BlobRef = {
  provider: string;
  name: string;
};

type Resource1Data = {
  accessRight: Option<string>;
  title: string;
  keywords: string;
  archived: boolean;
  license: Option<string>;
  author: Option<string>;
  attribution: Option<string>;
  duration: Option<number>;
  iconAlt: string;
  iconCls: string;
  instructions: {
    parts: HtmlPart[];
  };
  embedCode?: any;
  lti?: any;
  resourceType: 'readingInstructions';
};

interface CourseLinkData {
  title: string;
  duration: Option<number>;
  keywords: string;
  archived: boolean;
  instructions: {
    parts: HtmlPart[];
  };
  license: Option<string>;
  author: Option<string>;
  attribution: Option<string>;
  gradable: boolean;
  isForCredit: boolean;
  pointsPossible: number;
  branch: Option<number>;
  newWindow: boolean;
  sectionPolicy: SectionPolicy;
}

type SectionPolicy = 'MostRecent';

interface ImageData {
  title: string;
  subtitle: string;
  keywords: string;
  archived: boolean;
  altText: Option<string>;
  caption: Option<string>;
  width: number;
  height: number;
  mimeType: string;
  license: Option<string>;
  author: Option<string>;
  attribution: Option<string>;
  source: Option<BlobRef>;
}

interface FileData {
  title: string;
  keywords: string;
  archived: boolean;
  license: Option<string>;
  author: Option<string>;
  attribution: Option<string>;
  source: Option<BlobRef>;
}
