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

import React from 'react';
import { IoTrashOutline } from 'react-icons/io5';
import NumericInput from 'react-numeric-input2';
import { useDispatch } from 'react-redux';
import Textarea from '../../../react-textarea-autosize';
import { Button, Input } from 'reactstrap';

import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
} from '../../../graphEdit';
import { RubricCriterionLevel } from '../../../types/asset';
import { NewAssetWithEdge } from '../../../types/edge';
import { plural } from '../../story';

export const CriterionLevel: React.FC<{
  criterion: NewAssetWithEdge<'rubricCriterion.1'>;
  critdex: number;
  level: RubricCriterionLevel;
  index: number;
  editable: boolean;
}> = ({ criterion, critdex, level, index, editable }) => {
  const dispatch = useDispatch();

  const onEditLevelName = (index: number, name: string) => {
    dispatch(beginProjectGraphEdit('Edit level name', `crit:name:${criterion.name}:${index}`));
    dispatch(
      editProjectGraphNodeData(criterion.name, {
        levels: criterion.data.levels.map((l, i) => (i === index ? { ...l, name } : l)),
      })
    );
    // TODO: BAD BLUR
  };

  const onEditLevelDescription = (index: number, description: string) => {
    dispatch(
      beginProjectGraphEdit('Edit level description', `crit:desc:${criterion.name}:${index}`)
    );
    dispatch(
      editProjectGraphNodeData(criterion.name, {
        levels: criterion.data.levels.map((l, i) => (i === index ? { ...l, description } : l)),
      })
    );
    dispatch(autoSaveProjectGraphEdits()); // TODO: BAD BLUR
  };

  const onEditLevelPoints = (index: number, points: number) => {
    dispatch(beginProjectGraphEdit('Edit level points', `crit:points:${criterion.name}:${index}`));
    dispatch(
      editProjectGraphNodeData(criterion.name, {
        levels: criterion.data.levels.map((l, i) => (i === index ? { ...l, points } : l)),
      })
    );
    dispatch(autoSaveProjectGraphEdits()); // TODO: BAD BLUR
  };

  const onDeleteLevel = (index: number) => {
    dispatch(beginProjectGraphEdit('Delete criterion level'));
    const levels = [...criterion.data.levels];
    levels.splice(index, 1);
    dispatch(editProjectGraphNodeData(criterion.name, { levels }));
    dispatch(autoSaveProjectGraphEdits());
  };

  return (
    <td>
      <div className="d-flex flex-column h-100 criterion-level">
        {editable ? (
          <Input
            className="secret-input bg-transparent overflow-ellipsis"
            type="text"
            value={level.name}
            placeholder="Rating name..."
            onChange={e => onEditLevelName(index, e.target.value)}
          />
        ) : (
          <div
            className="input-padding"
            data-id={`criterion-${critdex}-level-${index}-name`}
          >
            {level.name}
          </div>
        )}
        {editable ? (
          <Textarea
            className="form-control secret-input text-small bg-transparent flex-grow-1"
            value={level.description}
            placeholder="Rating description..."
            onChange={e => onEditLevelDescription(index, e.target.value)}
          />
        ) : (
          <div
            className="input-padding text-small flex-grow-1"
            data-id={`criterion-${critdex}-level-${index}-description`}
          >
            {level.description}
          </div>
        )}
        <div className="d-flex justify-content-between align-items-center hide-nudge-arrows">
          {editable ? (
            <NumericInput
              value={level.points}
              format={n => plural(parseFloat(n), 'Point')}
              className="form-control secret-input point-editor"
              onChange={points => onEditLevelPoints(index, points)}
            />
          ) : (
            <div
              className="input-padding"
              data-id={`criterion-${critdex}-level-${index}-points`}
            >
              {plural(level.points, 'Point')}
            </div>
          )}
          {editable && (
            <Button
              size="sm"
              outline
              color="danger"
              className="mini-button p-2 d-flex delete-level me-1"
              title="Delete Rating"
              onClick={() => onDeleteLevel(index)}
            >
              <IoTrashOutline />
            </Button>
          )}
        </div>
      </div>
    </td>
  );
};
