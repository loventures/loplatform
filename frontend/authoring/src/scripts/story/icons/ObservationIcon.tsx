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

// IoClipboardOutline + IoEyeOutline
export const ObservationIcon: IconType = ({ size = '1em', className }) => (
  <svg
    stroke="currentColor"
    fill="currentColor"
    strokeWidth="0"
    viewBox="0 0 512 512"
    height={size}
    width={size}
    className={className}
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      fill="none"
      strokeLinejoin="round"
      strokeWidth="32"
      d="M336 64h32a48 48 0 0148 48v320a48 48 0 01-48 48H144a48 48 0 01-48-48V112a48 48 0 0148-48h32"
    />
    <rect
      width="160"
      height="64"
      x="176"
      y="32"
      fill="none"
      strokeLinejoin="round"
      strokeWidth="32"
      rx="26.13"
      ry="26.13"
    />
    <path
      fill="none"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="32"
      d="m 255.83428,184 c -38.97,0 -78.945,22.555 -110.415,67.665 a 8,8 0 0 0 -0.135,8.885 c 24.18,37.85 63.62,67.45 110.55,67.45 46.42,0 86.67,-29.69 110.895,-67.625 a 8.07,8.07 0 0 0 0,-8.735 c -24.28,-37.5 -64.825,-67.64 -110.895,-67.64 z"
    ></path>
    <circle
      cx="256"
      cy="256"
      r="32"
      fill="none"
      strokeMiterlimit="10"
      strokeWidth="24"
    />
  </svg>
);
