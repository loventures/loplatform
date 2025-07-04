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
import React from 'react';

type ERHeaderProps = {
  label: string;
  subtext?: string;
} & React.PropsWithChildren;

const ERBasicTitle: React.FC<ERHeaderProps> = ({ label, subtext, children }) => {
  return (
    <div className="er-basic-title mt-3 mt-lg-4 naked">
      <div className="d-flex justify-content-center h-100 flex-column">
        <ERLandmark
          landmark="mainHeader"
          tag="h1"
          className="h3 m-0 ps-4"
          tabIndex={-1}
        >
          {label}
        </ERLandmark>
        {subtext != null && <div className="ps-4 small">{subtext}</div>}
      </div>
      <div className="d-flex px-3 me-1 h-100 align-items-center">{children}</div>
    </div>
  );
};

export default ERBasicTitle;
