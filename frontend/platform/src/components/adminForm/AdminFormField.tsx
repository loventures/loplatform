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
import {
  Col,
  FormFeedback,
  FormGroup,
  FormText,
  Input,
  InputGroup,
  InputGroupText,
  Label,
} from 'reactstrap';
import { InputType } from 'reactstrap/types/lib/Input';

interface AdminFormFieldProps {
  addOn?: React.ReactNode;
  type?: InputType;
  defaultValue?: string;
  entity?: string;
  inputName?: string;
  inputRef?: (el: HTMLInputElement | null) => void;
  field?: string;
  autoFocus?: boolean;
  readOnly?: boolean;
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void;
  help?: React.ReactNode;
  value?: string;
  invalid?: string;
  required?: boolean;
  label?: string;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  T: Polyglot;
  disabled?: boolean;
  labelClassName?: string;
  labelColSize?: number;
  inputColSize?: number;
  title?: string;
}

const AdminFormField: React.FC<AdminFormFieldProps> = ({
  addOn,
  type,
  defaultValue,
  entity,
  inputName,
  inputRef = () => null,
  field,
  autoFocus,
  readOnly,
  onBlur,
  help,
  value,
  invalid,
  required,
  label,
  onChange,
  T,
  disabled,
  labelClassName,
  labelColSize,
  inputColSize,
  title,
}) => {
  const id = `${entity}-${field}`;
  const validProp = invalid ? { invalid: true } : {};
  return (
    <FormGroup
      row
      className={classNames({ 'has-danger': invalid, 'is-required': required })}
    >
      <Label
        id={id + '-label'}
        className={labelClassName}
        lg={labelColSize || 2}
        for={id}
        title={title}
      >
        {label || T.t(`adminPage.${entity}.fieldName.${field}`)}
      </Label>
      <Col
        lg={inputColSize || 10}
        className="d-flex flex-column"
      >
        <InputGroup>
          <Input
            disabled={disabled}
            onChange={onChange}
            onBlur={onBlur}
            {...validProp}
            type={type || 'text'}
            id={id}
            name={inputName || field}
            defaultValue={defaultValue || value}
            innerRef={inputRef}
            autoFocus={autoFocus}
            readOnly={readOnly}
            required={false /* do not enable me, i then behave aberrantly and fail tests */}
            aria-required={required}
          />
          {addOn &&
            (React.isValidElement(addOn) ? addOn : <InputGroupText>{addOn}</InputGroupText>)}
        </InputGroup>
        {invalid && (
          <FormFeedback
            style={{ display: 'block' }}
            id={id + '-problem'}
          >
            {invalid}
          </FormFeedback>
        )}
        {help && React.isValidElement(help) ? help : <FormText id={id + '-help'}>{help}</FormText>}
      </Col>
    </FormGroup>
  );
};

export default AdminFormField;
