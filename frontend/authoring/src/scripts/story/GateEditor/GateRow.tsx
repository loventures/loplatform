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

import { round } from 'lodash';
import React from 'react';
import { GiCheckMark } from 'react-icons/gi';
import { IoClose } from 'react-icons/io5';
import NumericInput from 'react-numeric-input2';
import { Link } from 'react-router-dom';
import { Button } from 'reactstrap';

import {
  useEditedAssetContextPath,
  useEditedAssetTitle,
  useEditedAssetTypeId,
} from '../../graphEdit';
import { FakeEdge } from '../../graphEdit/graphEditReducer';
import { useBranchId } from '../../hooks';
import { NewEdge } from '../../types/edge';
import { getIcon } from '../AddAsset';
import { GateIcon } from '../icons/GateIcon';
import { editorUrl } from '../story';

export const GateRow: React.FC<{
  gate: NewEdge | FakeEdge;
  editMode: boolean;
  onDeleteGate: () => void;
  onEditGate: (threshold: number) => void;
  onBlur: () => void;
}> = ({ gate, editMode, onDeleteGate, onEditGate, onBlur }) => {
  const branchId = useBranchId();
  const name = gate.targetName;
  const typeId = useEditedAssetTypeId(gate.targetName);
  const title = useEditedAssetTitle(gate.targetName);
  const contextPath = useEditedAssetContextPath(gate.targetName);

  const Icon = getIcon(typeId);
  const value = round(gate.data.performanceGate.threshold * 100);
  return !contextPath ? null : editMode ? (
    <div className="d-flex align-items-center gate-row">
      <div className="gate-delete flex-shrink-0">
        <Icon className="gate" />
        <Button
          size="sm"
          color="danger"
          outline
          className="p-1 d-flex align-items-center justify-content-center mini-button delete"
          onClick={() => onDeleteGate()}
        >
          <IoClose />
        </Button>
      </div>
      <div className="flex-grow-1 minw-0 d-flex">
        <Link
          className="input-padding ps-2 text-truncate"
          to={editorUrl('story', branchId, name, contextPath)}
        >
          {title}
        </Link>
      </div>
      <div
        className="flex-shrink-0 d-flex align-items-center"
        title={gate.group === 'testsOut' ? 'Tests Out' : 'Gates'}
      >
        {gate.group === 'testsOut' ? <GiCheckMark size="1rem" /> : <GateIcon size="1.4rem" />}
      </div>
      <NumericInput
        step={1}
        min={0}
        max={100}
        value={value}
        onChange={v => {
          if (v !== value) onEditGate(v / 100);
        }}
        format={n => `${parseInt(n)}%`}
        className="form-control secret-input"
        onBlur={onBlur}
      />
    </div>
  ) : (
    <div className="d-flex align-items-center">
      <Icon className="gate flex-shrink-0" />
      <Link
        className="input-padding ps-2 text-truncate flex-grow-1"
        to={editorUrl('story', branchId, name, contextPath)}
      >
        {title}
      </Link>
      {gate.group === 'testsOut' ? (
        <GiCheckMark
          size="1rem"
          className="flex-shrink-0"
        />
      ) : (
        <GateIcon
          size="1.4rem"
          className="flex-shrink-0"
        />
      )}
      <span className="flex-shrink-0 ms-1">{value}%</span>
    </div>
  );
};
