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
import React from 'react';
import { FormText, Input, Label } from 'reactstrap';

const InputText = ({ T, id, onChange, type }) => {
  return (
    <div>
      <Input
        id={id}
        type={type || 'text'}
        placeholder={T.t(`adminPage.provision.field.${id}.placeholder`)}
        onChange={onChange}
      />
      <Label for={id}>
        <FormText> {T.t(`adminPage.provision.field.${id}.label`)} </FormText>
      </Label>
    </div>
  );
};

InputText.propTypes = {
  T: PropTypes.object.isRequired,
  id: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  type: PropTypes.string,
};

export default InputText;
