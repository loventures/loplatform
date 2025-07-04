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
import moment from 'moment-timezone';
import PropTypes from 'prop-types';
import React from 'react';
import DateTime from 'react-datetime';
import onClickOutside from 'react-onclickoutside';
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

import { inCurrTimeZone } from '../../services/moment.js';

// This is totally not keyboard accessible. react-datetime is non-tabbable
// and i had to make the input box read only to make life not atrocious.

class AdminFormDateTime extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      date: props.value && moment(props.value),
      open: null,
    };
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    if (nextProps.value && nextProps.value !== this.props.value) {
      this.setState({ date: moment(nextProps.value) });
    }
  }

  setDate = date => {
    this.setState({ date: date ? moment(date) : null });
    this.props.onChange && this.props.onChange(date);
  };

  handleClickOutside = () => this.setState({ open: false });

  toggleOpen = open => this.setState({ open: this.state.open === open ? null : open });

  render() {
    const {
      disabled,
      entity,
      inputName,
      field,
      label,
      autoFocus,
      invalid,
      help,
      required,
      placeholder,
      T,
    } = this.props;
    const id = `${entity}-${field}`;
    const { date, open } = this.state;
    const encoded = date ? date.toISOString() : '';
    const dateTimeFormat = T.t('format.dateTime.full');
    const dateInput = T.t('format.date.input');
    const timeInput = T.t('format.time.input');
    const formatted = date ? inCurrTimeZone(date).format(dateTimeFormat) : '';
    return (
      <FormGroup
        row
        className={classNames({ 'is-required': required })}
      >
        <Label
          lg={2}
          for={id}
        >
          {label || T.t(`adminPage.${entity}.fieldName.${field}`)}
        </Label>
        <Col
          lg={10}
          style={{ position: 'relative' }}
        >
          <Input
            type="hidden"
            name={inputName || field}
            value={encoded}
          />
          <InputGroup>
            <Input
              disabled={disabled}
              type="text"
              id={id}
              autoFocus={autoFocus}
              autoComplete="off"
              onClick={() => this.toggleOpen(open ? null : 'days')}
              value={formatted}
              readOnly
              className={classNames({ 'admin-form-date-time': true, 'is-invalid': invalid })}
              placeholder={placeholder}
              aria-required={required}
            />
            {!disabled && [
              <InputGroupText
                id={`${id}-days`}
                key={`${id}-days`}
                disabled={disabled}
                onClick={() => this.toggleOpen('days')}
                className="clickable"
              >
                <span className="material-icons md-18">date_range</span>
              </InputGroupText>,
              <InputGroupText
                id={`${id}-time`}
                key={`${id}-time`}
                disabled={disabled}
                onClick={() => this.toggleOpen('time')}
                className="clickable"
              >
                <span className="material-icons md-18">access_time</span>
              </InputGroupText>,
            ]}
          </InputGroup>
          {date && !required && !disabled && (
            <div
              id={`${id}-clear`}
              onClick={() => this.setDate(null)}
              className="clear-date-time material-icons"
            >
              close
            </div>
          )}
          {open && (
            <DateTime
              key={open}
              value={date}
              viewMode={open}
              input={false}
              onChange={this.setDate}
              dateFormat={dateInput}
              timeFormat={timeInput}
              closeOnSelect
            />
          )}
          {invalid && <FormFeedback style={{ display: 'block' }}>{invalid}</FormFeedback>}
          {help && <FormText>{help}</FormText>}
        </Col>
      </FormGroup>
    );
  }
}

AdminFormDateTime.propTypes = {
  value: PropTypes.string,
  entity: PropTypes.string,
  field: PropTypes.string,
  inputName: PropTypes.string,
  invalid: PropTypes.string,
  placeholder: PropTypes.string,
  help: PropTypes.string,
  label: PropTypes.string,
  required: PropTypes.bool,
  autoFocus: PropTypes.bool,
  onChange: PropTypes.func,
  T: PropTypes.shape({
    t: PropTypes.func,
  }),
};

export default onClickOutside(AdminFormDateTime);
