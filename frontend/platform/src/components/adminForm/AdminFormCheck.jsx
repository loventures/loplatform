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

import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { Col, Input, FormGroup, Label } from 'reactstrap';

class AdminFormCheck extends React.Component {
  render() {
    const { addOn, disabled, entity, inputName, field, label, value, invalid, onChange, T } =
      this.props;
    const id = `${entity}-${field}`;
    return (
      <FormGroup
        check
        row
        className={classNames({ 'has-danger': invalid })}
      >
        <Col lg={{ size: 10, offset: 2 }}>
          <Input
            id={id}
            disabled={disabled}
            type="checkbox"
            name={inputName || field}
            defaultChecked={value}
            onChange={onChange}
          >
            {addOn}
          </Input>
          <Label
            id={`${id}-label`}
            for={id}
            check
          >
            {label || T.t(`adminPage.${entity}.fieldName.${field}`)}
          </Label>
        </Col>
      </FormGroup>
    );
  }
}

AdminFormCheck.propTypes = {
  entity: PropTypes.string,
  field: PropTypes.string,
  inputName: PropTypes.string,
  value: PropTypes.bool,
  disabled: PropTypes.bool,
  invalid: PropTypes.string,
  label: PropTypes.string,
  addOn: PropTypes.element,
  onChange: PropTypes.func,
  T: PropTypes.shape({
    t: PropTypes.func,
  }),
};

export default AdminFormCheck;
