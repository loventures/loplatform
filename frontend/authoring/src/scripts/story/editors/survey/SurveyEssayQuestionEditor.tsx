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
import { useDispatch } from 'react-redux';

import { beginProjectGraphEdit, editProjectGraphNodeData } from '../../../graphEdit';
import { HtmlPart } from '../../../types/asset';
import { PartEditor } from '../../PartEditor';
import { NarrativeEditor } from '../../story';
import { useNarrativeAssetState } from '../../storyHooks';

export const SurveyEssayQuestionEditor: NarrativeEditor<'surveyEssayQuestion.1'> = ({
  asset: question,
  readOnly,
}) => {
  const dispatch = useDispatch();

  const { created } = useNarrativeAssetState(question);

  const editPrompt = useCallback(
    (prompt: HtmlPart, session: string) => {
      dispatch(beginProjectGraphEdit('Question prompt', session));
      dispatch(
        editProjectGraphNodeData(question.name, {
          prompt,
        })
      );
    },
    [dispatch]
  );

  return (
    <div className="mb-3">
      <PartEditor
        id="prompt"
        placeholder="Essay prompt"
        asset={question}
        part={question.data.prompt}
        onChange={editPrompt}
        autoedit={created}
        compact
        readOnly={readOnly}
      />
    </div>
  );
};
