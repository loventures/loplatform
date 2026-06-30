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
import { formatInTimeZone } from 'date-fns-tz';
import moment, { Moment } from 'moment-timezone';
import Polyglot from 'node-polyglot';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { Col, FormFeedback, FormText, Input, Label } from 'reactstrap';

// date-fns (react-datepicker's formatter) has no token for the timezone
// abbreviation (EDT/EST); date-fns-tz's `zzz` does. We render the input value
// ourselves via a customInput so the suffix shows, like the old react-datetime.
const DisplayFormat = 'MMM d, yyyy h:mm aa zzz';

interface AdminFormDateTimeProps {
  value?: string;
  entity?: string;
  field?: string;
  inputName?: string;
  invalid?: string;
  placeholder?: string;
  help?: string;
  label?: string;
  required?: boolean;
  autoFocus?: boolean;
  disabled?: boolean;
  onChange?: (date: Moment | string | null) => void;
  T: Polyglot;
}

// react-datepicker replaces react-datetime, which called the React-19-removed
// findDOMNode. It renders/parses in the browser's local timezone (matching the
// app's moment.tz.guess() display). The hidden input carries the ISO value for
// form serialization; the month/year selects keep calendar navigation E2E-able.
const AdminFormDateTime: React.FC<AdminFormDateTimeProps> = ({
  value,
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
  onChange,
  T,
}) => {
  const [date, setLocalDate] = useState<Moment | null>(value ? moment(value) : null);

  useEffect(() => {
    if (value) {
      setLocalDate(moment(value));
    }
  }, [value]);

  const setDate = (d: Date | null) => {
    const m = d ? moment(d) : null;
    setLocalDate(m);
    onChange && onChange(m);
  };

  // Render react-datepicker's input ourselves so the value carries the timezone
  // abbreviation (date-fns-tz). The component is stable (so it doesn't remount
  // mid-interaction) and reads the latest date through a ref.
  const tz = moment.tz.guess();
  const dateRef = useRef(date);
  dateRef.current = date;
  const TzDateInput = useMemo(
    () =>
      React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
        function TzDateInput({ value: _value, ...rest }, ref) {
          const d = dateRef.current;
          return (
            <input
              {...rest}
              ref={ref}
              readOnly
              value={d ? formatInTimeZone(d.toDate(), tz, DisplayFormat) : ''}
            />
          );
        }
      ),
    [tz]
  );

  const id = `${entity}-${field}`;
  const encoded = date ? date.toISOString() : '';
  return (
    <div className={classNames('row', 'mb-3', { 'is-required': required })}>
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
        <DatePicker
          id={id}
          showIcon
          toggleCalendarOnIconClick
          icon={
            <i
              className="material-icons md-18"
              aria-hidden="true"
            >
              calendar_month
            </i>
          }
          selected={date ? date.toDate() : null}
          onChange={setDate}
          // showTimeInput (not showTimeSelect) so picking a day closes the popup
          // — react-datepicker keeps it open with showTimeSelect, which overlaps
          // adjacent date fields. Time stays editable via the time input.
          showTimeInput
          dateFormat="MMM d, yyyy h:mm aa"
          showMonthDropdown
          showYearDropdown
          dropdownMode="select"
          yearDropdownItemNumber={15}
          scrollableYearDropdown
          disabled={disabled}
          autoFocus={autoFocus}
          autoComplete="off"
          placeholderText={placeholder}
          required={required}
          wrapperClassName="d-block"
          customInput={
            <TzDateInput
              className={classNames('form-control admin-form-date-time', {
                'is-invalid': invalid,
              })}
            />
          }
        />
        {date && !required && !disabled && (
          <div
            id={`${id}-clear`}
            onClick={() => setDate(null)}
            className="clear-date-time material-icons"
          >
            close
          </div>
        )}
        {invalid && <FormFeedback style={{ display: 'block' }}>{invalid}</FormFeedback>}
        {help && <FormText>{help}</FormText>}
      </Col>
    </div>
  );
};

export default AdminFormDateTime;
