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

import axios from 'axios';
import classNames from 'classnames';
import debounce from 'debounce-promise';
import PropTypes from 'prop-types';
import React from 'react';
import Select from 'react-select';
import AsyncSelect from 'react-select/async';
import { Col, FormFeedback, FormGroup, FormText, Input, InputGroup, Label } from 'reactstrap';

import encodeQuery from '../matrix.js';

// TODO: handle when the user types delete in the combo

const MaxResults = 32;
const DebounceInterval = // as per UX standards
  ((Math.atan(10) * 10 * 10) << (10 / 10)) + ((10 >> (10 / 10)) + Math.log10(10));

class AdminFormCombobox extends React.Component {
  constructor(props) {
    super(props);
    const { value, dataFormat } = props;
    this.state = {
      selection: value && { ...value, displayString: dataFormat(value) },
      menuOpen: false,
    };
  }

  UNSAFE_componentWillReceiveProps(props) {
    const { value, dataFormat } = props;
    this.setSelection(value && { ...value, displayString: dataFormat(value) });
  }

  setSelection = selection => this.setState({ selection });

  _standardLoadOptions = debounce(
    input => {
      const {
        targetEntity,
        matrixFilter,
        matrixOrder,
        matrixPrefilter: prefilter,
        dataFormat,
        processData,
        T,
      } = this.props;
      const filter = matrixFilter(input);
      const order = matrixOrder(input);
      const matrix = encodeQuery({ offset: 0, limit: MaxResults, filter, prefilter, order });
      if (!targetEntity) {
        return Promise.resolve([]);
      }
      return axios.get(`/api/v2/${targetEntity};${matrix}`).then(res => {
        const options = processData(res.data.objects, input).map(data => ({
          ...data,
          displayString: dataFormat(data),
        }));
        const more = { id: -1, displayString: T.t('adminForm.combobox.more'), isDisabled: true };
        return res.data.filterCount > res.data.count ? [...options, more] : options;
      });
    },
    DebounceInterval,
    { leading: true }
  );

  renderInput = () => {
    const {
      entity,
      inputName,
      field,
      help,
      invalid,
      loadOptions,
      readOnly,
      onChange,
      options,
      required,
      T,
      placeholder,
      disabled,
      multiSelect,
      autoFocus,
    } = this.props;
    const { selection } = this.state;
    const id = `${entity}-${field}`;
    const SelectComponent = options ? Select : AsyncSelect;
    const customStyles = {
      control: (provided, state) => ({
        ...provided,
        ...(state.isFocused
          ? {
              border: '1px solid #80bdff',
              outline: '0',
              boxShadow: '0 0 0 0.2rem rgba(0,123,255,.25)',
            }
          : {}),
        '&:hover': {},
      }),
      menu: provided => ({ ...provided, zIndex: 100, minWidth: '320px' }),
      placeholder: provided => ({
        ...provided,
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        width: '100%',
      }),
    };
    return (
      <React.Fragment>
        {readOnly ? (
          <React.Fragment>
            <Input
              id={id}
              value={selection ? selection.displayString : ''}
              readOnly
            />
            <Input
              type="hidden"
              name={field}
              value={selection ? '' + selection.id : ''}
            />
          </React.Fragment>
        ) : (
          <SelectComponent
            id={id}
            classNamePrefix="react-select"
            styles={customStyles}
            style={{ height: '38px' }} // Same as bootstrap Input
            name={inputName || field}
            value={selection}
            isClearable={!required}
            isMulti={multiSelect}
            getOptionValue={o => o.id}
            getOptionLabel={o => o.displayString}
            isOptionDisabled={o => o.isDisabled}
            loadingPlaceholder={T.t('adminForm.combobox.loadingPlaceholder')}
            placeholder={placeholder || T.t('adminForm.combobox.selectPlaceholder')}
            clearValueText={T.t('adminForm.combobox.clear')}
            loadOptions={loadOptions || this._standardLoadOptions}
            defaultOptions
            options={options}
            disabled={disabled}
            onChange={sel => {
              onChange && onChange(sel);
              this.setSelection(sel);
            }}
            onMenuOpen={() => this.setState({ menuOpen: true })}
            onMenuClose={() => this.setState({ menuOpen: false })}
            autoFocus={autoFocus}
          />
        )}
        {invalid && (
          <FormFeedback
            style={{ display: 'block' }}
            id={id + '-problem'}
          >
            {invalid}
          </FormFeedback>
        )}
        {help && <FormText>{help}</FormText>}
      </React.Fragment>
    );
  };

  render() {
    const { addon, entity, field, invalid, required, T, inputOnly, labelWidth } = this.props;
    const id = `${entity}-${field}`;
    const input = this.renderInput();
    if (inputOnly) return input;
    // Key down triggers the select menu to close
    const onKD = e => {
      if (e.which === 27) this.menuWasOpen = this.state.menuOpen;
    };
    // Key up triggers the modal to close
    const onKU = e => {
      // Trap Escape if it is just intended to close a select list
      // to not also close modal
      if (e.which === 27 && this.menuWasOpen) {
        e.preventDefault();
        e.stopPropagation();
      }
    };
    return (
      <FormGroup
        row
        className={classNames({ 'has-danger': invalid, 'is-required': required })}
        onKeyDown={onKD}
        onKeyUp={onKU}
      >
        <Label
          lg={labelWidth}
          for={id}
        >
          {T.t(`adminPage.${entity}.fieldName.${field}`)}
        </Label>
        <Col lg={12 - labelWidth}>
          {addon ? (
            <InputGroup>
              <div className="react-select-full-width">{input}</div>
              {addon}
            </InputGroup>
          ) : (
            input
          )}
        </Col>
      </FormGroup>
    );
  }
}

AdminFormCombobox.propTypes = {
  disabled: PropTypes.bool,
  autoFocus: PropTypes.bool,
  entity: PropTypes.string,
  field: PropTypes.string,
  targetEntity: PropTypes.string,
  inputName: PropTypes.string,
  invalid: PropTypes.string,
  inputOnly: PropTypes.bool,
  labelWidth: PropTypes.number.isRequired,
  loadOptions: PropTypes.func,
  value: PropTypes.object,
  help: PropTypes.string,
  required: PropTypes.bool,
  readOnly: PropTypes.bool,
  onChange: PropTypes.func,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      displayString: PropTypes.string.isRequired,
      id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    })
  ),
  matrixFilter: PropTypes.func,
  matrixPrefilter: PropTypes.array,
  matrixOrder: PropTypes.func,
  multiSelect: PropTypes.bool,
  processData: PropTypes.func,
  dataFormat: PropTypes.func,
  T: PropTypes.shape({
    t: PropTypes.func,
  }),
};

const defaultFilter = value =>
  [{ property: 'disabled', operator: 'eq', value: false }].concat(
    value === '' ? [] : { property: 'name', operator: 'ts', value }
  );

const defaultOrder = () => ({
  property: 'name',
  direction: 'asc',
});

AdminFormCombobox.defaultProps = {
  labelWidth: 2,
  matrixFilter: defaultFilter,
  matrixPrefilter: [],
  matrixOrder: defaultOrder,
  dataFormat: data => data.displayString || data.name,
  processData: data => data,
  multiSelect: false,
};

export default AdminFormCombobox;
