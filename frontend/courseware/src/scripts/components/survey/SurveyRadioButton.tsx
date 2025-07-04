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

type SurveyRadioButtonProps = {
  name: string;
  value: number | string;
  selected: string;
  setSelected: (r: string) => void;
  disabled: boolean;
} & React.PropsWithChildren;

const SurveyRadioButton: React.FC<SurveyRadioButtonProps> = ({
  name,
  value,
  selected,
  setSelected,
  disabled,
  children,
}) => {
  return (
    <div className="form-check">
      <label className="form-check-label">
        {children}
        <input
          type="radio"
          className="form-check-input"
          name={name}
          id={name + value}
          value={value}
          checked={selected === `${value}`}
          disabled={disabled}
          onChange={e => setSelected(e.target.value)}
        />
      </label>
    </div>
  );
};

export default SurveyRadioButton;
