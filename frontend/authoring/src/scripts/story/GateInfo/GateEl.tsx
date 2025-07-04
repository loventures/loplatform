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
import { Link } from 'react-router-dom';

import { useEditedAssetContextPath, useEditedAssetTitle } from '../../graphEdit';
import { FakeEdge } from '../../graphEdit/graphEditReducer';
import { useBranchId } from '../../hooks';
import { NewEdge } from '../../types/edge';
import { editorUrl } from '../story';

export const GateEl: React.FC<{ gate: NewEdge | FakeEdge; last: boolean }> = ({ gate, last }) => {
  const branchId = useBranchId();
  const name = gate.sourceName;
  const title = useEditedAssetTitle(name);
  const contextPath = useEditedAssetContextPath(name);
  return contextPath ? (
    <span className="ms-2">
      {`${round(gate.data.performanceGate.threshold * 100)}% on `}
      <Link to={editorUrl('story', branchId, name, contextPath)}>{title}</Link>
      {last ? '.' : ','}
    </span>
  ) : null;
};
