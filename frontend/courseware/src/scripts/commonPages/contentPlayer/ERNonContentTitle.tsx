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

import { ERLandmark } from '../../landmarks/ERLandmarkProvider';
import React, { useRef } from 'react';
import classnames from 'classnames';
import { useMagicalStuckness } from './ERContentTitle.tsx';
import ERSidebarButton from '../sideNav/ERSidebarButton.tsx';

type ERHeaderProps = {
  label: string;
};

const ERNonContentTitle: React.FC<ERHeaderProps> = ({ label }) => {
  const ref = useRef<HTMLDivElement | null>(null);
  const { stuck, minHeight, mediumScreen } = useMagicalStuckness(ref);

  return (
    <div
      className={classnames('er-content-title mb-4', stuck && 'stuck')}
      style={minHeight ? { minHeight: `${minHeight}px` } : undefined}
      ref={ref}
    >
      <div className="content-title">
        {!mediumScreen && (
          <>
            <div className="d-flex d-print-none content-spacer flex-grow-0 me-1">
              <ERSidebarButton header />
            </div>
            <div className="d-flex d-print-none content-actions flex-grow-0 flex-shrink-0 ms-1"></div>
          </>
        )}
        <div className="title-bits">
          <ERLandmark
            landmark="mainHeader"
            tag="h1"
            className={classnames('h3 activity-title pb-1', stuck ? 'pt-1' : 'pt-3')}
            tabIndex={-1}
          >
            {label}
          </ERLandmark>
        </div>
      </div>
    </div>
  );
};

export default ERNonContentTitle;
