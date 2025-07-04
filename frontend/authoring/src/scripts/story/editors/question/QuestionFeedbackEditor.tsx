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
import { PartEditor } from '../../PartEditor';
import {
  RichCorrectAnswerFeedback,
  RichIncorrectAnswerFeedback,
  StoryQuestionTypes,
} from '../../questionUtil';

export const QuestionFeedbackEditor: React.FC<{
  question: NewAsset<StoryQuestionTypes>;
  content: ChoiceQuestionContent;
  editContent: (
    comment: string,
    key: string | undefined,
    update: Partial<ChoiceQuestionContent>
  ) => void;
  readOnly: boolean;
}> = ({ question, content, editContent, readOnly }) => {
  const editCorrectFeedback = useCallback(
    (html: HtmlPart, session: string) =>
      editContent('Correct feedback', session, {
        [RichCorrectAnswerFeedback]: html,
      }),
    [editContent]
  );

  const editIncorrectFeedback = useCallback(
    (html: HtmlPart, session: string) =>
      editContent('Incorrect feedback', session, {
        [RichIncorrectAnswerFeedback]: html,
      }),
    [editContent]
  );

  return (
    <>
      <PartEditor
        id="correct-feedback"
        sometimes
        className="text-success"
        placeholder="Correct answer feedback"
        asset={question}
        part={content[RichCorrectAnswerFeedback]}
        onChange={editCorrectFeedback}
        readOnly={readOnly}
      />
      <PartEditor
        id="incorrect-feedback"
        sometimes
        className="text-danger"
        placeholder="Incorrect answer feedback"
        asset={question}
        part={content[RichIncorrectAnswerFeedback]}
        onChange={editIncorrectFeedback}
        readOnly={readOnly}
      />
    </>
  );
};
