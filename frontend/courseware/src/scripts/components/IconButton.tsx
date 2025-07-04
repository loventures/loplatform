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

import React, { ButtonHTMLAttributes } from 'react';

const IconButton: React.FC<
  {
    icon: string;
    srOnly: string;
    color?: string;
  } & ButtonHTMLAttributes<any>
> = ({ className = '', icon, srOnly, color = 'primary', ...buttonAttributes }) => {
  return (
    <button
      className={`icon-btn icon-btn-${color} ${className}`}
      type="button"
      {...buttonAttributes}
    >
      <span
        className={'icon ' + icon}
        role="presentation"
      ></span>
      <span className="sr-only">{srOnly}</span>
    </button>
  );
};

export default IconButton;
