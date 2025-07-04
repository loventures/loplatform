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
import { IoTrashOutline } from 'react-icons/io5';
import { Button } from 'reactstrap';

import Textarea from '../../../react-textarea-autosize';
import { MatchTuple, NewAsset } from '../../../types/asset';
import { FocusLoser } from '../../components/FocusLoser';
import { DefinitionText, StoryQuestionTypes, TermText } from '../../questionUtil';
import { useEditSession } from '../../story';
import { useIsEditable } from '../../storyHooks';
import { autoSaveProjectGraphEdits } from '../../../graphEdit';
import { useDispatch } from 'react-redux';

export const MatchEditor: React.FC<{
  question: NewAsset<StoryQuestionTypes>;
  match: MatchTuple;
  editMatch: (match: MatchTuple, comment: string, session: string) => void;
  deleteMatch: (match: MatchTuple) => void;
  isAdd: boolean;
  readOnly: boolean;
}> = ({ question, match, editMatch, deleteMatch, isAdd, readOnly }) => {
  const dispatch = useDispatch();
  const editMode = useIsEditable(question.name) && !readOnly;
  const index = match.definition.index;
  const [dirty, setDirty] = useState(false);
  const termSession = useEditSession(editMode);
  const defnSession = useEditSession(editMode);

  const onFocusLost = useCallback(() => {
    if (dirty) {
      // if you delete all the content in a match and tab out, delete it.
      // if you just tab through and don't modify, don't delete it.
      if (!match.definition[DefinitionText].trim() && !match.term[TermText].trim()) {
        deleteMatch(match);
      }
      dispatch(autoSaveProjectGraphEdits()); // meh
    }
  }, [match, dirty, deleteMatch]);

  const editText = useCallback(
    (tpe: 'term' | 'definition', session: string, value: string) => {
      setDirty(true);
      const update: MatchTuple = {
        definition:
          tpe === 'definition'
            ? { ...match.definition, [DefinitionText]: value }
            : match.definition,
        term: tpe === 'term' ? { ...match.term, [TermText]: value } : match.term,
      };
      editMatch(update, isAdd ? 'Add term' : `Edit ${tpe} text`, session);
    },
    [match, editMatch, setDirty, isAdd]
  );

  const divRef = useRef<HTMLDivElement>();

  return (
    <FocusLoser
      divRef={divRef}
      focusLost={onFocusLost}
    >
      {onFocus => (
        <div
          ref={divRef}
          onFocus={onFocus}
          className={classNames(
            'd-flex align-items-start my-2 choice position-relative',
            editMode ? 'me-4 pe-2' : 'mx-4'
          )}
        >
          <div className="d-flex align-items-center flex-grow-1">
            {editMode && (
              <Button
                size="sm"
                outline
                color="danger"
                className={classNames(
                  'mini-button p-2 d-flex choice-delete me-1 flex-shrink-0',
                  isAdd && 'invisible'
                )}
                title="Delete Match"
                onClick={() => deleteMatch(match)}
                disabled={isAdd}
              >
                <IoTrashOutline />
              </Button>
            )}
            <Textarea
              data-id={`term-${index}`}
              className="form-control match-input flex-grow-1"
              placeholder={isAdd ? 'Add new term' : 'Term'}
              value={match.term[TermText]}
              readOnly={readOnly}
              onChange={e => editText('term', termSession, e.target.value)}
            />
            <div className={classNames('flex-shrink-0 match-spacer', isAdd && 'invisible')} />
            <Textarea
              data-id={`definition-${index}`}
              className={classNames('form-control match-input flex-grow-1', isAdd && 'invisible')}
              disabled={isAdd}
              placeholder="Definition"
              value={match.definition[DefinitionText]}
              readOnly={readOnly}
              onChange={e => editText('definition', defnSession, e.target.value)}
            />
          </div>
        </div>
      )}
    </FocusLoser>
  );
};
