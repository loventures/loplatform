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

import { CustomisableContent } from '../../../api/customizationApi';
import { WithTranslate } from '../../../i18n/translationContext';
import React, { ChangeEvent } from 'react';
import { Input } from 'reactstrap';
import { withHandlers } from 'recompose';
import { Dispatch } from 'redux';

import { changePointsPossible } from '../contentEdits';
import { addEdit } from '../courseCustomizerReducer';
import { DebouncedEdit } from './DebouncedEdit';

type PointsEditorProps = PointsEditorOuterProps & {
  updatePoints: (n: number) => void;
};
type PointsEditorOuterProps = {
  dispatch: Dispatch;
  content: CustomisableContent;
};

const mapper = (e: ChangeEvent<HTMLInputElement>) => Number.parseInt(e.target.value, 10);

const PointsEditorInner: React.FC<PointsEditorProps> = ({ content, updatePoints }) => (
  <WithTranslate>
    {translate => (
      <DebouncedEdit<ChangeEvent<HTMLInputElement>, number>
        state={content.pointsPossible || 0}
        onCommit={updatePoints}
        mapper={mapper}
      >
        {([points, updatePointsHandler]) => (
          <>
            <span className="d-flex align-items-center points-editor">
              {translate('POINTS_POSSIBLE')}
              <Input
                aria-label={translate('POINTS_POSSIBLE_FIELD', {
                  name: content.title,
                })}
                type="number"
                value={points}
                onChange={updatePointsHandler}
              />
            </span>
          </>
        )}
      </DebouncedEdit>
    )}
  </WithTranslate>
);

export const PointsEditor = withHandlers({
  updatePoints: (props: PointsEditorOuterProps) => (points: number) => {
    props.dispatch(
      addEdit(
        changePointsPossible({
          id: props.content.id,
          newPointsPossible: points,
        })
      )
    );
  },
})(PointsEditorInner);
