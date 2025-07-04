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
import { IconType } from 'react-icons';

// BiChevronRight embiggened
export const ChevronPlain: IconType = ({ size = '1em', className, style }) => (
  <svg
    stroke="currentColor"
    fill="currentColor"
    strokeWidth={0}
    height={size}
    width={size}
    className={className}
    style={style}
    viewBox="0 0 24 24"
    version="1.1"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path d="M 8.6557426,6.6987148 13.807343,12 8.6557426,17.301285 10.202858,18.8484 17.051258,12 10.202858,5.1516 Z" />
  </svg>
);
