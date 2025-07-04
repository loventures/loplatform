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

// GiLightningArc split

export const LightningArc: React.FC<{ className?: string }> = ({ className }) => (
  <svg
    stroke="currentColor"
    fill="currentColor"
    strokeWidth="0"
    viewBox="0 0 512 512"
    height="1.75rem"
    width="1.75rem"
    xmlns="http://www.w3.org/2000/svg"
    className={className}
  >
    <path d="M192.063 20.375l-44.625 98.563-36.344-13.657 40.312 47.22 29.47-50.656 17.093 159.437 88.874-159.936 7.906 138.22 74.72-140.408 32.905 9.094-32.594-57.28-58.75 91.343L300.657 27.28 216.75 153.657l-24.688-133.28z"></path>
  </svg>
);
