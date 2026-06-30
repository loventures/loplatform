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
import React, { useState } from 'react';
import { ChromePicker } from 'react-color';
import { Col, FormGroup, Input, Label } from 'reactstrap';

interface AdminFormColorProps {
  entity?: string;
  field?: string;
  inputName?: string;
  value?: string;
  invalid?: string;
  onChange?: (color: string) => void;
  required?: boolean;
  T: Polyglot;
}

// Replaces the unmaintained rc-color-picker, which relied on the React-19-removed
// findDOMNode/unmountComponentAtNode. A hex text input drives the value (and is
// what E2E targets); the swatch toggles a react-color ChromePicker popover.
const AdminFormColor: React.FC<AdminFormColorProps> = ({
  entity,
  field,
  inputName,
  value,
  invalid,
  onChange,
  required,
  T,
}) => {
  const [color, setColor] = useState(value ?? '');
  const [open, setOpen] = useState(false);

  const updateColor = (c: string) => {
    setColor(c);
    onChange && onChange(c);
  };

  const id = `${entity}-${field}`;
  return (
    <FormGroup
      row
      className={classNames({ 'has-danger': invalid, 'is-required': required })}
    >
      <Label
        lg={2}
        for={id}
      >
        {T.t(`adminPage.${entity}.fieldName.${field}`)}
      </Label>
      <Col lg={10}>
        <div className="admin-form-color">
          <button
            type="button"
            className="admin-form-color-swatch"
            style={{ background: color || '#000000' }}
            onClick={() => setOpen(o => !o)}
          >
            {T.t('adminForm.color.choose')}
          </button>
          <Input
            id={id}
            name={inputName || field}
            className="admin-form-color-input"
            value={color}
            aria-required={required}
            autoComplete="off"
            onChange={e => updateColor(e.target.value)}
          />
          {open && (
            <>
              <div
                className="admin-form-color-backdrop"
                onClick={() => setOpen(false)}
              />
              <div className="admin-form-color-popover">
                <ChromePicker
                  color={color || '#000000'}
                  disableAlpha
                  onChange={c => updateColor(c.hex)}
                />
              </div>
            </>
          )}
        </div>
      </Col>
    </FormGroup>
  );
};

export default AdminFormColor;
