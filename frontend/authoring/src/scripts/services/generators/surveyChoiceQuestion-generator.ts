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

import emptyHtmlPart from '../../editors/constants/emptyHtmlPart.constant';
import { SurveyChoiceQuestionData } from '../../types/asset';

export function computeNewGuid(): string {
  return (
    'choice_' +
    Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .substring(1)
  );
}

function generateSurveyChoice(asset: { title: string }): Partial<SurveyChoiceQuestionData> {
  const defaultEssay: Partial<SurveyChoiceQuestionData> = {
    prompt: {
      html: asset.title,
      partType: 'html' as const,
    },
    choices: [
      {
        value: computeNewGuid(),
        label: emptyHtmlPart,
      },
      {
        value: computeNewGuid(),
        label: emptyHtmlPart,
      },
      {
        value: computeNewGuid(),
        label: emptyHtmlPart,
      },
      {
        value: computeNewGuid(),
        label: emptyHtmlPart,
      },
    ],
  };
  delete asset.title;

  return Object.assign({}, defaultEssay, asset);
}

export default generateSurveyChoice;
