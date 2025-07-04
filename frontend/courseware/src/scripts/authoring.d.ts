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

export type Asset<A> = {
  typeId: string;
  name: string;
  id: number;
  data: A;
};

type HtmlPart = {
  html: string;
  partType: 'html';
  renderedHtml: string;
};

export type EssayQuestion = {
  title: string;
  questionContent: any;
};

export type LikertScaleQuestion1 = {
  title: string;
};

export type MultipleChoiceQuestion = {
  title: string;
  questionContent: any;
};

export type RatingScaleQuestion1 = {
  title: string;
  max: number;
  lowRatingText: string;
  highRatingText: string;
};

export type SurveyChoiceQuestion1 = {
  prompt: HtmlPart;
  keywords: string;
  choices: SurveyChoice[];
};

type SurveyChoice = {
  value: string;
  label: HtmlPart;
};

export type SurveyEssayQuestion1 = {
  prompt: HtmlPart;
  keywords: string;
};
