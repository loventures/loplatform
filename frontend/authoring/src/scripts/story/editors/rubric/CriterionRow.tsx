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

import React, { useState } from 'react';
import { BiBullseye } from 'react-icons/bi';
import { IoAddOutline, IoTrashOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import Textarea from '../../../react-textarea-autosize';
import { Button, Input } from 'reactstrap';

import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  deleteProjectGraphEdge,
  editProjectGraphNodeData,
  useAllEditedOutEdges,
} from '../../../graphEdit';
import { NewAssetWithEdge } from '../../../types/edge';
import { AlignmentEditor } from '../../AlignmentEditor';
import { CriterionLevel } from './CriterionLevel';
import { maxPoints } from './util';

export const CriterionRow: React.FC<{
  criterion: NewAssetWithEdge<'rubricCriterion.1'>;
  index: number;
  maxLevels: number;
  editable: boolean;
  competent: boolean;
}> = ({ criterion, index, maxLevels, editable, competent }) => {
  const dispatch = useDispatch();
  const allEdges = useAllEditedOutEdges(criterion.name);
  const isAligned = allEdges.some(edge => edge.group === 'assesses');
  const [showAlignment, setShowAlignment] = useState(isAligned);
  const aligner = isAligned || showAlignment;

  const onDeleteCriterion = () => {
    dispatch(beginProjectGraphEdit('Delete criterion'));
    dispatch(deleteProjectGraphEdge(criterion.edge));
    dispatch(autoSaveProjectGraphEdits()); // TODO: BAD BLUR
  };

  const onEditCriterionTitle = (title: string) => {
    dispatch(beginProjectGraphEdit('Edit criterion title', `crit:title:${criterion.name}`));
    dispatch(editProjectGraphNodeData(criterion.name, { title: title || 'Untitled' }));
    dispatch(autoSaveProjectGraphEdits()); // TODO: BAD BLUR
  };

  const onEditCriterionDescription = (description: string) => {
    dispatch(beginProjectGraphEdit('Edit criterion description', `crit:desc:${criterion.name}`));
    dispatch(editProjectGraphNodeData(criterion.name, { description }));
    dispatch(autoSaveProjectGraphEdits()); // TODO: BAD BLUR
  };

  const onAddLevel = () => {
    dispatch(beginProjectGraphEdit('Add criterion level'));
    const levels = [...criterion.data.levels, { name: '', description: '', points: 0 }];
    dispatch(editProjectGraphNodeData(criterion.name, { levels }));
    dispatch(autoSaveProjectGraphEdits());
  };

  // TODO: the alignability of remote criteria is wrong but remote is moribund

  return (
    <>
      <tr className="criterion-row">
        <td rowSpan={aligner ? 2 : 1}>
          <div className="d-flex flex-column h-100">
            {editable ? (
              <Input
                className="secret-input bg-transparent overflow-ellipsis"
                type="text"
                value={criterion.data.title === 'Untitled' ? '' : criterion.data.title}
                placeholder="Criterion name..."
                onChange={e => onEditCriterionTitle(e.target.value)}
                readOnly={!editable}
              />
            ) : (
              <div
                className="input-padding"
                data-id={`criterion-${index}-title`}
              >
                {criterion.data.title}
              </div>
            )}
            {editable ? (
              <Textarea
                className="form-control secret-input text-small bg-transparent flex-grow-1"
                value={criterion.data.description}
                placeholder="Criterion description..."
                onChange={e => onEditCriterionDescription(e.target.value)}
                readOnly={!editable}
              />
            ) : (
              <div
                className="input-padding text-small flex-grow-1"
                data-id={`criterion-${index}-description`}
              >
                {criterion.data.description}
              </div>
            )}
            <div className="d-flex justify-content-between align-items-center">
              <div className="input-padding text-truncate">{`${maxPoints(criterion)} Points`}</div>
              <div className="d-flex">
                {competent && !aligner && editable && (
                  <Button
                    size="sm"
                    outline
                    color="primary"
                    className="mini-button p-2 d-flex align-criterion"
                    title="Add Alignment"
                    onClick={() => setShowAlignment(true)}
                  >
                    <BiBullseye />
                  </Button>
                )}
                {editable && (
                  <Button
                    size="sm"
                    outline
                    color="danger"
                    className="mini-button p-2 d-flex delete-criterion me-1"
                    title="Delete Criterion"
                    onClick={() => onDeleteCriterion()}
                  >
                    <IoTrashOutline />
                  </Button>
                )}
              </div>
            </div>
          </div>
        </td>
        {criterion.data.levels.map((level, levdex) => (
          <CriterionLevel
            key={levdex}
            criterion={criterion}
            critdex={index}
            level={level}
            index={levdex}
            editable={editable}
          />
        ))}
        {criterion.data.levels.length < maxLevels && (
          <td colSpan={maxLevels - criterion.data.levels.length}>&nbsp;</td>
        )}
        {editable && (
          <td>
            <Button
              size="sm"
              outline
              color="primary"
              className="mini-button p-2 d-flex add-level"
              title="Add Rating"
              onClick={() => onAddLevel()}
            >
              <IoAddOutline />
            </Button>
          </td>
        )}
      </tr>
      {aligner && (
        <tr>
          <td
            colSpan={maxLevels + (editable ? 1 : 0)}
            className="unbordered"
          >
            <AlignmentEditor
              name={criterion.name}
              typeId={criterion.typeId}
            />
          </td>
        </tr>
      )}
    </>
  );
};
