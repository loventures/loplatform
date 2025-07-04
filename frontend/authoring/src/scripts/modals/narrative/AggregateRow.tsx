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
import { Badge } from 'reactstrap';

import { useDcmSelector } from '../../hooks';
import { getIcon } from '../../story/AddAsset';
import { plural } from '../../story/story';
import { ProjectWithHitCount } from './types';
import { contrastColor } from '../../projects/ProjectList/ProjectRow';

const AggregateRow: React.FC<{
  project: ProjectWithHitCount;
  onClick: () => void;
}> = ({ project, onClick }) => {
  const CourseIcon = getIcon('course.1');
  const { code, name, productType, category, subCategory, liveVersion } = project.project;
  const projectStatuses = useDcmSelector(state => state.configuration.projectStatuses ?? {});
  const projectStatusColor = useDcmSelector(state => state.configuration.projectStatusColor ?? {});

  const status = Object.entries(projectStatuses ?? {}).find(([, v]) => v === liveVersion)?.[0];
  const color = projectStatusColor?.[status] ?? 'light';

  return (
    <div
      className={classNames(
        'project-row',
        'story-index-item',
        'add-content-item',
        'd-flex',
        'align-items-stretch',
        'mx-3',
        'depth-1',
        'pointer'
      )}
      onClick={onClick}
    >
      <div className="leader">
        <CourseIcon size="1.5rem" />
      </div>
      <div className="d-flex content flex-grow-1 flex-shrink-1 minw-0">
        <div className="flex-grow-1 flex-shrink-1 minw-0">
          <div className="a d-flex">
            {code && <span className="fw-bold me-2 project-code flex-shrink-0">{code}</span>}
            <div className="project-name text-truncate">{name}</div>
          </div>
          <div className="badges">
            {liveVersion && (
              <Badge
                color={color}
                className={classNames(
                  'project-live-version metadata-badge fw-normal',
                  contrastColor[color]
                )}
              >
                {liveVersion}
              </Badge>
            )}
            {productType && (
              <Badge
                color="light"
                className="project-product-type metadata-badge fw-normal text-dark"
                title="Product Type"
              >
                {productType}
              </Badge>
            )}
            {category && (
              <Badge
                color="light"
                className="project-category metadata-badge fw-normal text-dark"
                title="Category"
              >
                {category}
              </Badge>
            )}
            {subCategory && (
              <Badge
                color="light"
                className="project-sub-category metadata-badge fw-normal text-dark"
                title="Subcategory"
              >
                {subCategory}
              </Badge>
            )}
          </div>
        </div>
        {!!project.hits && (
          <div className="flex-shrink-0 hits px-2">{plural(project.hits, 'hit')}</div>
        )}
      </div>
    </div>
  );
};

export default AggregateRow;
