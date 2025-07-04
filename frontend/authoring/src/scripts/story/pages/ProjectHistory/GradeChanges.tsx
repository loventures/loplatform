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

import React, { Fragment } from 'react';
import { Link } from 'react-router-dom';

import { useBranchId, useDcmSelector } from '../../../hooks';
import { CreateLineItem, DeleteLineItem, UpdateLineItem } from './PublishAnalysis';

export type GradeChangePanelProps = {
  creates: CreateLineItem[];
  updates: UpdateLineItem[];
  deletes: DeleteLineItem[];
};

const GradeChangePanel: React.FC<GradeChangePanelProps> = ({ creates, updates, deletes }) => {
  const branchId = useBranchId();
  const project = useDcmSelector(state => state.layout.project);
  const csvUrl = `/api/v2/authoring/projects/${project.id}/publishAnalysis/detail.csv`;
  return (
    <div>
      <p>
        <span className="fw-bold align-text-bottom">LIS Result Line Item Changes</span> (
        <a href={csvUrl}>download detail</a>)
      </p>

      <ul>
        {creates.map(create => (
          <li key={create.name}>
            create <Link to={`/branch/${branchId}/launch/${create.name}`}>{create.title}</Link>
          </li>
        ))}
        {updates.map(update => {
          return (
            <Fragment key={update.name}>
              {update.prevPointsPossible == null ? null : (
                <li>
                  update{' '}
                  <Link to={`/branch/${branchId}/launch/${update.name}`}>{update.title}</Link>{' '}
                  <span className="fw-bold">Points Possible</span> from{' '}
                  <span className="fw-bold">{update.prevPointsPossible.toString()}</span> to{' '}
                  <span className="fw-bold">{update.pointsPossible.toString()}</span>
                </li>
              )}
              {update.prevIsForCredit == null ? null : (
                <li>
                  update{' '}
                  <Link to={`/branch/${branchId}/launch/${update.name}`}>{update.title}</Link>{' '}
                  <span className="fw-bold">Is For Credit</span> from{' '}
                  <span className="fw-bold">{update.prevIsForCredit.toString()}</span> to{' '}
                  <span className="fw-bold">{update.isForCredit.toString()}</span>
                </li>
              )}
            </Fragment>
          );
        })}
        {deletes.map(del => (
          <li key={del.name}>
            delete <Link to={`/branch/${branchId}/launch/${del.name}`}>{del.title}</Link>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default GradeChangePanel;
