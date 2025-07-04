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

import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { Input } from 'reactstrap';

const Color = ({ T, id, defaultColor, updateFormData }) => {
  const [hex, setHex] = useState(defaultColor);

  const changeColor = color => {
    const c = color.length > 0 ? color : defaultColor;
    setHex(c);
    updateFormData(c);
  };

  return (
    <div className="color-block d-flex">
      <Input
        className="w-75"
        key={id}
        id={id}
        placeholder={T.t(`adminPage.provision.field.${id}.placeholder`)}
        onChange={ev => changeColor((ev.target.value || '').trim())}
      />
      <div
        id={`${id}-block`}
        className="w-25 color-preview"
        style={{ backgroundColor: hex }}
      />
    </div>
  );
};

Color.propTypes = {
  T: PropTypes.object.isRequired,
  id: PropTypes.string.isRequired,
  defaultColor: PropTypes.string.isRequired,
  updateFormData: PropTypes.func.isRequired,
};

export default Color;
