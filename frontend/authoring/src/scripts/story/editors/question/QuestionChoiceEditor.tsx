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

import { maxBy } from 'lodash';
import React, { useCallback, useMemo } from 'react';

import { ChoiceContent, ChoiceQuestionContent, HtmlPart, NewAsset } from '../../../types/asset';
import { MultipleChoiceQuestion, MultipleSelectQuestion } from '../../../types/typeIds';
import {
  ChoiceCorrect,
  ChoiceCorrectFeedback,
  ChoiceIncorrectFeedback,
  ChoicePartKey,
  ChoicePointValue,
  Choices,
  PointsPossible,
  StoryQuestionTypes,
  emptyHtml,
  newChoice,
  parseStringDefaultOne,
} from '../../questionUtil';
import { useFocusedRemoteEditor, useIsEditable } from '../../storyHooks';
import { ChoiceEditor } from './ChoiceEditor';

const NoChoices = [];

export const QuestionChoiceEditor: React.FC<{
  question: NewAsset<StoryQuestionTypes>;
  content: ChoiceQuestionContent;
  editContent: (
    comment: string,
    key: string | undefined,
    update: Partial<ChoiceQuestionContent>
  ) => void;
  readOnly: boolean;
}> = ({ question, content, editContent, readOnly }) => {
  const editMode = useIsEditable(question.name) && !readOnly;

  const multipleChoice = question.typeId === MultipleChoiceQuestion;
  const multipleSelect = question.typeId === MultipleSelectQuestion;
  const choices: ChoiceContent[] = content[Choices] ?? NoChoices; // essay has none
  const correctCount = choices.filter(c => c[ChoiceCorrect]).length;

  const nextIndex = (maxBy(choices, 'index')?.index ?? -1) + 1;

  const nextChoice = useMemo(() => newChoice({ part: emptyHtml(), index: nextIndex }), [nextIndex]);

  const choicesWithNext = useMemo(
    () => [...choices, ...(editMode && (multipleSelect || multipleChoice) ? [nextChoice] : [])],
    [choices, nextChoice, editMode]
  );

  const editChoices = useCallback(
    (comment: string, key: string | undefined, choices: ChoiceContent[]) => {
      const pointsPossible = parseStringDefaultOne(content[PointsPossible]);
      const incorrectCount = choices.length - correctCount;
      const pointsForCorrect = pointsPossible / correctCount;
      const pointsForIncorrect = multipleSelect ? -(pointsPossible / incorrectCount) : 0;
      const cleaned = choices.map(choice => ({
        ...choice,
        [ChoicePointValue]: choice[ChoiceCorrect] ? pointsForCorrect : pointsForIncorrect,
        [choice[ChoiceCorrect] ? ChoiceIncorrectFeedback : ChoiceCorrectFeedback]: emptyHtml(),
        // index - cannot reindex them or react keys break
      }));
      editContent(comment, key, { [Choices]: cleaned });
    },
    [content, editContent]
  );

  const editChoice = useCallback(
    (comment: string, key: string | undefined, index: number, choice: ChoiceContent): void => {
      const choices = [...content[Choices]];
      choices[index] = choice;
      editChoices(comment, key, choices);
    },
    [content, editChoices]
  );

  const clickCorrect = useCallback(
    (choice: ChoiceContent) => {
      if (!multipleSelect && choice[ChoiceCorrect]) return;
      const choices = content[Choices].map(c => ({
        ...c,
        [ChoiceCorrect]: !multipleSelect
          ? c === choice
          : c === choice
            ? !c[ChoiceCorrect]
            : c[ChoiceCorrect],
      }));
      editChoices('Choice correctness', undefined, choices);
    },
    [multipleSelect, content, editChoices]
  );

  const deleteChoice = useCallback(
    (choice: ChoiceContent) => {
      const choices = content[Choices].filter(c => c !== choice);
      editChoices('Delete choice', undefined, choices);
    },
    [content, editChoices]
  );

  const editChoicePart = useCallback(
    (choice: ChoiceContent, key: ChoicePartKey, comment: string, session: string, part: HtmlPart) =>
      editChoice(comment, session, choicesWithNext.indexOf(choice), { ...choice, [key]: part }),
    [choicesWithNext, editChoice]
  );
  const { onFocus, onBlur, remoteEditor } = useFocusedRemoteEditor(question.name, 'choices');

  return (
    <div
      className="mt-2"
      style={remoteEditor}
    >
      {choicesWithNext.map(choice => (
        <ChoiceEditor
          key={choice.index}
          question={question}
          choice={choice}
          clickCorrect={clickCorrect}
          updatePart={editChoicePart}
          deleteChoice={deleteChoice}
          addChoice={choice.index === nextIndex}
          readOnly={readOnly}
          onFocus={onFocus}
          onBlur={onBlur}
          remoteEdit={remoteEditor ? 'remote-edit' : undefined}
        />
      ))}
    </div>
  );
};
