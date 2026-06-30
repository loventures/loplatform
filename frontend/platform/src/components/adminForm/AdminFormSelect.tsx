/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import classNames from 'classnames';
import Polyglot from 'node-polyglot';
import React from 'react';
import { Col, FormFeedback, FormGroup, Input, Label } from 'reactstrap';

interface SelectOption {
  id: string | number;
  text?: string;
  name?: string;
}

interface AdminFormSelectProps {
  disabled?: boolean;
  entity?: string;
  field?: string;
  label?: string;
  addOn?: React.ReactElement<any>;
  inputName?: string;
  invalid?: string;
  required?: boolean;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  options: SelectOption[];
  value?: string;
  T: Polyglot;
}

const AdminFormSelect: React.FC<AdminFormSelectProps> = ({
  addOn,
  disabled,
  entity,
  field,
  inputName,
  label,
  value,
  invalid,
  required,
  onChange,
  options,
  T,
}) => {
  const id = `${entity}-${field}`;
  const validProp = invalid ? { invalid: true } : {};
  return (
    <FormGroup
      row
      className={classNames({ 'is-required': required })}
    >
      <Label
        lg={2}
        for={id}
        id={`${id}-label`}
      >
        {label || T.t(`adminPage.${entity}.fieldName.${field}`)}
      </Label>
      <Col
        lg={10}
        className="d-flex"
      >
        <Input
          {...validProp}
          type="select"
          name={inputName || field}
          id={id}
          defaultValue={value}
          onChange={onChange}
          disabled={disabled}
          aria-required={required}
        >
          {options.map(o => (
            <option
              key={o.id}
              value={o.id}
            >
              {o.text || o.name}
            </option>
          ))}
        </Input>
        {addOn}
        {invalid && <FormFeedback>{invalid}</FormFeedback>}
      </Col>
    </FormGroup>
  );
};

export default AdminFormSelect;
