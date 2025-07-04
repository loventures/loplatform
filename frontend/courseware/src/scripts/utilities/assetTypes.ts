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

import { QuestionScoringOption } from '../api/quizApi.ts';

import { Option } from '../types/option.ts';

export type ChoiceContent = {
  incorrectChoiceFeedback: Option<HtmlPart>;
  choiceContent: Option<HtmlPart>;
  choiceText: Option<string>;
  correct: boolean;
  points: number;
  correctChoiceFeedback: Option<HtmlPart>;
  index: number;
};

// --------------

export type RubricCriterionData = {
  author: Option<string>;
  levels: RubricCriterionLevel[];
  description: string;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  title: string;
};

// --------------

export type DefinitionContent = {
  definitionText: string;
  index: number;
};

// --------------

export type HtmlData = {
  duration: Option<number /* guessed from name: duration */>;
  author: Option<string>;
  source: Option<BlobRef>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
};

// --------------

export type ScormData = {
  duration: Option<number /* guessed from name: duration */>;
  author: Option<string>;
  source: Option<BlobRef>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
  displayFiles: DisplayFile[];
  zipFileTree: ZipFileTree[];
  pointsPossible: number;
  isForCredit: boolean;
  contentWidth: Option<number>;
  contentHeight: Option<number>;
  launchNewWindow: boolean;
};

// --------------

export type ShortAnswerQuestionData = {
  questionContent: ShortAnswerContent;
  license: Option<License>;
  author: string;
  attribution: string;
};

// --------------

export type TermContent = {
  correctIndex: number;
  feedbackInline: Option<string>;
  pointValue: Option<string>;
  termIdentifier: Option<string>;
  termText: string;
  index: Option<number /* guessed from name: index */>;
};

// --------------

export type AudioData = {
  subtitle: string;
  author: Option<string>;
  source: Option<BlobRef>;
  archived: boolean;
  attribution: Option<string>;
  mimeType: Option<string>;
  license: Option<License>;
  caption: Option<string>;
  keywords: string;
  title: string;
};

// --------------

export type Resource1Data = {
  duration: Option<number /* guessed from name: duration */>;
  author: Option<string>;
  resourceType: ResourceType;
  lti: Option<AssetLtiToolConfiguration>;
  archived: boolean;
  embedCode: Option<string>;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  instructions: BlockPart;
  title: string;
};

// --------------

export type DiagnosticData = {
  subtitle: string;
  author: Option<string>;
  name: Option<string>;
  shouldDisplayConfidenceIndicator: boolean;
  immediateFeedback: boolean;
  archived: boolean;
  pointsPossible: number;
  assessmentType: AssessmentType;
  shouldHideAnswerIfIncorrect: boolean;
  scoringOption: Option<ScoringOption>;
  attribution: Option<string>;
  license: Option<License>;
  maxAttempts: Option<number /* guessed from name: maxAttempts */>;
  keywords: string;
  maxMinutes: Option<number /* guessed from name: maxMinutes */>;
  isForCredit: boolean;
  instructions: BlockPart;
  title: string;
  singlePage: boolean;
  shouldRandomizeQuestionOrder: boolean;
};

// --------------

export type HotspotQuestionContent = {
  richCorrectAnswerFeedback: Option<HtmlPart>;
  questionTitle: Option<string>;
  hotspotVisible: boolean;
  choices: HotspotChoice[];
  questionComplexText: HtmlPart;
  pointsPossible: string;
  richIncorrectAnswerFeedback: Option<HtmlPart>;
  questionContentBlockText: BlockPart;
  allowDistractorRandomization: boolean;
  questionText: Option<string>;
};

// --------------

export type AssessmentData = {
  subtitle: string;
  author: Option<string>;
  name: Option<string>;
  shouldDisplayConfidenceIndicator: boolean;
  immediateFeedback: boolean;
  archived: boolean;
  pointsPossible: number;
  assessmentType: AssessmentType;
  shouldHideAnswerIfIncorrect: boolean;
  scoringOption: Option<ScoringOption>;
  attribution: Option<string>;
  license: Option<License>;
  maxAttempts: Option<number /* guessed from name: maxAttempts */>;
  softAttemptLimit: Option<number>;
  keywords: string;
  maxMinutes: Option<number /* guessed from name: maxMinutes */>;
  isForCredit: boolean;
  instructions: BlockPart;
  title: string;
  softLimitMessage: Option<string>;
  singlePage: boolean;
  shouldRandomizeQuestionOrder: boolean;
};

// --------------

export type DisplayFileRenderChoice = 'tab' | 'embed';

// --------------

export type FileData = {
  author: Option<string>;
  source: Option<BlobRef>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
};

// --------------

export type BlockPart = {
  parts: any[];
  renderedHtml: Option<string>;
};

// --------------

export type ImageAssetData = {
  subtitle: string;
  author: Option<string>;
  source: Option<BlobRef>;
  archived: boolean;
  altText: Option<string>;
  height: number;
  attribution: Option<string>;
  mimeType: string;
  license: Option<License>;
  caption: Option<string>;
  keywords: string;
  title: string;
  width: number;
};

// --------------

export type FillInTheBlankQuestionData = {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  scoringOption: QuestionScoringOption;
  keywords: string;
  title: string;
  questionContent: FillInTheBlankContent;
};

// --------------

export type AssetLtiToolConfiguration = {
  toolId: string;
  name: string;
  toolConfiguration: LtiLaunchConfiguration;
};

// --------------

export type WebDependencyData = {
  subtitle: string;
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
};

// --------------

export type GradebookTemplate = any;

// --------------

export type ScoringOption = {
  gradeCalculationType: string;
};

// --------------

export type LtiData = {
  duration: Option<number /* guessed from name: duration */>;
  author: Option<string>;
  lti: AssetLtiToolConfiguration;
  archived: boolean;
  pointsPossible: number;
  assessmentType: AssessmentType;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  isForCredit: boolean;
  instructions: BlockPart;
  title: string;
};

// --------------

export type ResourceType = Record<string, unknown>;

// --------------

export type OrderingContent = {
  richCorrectAnswerFeedback: Option<HtmlPart>;
  questionTitle: Option<string>;
  choices: OrderingChoice[];
  questionComplexText: HtmlPart;
  pointsPossible: string;
  richIncorrectAnswerFeedback: Option<HtmlPart>;
  questionContentBlockText: BlockPart;
  allowDistractorRandomization: boolean;
  questionText: Option<string>;
};

// --------------

export type HorizontalRuleData = Record<string, unknown>;

// --------------

export type FileBundleData = {
  duration: Option<number /* guessed from name: duration */>;
  subtitle: string;
  author: Option<string>;
  source: Option<BlobRef>;
  archived: boolean;
  displayFiles: DisplayFile[];
  zipFileTree: ZipFileTree[];
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
};

// --------------

export type ChoiceQuestionContent = {
  richCorrectAnswerFeedback: Option<HtmlPart>;
  questionTitle: Option<string>;
  choices: ChoiceContent[];
  questionComplexText: HtmlPart;
  pointsPossible: string;
  richIncorrectAnswerFeedback: Option<HtmlPart>;
  questionContentBlockText: BlockPart;
  allowDistractorRandomization: boolean;
  choiceListingType: Option<string>;
  questionText: Option<string>;
  choiceInlineFeedback: boolean;
};

// --------------

export type RubricData = {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
};

// --------------

export type BlobRef = {
  name: string;
  provider: string;
  size: number;
  contentType: MediaType;
  filename: string;
};

// --------------

export type FillInTheBlankContent = {
  richCorrectAnswerFeedback: Option<HtmlPart>;
  questionTitle: Option<string>;
  questionComplexText: HtmlPart;
  pointsPossible: string;
  richIncorrectAnswerFeedback: Option<HtmlPart>;
  questionContentBlockText: BlockPart;
  allowDistractorRandomization: boolean;
  questionText: Option<string>;
};

// --------------

export type Level1CompetencyData = {
  author: Option<string>;
  description: string;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
};

// --------------

export type MatchingContent = {
  richCorrectAnswerFeedback: Option<HtmlPart>;
  questionTitle: Option<string>;
  questionComplexText: HtmlPart;
  pointsPossible: string;
  richIncorrectAnswerFeedback: Option<HtmlPart>;
  questionContentBlockText: BlockPart;
  definitionContent: DefinitionContent[];
  multipleDefinitionsPerTermAllowed: boolean;
  allowDistractorRandomization: boolean;
  terms: TermContent[];
  questionText: Option<string>;
};

// --------------

export type EssayContent = {
  richCorrectAnswerFeedback: Option<HtmlPart>;
  questionTitle: Option<string>;
  questionComplexText: HtmlPart;
  pointsPossible: string;
  richIncorrectAnswerFeedback: Option<HtmlPart>;
  questionContentBlockText: BlockPart;
  allowDistractorRandomization: boolean;
  questionText: Option<string>;
};

// --------------

export type MediaType = string;

// --------------

export type Level3CompetencyData = {
  author: Option<string>;
  description: string;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
};

// --------------

export type LtiLaunchConfiguration = {
  secret: Option<string>;
  useExternalId: Option<boolean>;
  customParameters: { [s: string]: string };
  includeUsername: Option<boolean /* guessed from name: includeUsername */>;
  includeRoles: Option<boolean /* guessed from name: includeRoles */>;
  url: Option<string>;
  isGraded: Option<boolean /* guessed from name: isGraded */>;
  launchStyle: Option<LtiLaunchStyle>;
  key: Option<string>;
  ltiVersion: Option<string>;
  ltiMessageType: Option<string>;
  includeEmailAddress: Option<boolean /* guessed from name: includeEmailAddress */>;
  includeContextTitle: Option<boolean /* guessed from name: includeContextTitle */>;
};

// --------------

export type Level2CompetencyData = {
  author: Option<string>;
  description: string;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
};

// --------------

export type BinContent = {
  index: number;
  label: string;
};

// --------------

export type MultipleSelectQuestionData = {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  scoringOption: QuestionScoringOption;
  keywords: string;
  title: string;
  questionContent: ChoiceQuestionContent;
};

// --------------

export type ZipFileTree = {
  title: string;
  path: string;
  nodes: ZipFileTree[];
};

// --------------

export type License = {
  entryName: string;
  text: string;
};

// --------------

export type HtmlPart = {
  html: string;
  renderedHtml: Option<string>;
};

// --------------

export type AssessmentType = 'formative' | 'summative';

// --------------

export type StylesheetData = {
  subtitle: string;
  author: Option<string>;
  source: Option<BlobRef>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
};

// --------------

export type HotspotQuestionData = {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
  questionContent: HotspotQuestionContent;
};

// --------------

export type LtiLaunchStyle = 'NEW_WINDOW' | 'FRAMED';

// --------------

export type RubricCriterionLevel = {
  points: number;
  name: string;
  description: string;
};

// --------------

export type ShortAnswerContent = {
  richCorrectAnswerFeedback: Option<HtmlPart>;
  questionTitle: Option<string>;
  questionComplexText: HtmlPart;
  pointsPossible: string;
  richIncorrectAnswerFeedback: Option<HtmlPart>;
  questionContentBlockText: BlockPart;
  answer: string;
  allowDistractorRandomization: boolean;
  questionText: Option<string>;
  width: number;
};

// --------------

export type HotspotChoice = {
  x: number;
  y: number;
  correct: boolean;
  height: number;
  radius: number;
  shape: string;
  width: number;
};

// --------------

export type CourseData = {
  defaultJs: string[];
  subtitle: string;
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  gradebookTemplate: GradebookTemplate;
  keywords: string;
  iconCls: string;
  title: string;
  defaultCss: string[];
};

// --------------

export type PdfData = {
  subtitle: string;
  author: Option<string>;
  source: Option<BlobRef>;
  archived: boolean;
  attribution: Option<string>;
  mimeType: string;
  license: Option<License>;
  keywords: string;
  title: string;
};

// --------------

export type LessonData = {
  author: Option<string>;
  iconAlt: string;
  description: string;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  iconCls: string;
  title: string;
};

// --------------

export type RootData = {
  subtitle: string;
  author: Option<string>;
  description: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
};

// --------------

export type PoolAssessmentData = {
  subtitle: string;
  author: Option<string>;
  name: Option<string>;
  shouldDisplayConfidenceIndicator: boolean;
  useAllQuestions: boolean;
  immediateFeedback: boolean;
  archived: boolean;
  pointsPossible: number;
  assessmentType: AssessmentType;
  shouldHideAnswerIfIncorrect: boolean;
  scoringOption: Option<ScoringOption>;
  attribution: Option<string>;
  license: Option<License>;
  maxAttempts: Option<number /* guessed from name: maxAttempts */>;
  keywords: string;
  maxMinutes: Option<number /* guessed from name: maxMinutes */>;
  isForCredit: boolean;
  instructions: BlockPart;
  title: string;
  singlePage: boolean;
  numberOfQuestionsForAssessment: number;
};

// --------------

export type VideoData = {
  subtitle: string;
  author: Option<string>;
  source: Option<BlobRef>;
  archived: boolean;
  attribution: Option<string>;
  mimeType: string;
  license: Option<License>;
  caption: Option<string>;
  keywords: string;
  title: string;
};

// --------------

export type ObservationAssessment1Data = {
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
};

// --------------

export type TableData = {
  html: string;
  license: Option<License>;
  author: Option<string>;
  attribution: Option<string>;
};

// --------------

export type BinDropContent = {
  richCorrectAnswerFeedback: Option<HtmlPart>;
  questionTitle: Option<string>;
  questionComplexText: HtmlPart;
  pointsPossible: string;
  richIncorrectAnswerFeedback: Option<HtmlPart>;
  questionContentBlockText: BlockPart;
  options: OptionContent[];
  allowDistractorRandomization: boolean;
  questionText: Option<string>;
  bins: BinContent[];
};

// --------------

export type ChoiceQuestionData = {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
  questionContent: ChoiceQuestionContent;
};

// --------------

export type ListBlockData = {
  html: string;
  license: Option<License>;
  author: Option<string>;
  attribution: Option<string>;
};

// --------------

export type DisplayFile = {
  path: string;
  mimeType: Option<string>;
  displayName: string;
  renderChoice: DisplayFileRenderChoice;
};

// --------------

export type Discussion1Data = {
  duration: Option<number /* guessed from name: duration */>;
  author: Option<string>;
  legacyInstructions: HtmlPart;
  archived: boolean;
  pointsPossible: number;
  assessmentType: AssessmentType;
  shouldAllowMultiplePosting: Option<boolean /* guessed from name: shouldAllowMultiplePosting */>;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  isForCredit: boolean;
  anonymized: boolean;
  instructions: BlockPart;
  title: string;
  gradable: boolean;
  viewBeforePosting: boolean;
};

// --------------

export type EssayQuestionData = {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
  questionContent: EssayContent;
};

// --------------

export type BinDropQuestionData = {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  scoringOption: QuestionScoringOption;
  keywords: string;
  title: string;
  questionContent: BinDropContent;
};

// --------------

export type Assignment1Data = {
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
};

// --------------

export type OrderingChoice = {
  incorrectChoiceFeedback: Option<HtmlPart>;
  choiceContent: Option<HtmlPart>;
  choiceText: Option<string>;
  renderingIndex: number;
  correct: boolean;
  answerIndex: number;
  points: number;
  choiceIdentifier: Option<string>;
  correctChoiceFeedback: Option<HtmlPart>;
  index: number;
};

// --------------

export type VideoCaptionData = {
  subtitle: string;
  author: Option<string>;
  source: Option<BlobRef>;
  archived: boolean;
  label: Option<string>;
  attribution: Option<string>;
  mimeType: string;
  license: Option<License>;
  language: string;
  keywords: string;
  title: string;
};

// --------------

export type FontData = {
  author: Option<string>;
  source: Option<BlobRef>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
};

// --------------

export type MatchingQuestionData = {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  scoringOption: QuestionScoringOption;
  keywords: string;
  title: string;
  questionContent: MatchingContent;
};

// --------------

export type CompetencySetData = {
  author: Option<string>;
  description: string;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  title: string;
};

// --------------

export type OptionContent = {
  index: number;
  label: string;
  binIndex: Option<number /* guessed from name: binIndex */>;
};

// --------------

export type ModuleData = {
  author: Option<string>;
  iconAlt: string;
  description: string;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  iconCls: string;
  title: string;
};

// --------------

export type OrderingQuestionData = {
  author: Option<string>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  scoringOption: QuestionScoringOption;
  keywords: string;
  title: string;
  questionContent: OrderingContent;
};

// --------------

export type JavascriptData = {
  subtitle: string;
  author: Option<string>;
  source: Option<BlobRef>;
  archived: boolean;
  attribution: Option<string>;
  license: Option<License>;
  keywords: string;
  title: string;
};

// --------------

export type AssetTypeMap = {
  'poolAssessment.1': PoolAssessmentData;
  'table.1': TableData;
  'course.1': CourseData;
  'html.1': HtmlData;
  'scorm.1': ScormData;
  'resource.1': Resource1Data;
  'audio.1': AudioData;
  'file.1': FileData;
  'competencySet.1': CompetencySetData;
  'rubricCriterion.1': RubricCriterionData;
  'lti.1': LtiData;
  'root.1': RootData;
  'rubric.1': RubricData;
  'observationAssessment.1': ObservationAssessment1Data;
  'diagnostic.1': DiagnosticData;
  'css.1': StylesheetData;
  'level2Competency.1': Level2CompetencyData;
  'js.1': JavascriptData;
  'list.1': ListBlockData;
  'discussion.1': Discussion1Data;
  'multipleSelectQuestion.1': MultipleSelectQuestionData;
  'video.1': VideoData;
  'trueFalseQuestion.1': ChoiceQuestionData;
  'level3Competency.1': Level3CompetencyData;
  'fileBundle.1': FileBundleData;
  'pdf.1': PdfData;
  'image.1': ImageAssetData;
  'assessment.1': AssessmentData;
  'checkpoint.1': AssessmentData;
  'videoCaption.1': VideoCaptionData;
  'webDependency.1': WebDependencyData;
  'assignment.1': Assignment1Data;
  'fillInTheBlankQuestion.1': FillInTheBlankQuestionData;
  'binDropQuestion.1': BinDropQuestionData;
  'hotspotQuestion.1': HotspotQuestionData;
  'shortAnswerQuestion.1': ShortAnswerQuestionData;
  'horizontalRule.1': HorizontalRuleData;
  'essayQuestion.1': EssayQuestionData;
  'unit.1': ModuleData;
  'module.1': ModuleData;
  'lesson.1': LessonData;
  'font.1': FontData;
  'level1Competency.1': Level1CompetencyData;
  'multipleChoiceQuestion.1': ChoiceQuestionData;
  'orderingQuestion.1': OrderingQuestionData;
  'matchingQuestion.1': MatchingQuestionData;
};

export type AssetTypeId = keyof AssetTypeMap;

export type AssetInfo<T extends AssetTypeId = AssetTypeId> = {
  id: number;
  name: string;
  typeId: T;
  created: Date;
  createdBy: Option<{ id: number }>;
  modified: Date;
};

export type AssetNode<T extends AssetTypeId = AssetTypeId> = {
  info: AssetInfo<T>;
  data: AssetTypeMap[T];
};

export function is<K extends AssetTypeId>(k: K, a: AssetNode): a is AssetNode<K> {
  return k === a.info.typeId;
}

export function as<K extends AssetTypeId>(a: AssetNode, k: K): Option<AssetNode<K>> {
  return is(k, a) ? a : null;
}
