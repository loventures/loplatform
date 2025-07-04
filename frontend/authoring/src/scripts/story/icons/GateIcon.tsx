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

export const GateIcon: React.FC<{ size?: string; className?: string }> = ({
  size = '1em',
  className,
}) => (
  <svg
    stroke="currentColor"
    fill="currentColor"
    strokeWidth="0"
    viewBox="0 64 512 384"
    height="1em"
    width={size}
    className={className}
    xmlns="http://www.w3.org/2000/svg"
  >
    <path d="M 52.906712,148.48 V 363.51996 L 459.09329,363.6527 V 148.48 Z m 23.893328,23.89334 h 319.63963 l -79.64442,39.8222 H 76.80004 Z m 358.39992,7.43348 v 32.38872 H 370.42249 Z M 76.80004,236.08888 h 192.20855 l -79.64442,39.8222 H 76.80004 Z m 245.83579,0 h 112.56413 v 39.8222 H 242.99141 Z M 76.80004,299.80442 h 64.77747 l -64.77747,32.38874 z m 118.40471,0 h 239.99521 v 39.95496 l -319.63963,-0.13274 z" />
  </svg>
);
