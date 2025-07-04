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

import classNames from 'classnames';
import React, { useCallback, useRef, useState } from 'react';
import { IoCheckmarkOutline, IoCloseOutline, IoTrashOutline } from 'react-icons/io5';
import { Button } from 'reactstrap';

import { ChoiceContent, HtmlPart, NewAsset } from '../../../types/asset';
import { TrueFalseQuestion } from '../../../types/typeIds';
import { FocusLoser } from '../../components/FocusLoser';
import { PartEditor } from '../../PartEditor';
import {
  ChoiceComplexText,
  ChoiceCorrect,
  ChoiceCorrectFeedback,
  ChoiceIncorrectFeedback,
  ChoicePartKey,
  StoryQuestionTypes,
} from '../../questionUtil';
import { useIsEditable } from '../../storyHooks';
import { isBlankHtml } from '../../editorUtils.ts';

export const ChoiceEditor: React.FC<{
  question: NewAsset<StoryQuestionTypes>;
  choice: ChoiceContent;
  addChoice: boolean;
  clickCorrect: (choice: ChoiceContent) => void;
  updatePart: (
    choice: ChoiceContent,
    key: ChoicePartKey,
    comment: string,
    session: string,
    part: HtmlPart
  ) => void;
  deleteChoice: (choice: ChoiceContent) => void;
  readOnly: boolean;
  onFocus: () => void;
  onBlur: (event: React.SyntheticEvent) => void;
  remoteEdit?: string;
}> = ({
  question,
  choice,
  addChoice,
  clickCorrect,
  updatePart,
  deleteChoice,
  readOnly,
  onFocus,
  onBlur,
  remoteEdit,
}) => {
  const editMode = useIsEditable(question.name) && !readOnly;
  const index = choice.index;
  const trueFalse = question.typeId === TrueFalseQuestion;
  const [dirty, setDirty] = useState(false);

  const maybeDelete = useCallback(() => {
    // if you delete all the content in a choice and tab out, delete it.
    // if you just tab through and don't modify, don't delete it.
    const isBlank = (key: ChoicePartKey) => isBlankHtml(choice[key].html);
    if (
      dirty &&
      isBlank(ChoiceComplexText) &&
      isBlank(choice[ChoiceCorrect] ? ChoiceCorrectFeedback : ChoiceIncorrectFeedback)
    ) {
      deleteChoice(choice);
    }
  }, [choice, dirty, deleteChoice]);

  const doClickCorrect = useCallback(() => {
    clickCorrect(choice);
  }, [choice, clickCorrect]);

  const editText = useCallback(
    (part: HtmlPart, session: string) => {
      setDirty(true);
      updatePart(
        choice,
        ChoiceComplexText,
        addChoice ? 'Add choice' : 'Edit choice text',
        session,
        part
      );
    },
    [choice, updatePart, setDirty, addChoice]
  );

  const editCorrectFeedback = useCallback(
    (part: HtmlPart, session: string) => {
      setDirty(true);
      updatePart(choice, ChoiceCorrectFeedback, 'Edit choice correct feedback', session, part);
    },
    [choice, updatePart, setDirty]
  );

  const editIncorrectFeedback = useCallback(
    (part: HtmlPart, session: string) => {
      setDirty(true);
      updatePart(choice, ChoiceIncorrectFeedback, 'Edit choice incorrect feedback', session, part);
    },
    [choice, updatePart, setDirty]
  );

  const divRef = useRef<HTMLDivElement>();

  return (
    <FocusLoser
      divRef={divRef}
      focusLost={maybeDelete}
    >
      {onFocusHandler => (
        <div
          ref={divRef}
          onFocus={onFocusHandler}
          className="d-flex align-items-start my-2 choice position-relative"
        >
          <div className="d-flex flex-column align-items-center flex-grow-0">
            <Button
              outline
              color="light"
              className={classNames(
                ' border-0 px-2 choice-correctness mt-1',
                addChoice && 'adding',
                remoteEdit
              )}
              disabled={!!addChoice || !editMode}
              onClick={doClickCorrect}
              onFocus={onFocus}
              onBlur={onBlur}
            >
              {choice[ChoiceCorrect] ? (
                <IoCheckmarkOutline
                  size="2rem"
                  className="text-success"
                />
              ) : (
                <IoCloseOutline
                  size="2rem"
                  className="text-danger"
                />
              )}
            </Button>
            {!addChoice && editMode && !trueFalse && (
              <Button
                size="sm"
                outline
                color="danger"
                className={classNames('mini-button p-2 d-flex choice-delete', remoteEdit)}
                title="Delete Distractor"
                style={{ marginTop: '1.15rem' }}
                onClick={() => deleteChoice(choice)}
                onFocus={onFocus}
                onBlur={onBlur}
              >
                <IoTrashOutline />
              </Button>
            )}
          </div>
          <div className="d-flex flex-column flex-grow-1">
            <PartEditor
              id={`choice-${index}`}
              concurrent="choices"
              readOnly={trueFalse || readOnly}
              className="mb-0"
              placeholder={addChoice ? 'Add new choice' : 'Choice text'}
              asset={question}
              part={choice[ChoiceComplexText]}
              onChange={editText}
              noMinHeight
            />
            {choice[ChoiceCorrect] && !addChoice && (
              <PartEditor
                id={`choice-${index}-feedback`}
                concurrent="choices"
                sometimes
                className="text-success"
                placeholder="Correct choice feedback"
                asset={question}
                part={choice[ChoiceCorrectFeedback]}
                onChange={editCorrectFeedback}
                readOnly={readOnly}
                noMinHeight
              />
            )}
            {!choice[ChoiceCorrect] && !addChoice && (
              <PartEditor
                id={`choice-${index}-feedback`}
                concurrent="choices"
                sometimes
                className="text-danger"
                placeholder="Incorrect choice feedback"
                asset={question}
                part={choice[ChoiceIncorrectFeedback]}
                onChange={editIncorrectFeedback}
                readOnly={readOnly}
                noMinHeight
              />
            )}
          </div>
        </div>
      )}
    </FocusLoser>
  );
};
