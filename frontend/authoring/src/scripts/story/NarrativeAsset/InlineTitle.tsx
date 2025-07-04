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
import { MdOpenInFull } from 'react-icons/md';
import { Link } from 'react-router-dom';

import { useBranchId, usePolyglot } from '../../hooks';
import { NodeName, TypeId } from '../../types/asset';
import { editorUrl, storyTypeName } from '../story';

export const InlineTitle: React.FC<{
  name: NodeName;
  typeId: TypeId;
  contextPath: string;
  commit?: number;
}> = ({ name, typeId, contextPath, commit }) => {
  const polyglot = usePolyglot();
  const branchId = useBranchId();

  return (
    <div className="d-flex justify-content-center feedback-context">
      <Link
        className="drill-in"
        to={editorUrl('story', branchId, name, contextPath, { commit })}
        data-id="title"
      >
        {storyTypeName(polyglot, typeId)}
        <div
          className="d-inline-block"
          style={{ width: 0, whiteSpace: 'nowrap' }}
        >
          &nbsp;
          <MdOpenInFull />
        </div>
      </Link>
    </div>
  );
};
