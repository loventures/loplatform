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

class AdminFormField extends React.Component {
  render() {
    const {
      addOn,
      type,
      defaultValue,
      entity,
      inputName,
      inputRef,
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
    } = this.props;
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
          {help && React.isValidElement(help) ? (
            help
          ) : (
            <FormText id={id + '-help'}>{help}</FormText>
          )}
        </Col>
      </FormGroup>
    );
  }
}

AdminFormField.propTypes = {
  autoFocus: PropTypes.bool,
  entity: PropTypes.string,
  field: PropTypes.string,
  inputColSize: PropTypes.number,
  inputName: PropTypes.string,
  inputRef: PropTypes.func,
  type: PropTypes.string,
  value: PropTypes.string,
  invalid: PropTypes.string,
  help: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
  addon: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
  required: PropTypes.bool,
  readOnly: PropTypes.bool,
  label: PropTypes.string,
  labelClassName: PropTypes.string,
  labelColSize: PropTypes.number,
  onChange: PropTypes.func,
  onBlur: PropTypes.func,
  T: PropTypes.shape({
    t: PropTypes.func,
  }),
};

AdminFormField.defaultProps = {
  inputRef: () => null,
};

export default AdminFormField;
