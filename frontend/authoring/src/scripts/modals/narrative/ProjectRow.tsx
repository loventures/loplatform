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
import * as React from 'react';
import { GoTriangleRight } from 'react-icons/go';

import { ProjectResponse } from '../../story/NarrativeMultiverse';

const ProjectRow: React.FC<{
  project: ProjectResponse;
  onClick: () => void;
}> = ({ project, onClick }) => {
  return (
    <div
      className={classNames(
        'story-index-item',
        'project-content-item',
        'd-flex',
        'align-items-stretch',
        'depth-1',
        'pointer'
      )}
      onClick={onClick}
    >
      <div className="a flex-shrink-1 text-truncate">{project.project.name}</div>
      <div className="flex-shrink-0 text-muted d-flex align-items-center go-arrow pe-1 ps-2">
        <GoTriangleRight
          size="1.5rem"
          style={{ transform: 'scaleY(.75)' }}
        />
      </div>
    </div>
  );
};

export default ProjectRow;
