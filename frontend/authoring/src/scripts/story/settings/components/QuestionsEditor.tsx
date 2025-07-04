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
import React, { useCallback, useMemo } from 'react';
import NumericInput from 'react-numeric-input2';
import { useDispatch } from 'react-redux';

import {
  beginProjectGraphEdit,
  editProjectGraphNodeData,
  useAllEditedOutEdges,
} from '../../../graphEdit';
import { NewAsset } from '../../../types/asset';
import { plural } from '../../story';
import { useFocusedRemoteEditor } from '../../storyHooks';

export const QuestionsEditor: React.FC<{
  asset: NewAsset<'poolAssessment.1'>;
  questions: number;
}> = ({ asset, questions }) => {
  const dispatch = useDispatch();
  const allEdges = useAllEditedOutEdges(asset.name);
  const edgeCount = useMemo(
    () => allEdges.filter(edge => edge.group === 'questions').length,
    [allEdges]
  );

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(
    asset.name,
    'numberOfQuestionsForAssessment'
  );

  const editQuestions = useCallback(
    (num: number) => {
      if (num === questions) return;
      dispatch(beginProjectGraphEdit('Edit questions', session));
      dispatch(
        editProjectGraphNodeData(asset.name, {
          numberOfQuestionsForAssessment: num,
          useAllQuestions: !num,
        })
      );
    },
    [questions, session]
  );

  return (
    <div style={remoteEditor}>
      <NumericInput
        step={1}
        min={0}
        value={!questions || isNaN(questions) ? 0 : questions}
        onChange={editQuestions}
        format={n => (!parseInt(n) ? 'Use All Questions' : plural(parseInt(n), 'Question'))}
        parse={s => ('Use All Questions'.startsWith(s) ? 0 : parseInt(s))}
        className={classNames(
          'form-control secret-input questions-editor',
          questions > edgeCount && 'is-invalid',
          remoteEditor && 'remote-edit'
        )}
        onFocus={onFocus}
        onBlur={onBlur}
      />
    </div>
  );
};
