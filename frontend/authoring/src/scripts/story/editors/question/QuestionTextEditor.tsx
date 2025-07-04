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

import React, { useCallback } from 'react';

import { ChoiceQuestionContent, HtmlPart, NewAsset } from '../../../types/asset';
import { EssayQuestion } from '../../../types/typeIds';
import { PartEditor } from '../../PartEditor';
import {
  ComplexQuestionText,
  ContentBlockQuestionText,
  StoryQuestionTypes,
} from '../../questionUtil';
import { useNarrativeAssetState } from '../../storyHooks';

export const QuestionTextEditor: React.FC<{
  question: NewAsset<StoryQuestionTypes>;
  content: ChoiceQuestionContent;
  editContent: (
    comment: string,
    key: string | undefined,
    update: Partial<ChoiceQuestionContent>
  ) => void;
  readOnly: boolean;
}> = ({ question, content, editContent, readOnly }) => {
  const { created } = useNarrativeAssetState(question);
  const essay = question.typeId === EssayQuestion;

  const editText = useCallback(
    (html: HtmlPart, session: string) =>
      editContent('Question prompt', session, {
        [ComplexQuestionText]: html,
      }),
    [content, editContent]
  );

  const editEssay = useCallback(
    (html: HtmlPart, session: string) =>
      editContent('Essay prompt', session, {
        [ContentBlockQuestionText]: {
          partType: 'block',
          parts: [html],
        },
      }),
    [content, editContent]
  );

  return essay ? (
    <PartEditor
      id="prompt"
      placeholder={'Question text'}
      asset={question}
      part={content[ContentBlockQuestionText]}
      onChange={editEssay}
      autoedit={created}
      readOnly={readOnly}
    />
  ) : (
    <PartEditor
      id="prompt"
      placeholder={'Question text'}
      asset={question}
      part={content[ComplexQuestionText]}
      onChange={editText}
      autoedit={created}
      fillInTheBlank={question.typeId === 'fillInTheBlankQuestion.1'}
      readOnly={readOnly}
    />
  );
};
