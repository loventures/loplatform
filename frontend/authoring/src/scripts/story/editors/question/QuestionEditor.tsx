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

import React, { useCallback, useEffect } from 'react';
import { useDispatch } from 'react-redux';

import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
} from '../../../graphEdit';
import { ChoiceQuestionContent, MatchingQuestionContent, QuestionData } from '../../../types/asset';
import {
  EssayQuestion,
  FillInTheBlankQuestion,
  MatchingQuestion,
  MultipleChoiceQuestion,
  MultipleSelectQuestion,
} from '../../../types/typeIds';
import { isBlankHtml } from '../../editorUtils';
import { ChoiceCorrect, Choices, isDistractable, StoryQuestionTypes } from '../../questionUtil';
import { QuestionSettingsEditor } from '../../settings';
import { NarrativeEditor } from '../../story';
import { setNarrativeAssetState } from '../../storyActions';
import { QuestionChoiceEditor } from './QuestionChoiceEditor';
import { QuestionFeedbackEditor } from './QuestionFeedbackEditor';
import { QuestionMatchEditor } from './QuestionMatchEditor';
import { QuestionTextEditor } from './QuestionTextEditor';

const FitbRE = /{{.*}}/;

export const QuestionEditor: NarrativeEditor<StoryQuestionTypes> = ({
  asset: question,
  readOnly,
}) => {
  const dispatch = useDispatch();

  const content = question.data.questionContent;

  const multipleChoice = question.typeId === MultipleChoiceQuestion;
  const multipleSelect = question.typeId === MultipleSelectQuestion;
  const matching = question.typeId === MatchingQuestion;
  const fitb = question.typeId === FillInTheBlankQuestion;
  const essay = question.typeId === EssayQuestion;
  const distractable = isDistractable(question.typeId);

  const correctCount = content[Choices]?.filter(c => c[ChoiceCorrect]).length ?? 0;
  const html = content['AssessQuestion.complexQuestionText']?.html;

  // TODO: invalid on null points
  const invalid = multipleChoice
    ? correctCount !== 1
    : multipleSelect
      ? correctCount === 0
      : fitb
        ? !isBlankHtml(html) && !html.match(FitbRE)
        : false;

  const editContent = useCallback(
    (
      comment: string,
      key: string | undefined,
      update: Partial<ChoiceQuestionContent | MatchingQuestionContent>,
      update2: Partial<QuestionData> = {}
    ): void => {
      dispatch(beginProjectGraphEdit(comment, key));
      dispatch(
        editProjectGraphNodeData(question.name, {
          questionContent: { ...content, ...update },
          ...update2,
        })
      );
      if (!key) dispatch(autoSaveProjectGraphEdits()); // TODO: BAD FIXME BLUR
    },
    [dispatch, content]
  );

  useEffect(() => {
    dispatch(setNarrativeAssetState(question.name, { invalid }));
  }, [invalid]);

  return (
    <div className="mb-3 eat-space">
      {invalid && !readOnly && (
        <div className="text-danger mb-3 ms-3 fw-bold text-center">
          {multipleSelect
            ? 'Question must have at least one correct answer.'
            : multipleChoice
              ? 'Question must have exactly one correct answer.'
              : 'Question must have at least one blank.'}
        </div>
      )}

      {!readOnly /* Don't show settings in preview. */ && (
        <QuestionSettingsEditor
          question={question}
          content={content}
          editContent={editContent}
          readOnly={readOnly}
        />
      )}
      <QuestionTextEditor
        question={question}
        content={content}
        editContent={editContent}
        readOnly={readOnly}
      />
      {distractable ? (
        <QuestionChoiceEditor
          question={question}
          content={content}
          editContent={editContent}
          readOnly={readOnly}
        />
      ) : matching ? (
        <QuestionMatchEditor
          question={question}
          content={content}
          editContent={editContent}
          readOnly={readOnly}
        />
      ) : null}
      {!essay && (
        <QuestionFeedbackEditor
          question={question}
          content={content}
          editContent={editContent}
          readOnly={readOnly}
        />
      )}
    </div>
  );
};
