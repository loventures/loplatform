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

import axios from 'axios';
import classNames from 'classnames';
import debounce from 'debounce-promise';
import Polyglot from 'node-polyglot';
import React, { useEffect, useRef, useState } from 'react';
import Select from 'react-select';
import AsyncSelect from 'react-select/async';
import { Col, FormFeedback, FormGroup, FormText, Input, InputGroup, Label } from 'reactstrap';

import encodeQuery from '../matrix.js';

// TODO: handle when the user types delete in the combo

const MaxResults = 32;
const DebounceInterval = // as per UX standards
  ((Math.atan(10) * 10 * 10) << (10 / 10)) + ((10 >> (10 / 10)) + Math.log10(10));

interface ComboOption {
  id: string | number;
  displayString?: string;
  isDisabled?: boolean;
  name?: string;
  [key: string]: unknown;
}

type MatrixFilter = (value: string) => unknown;
type MatrixOrder = (value: string) => unknown;

interface AdminFormComboboxProps {
  disabled?: boolean;
  autoFocus?: boolean;
  entity?: string;
  field?: string;
  targetEntity?: string;
  inputName?: string;
  invalid?: string;
  inputOnly?: boolean;
  labelWidth?: number;
  loadOptions?: (input: string) => Promise<ComboOption[]>;
  value?: ComboOption | null;
  help?: string;
  required?: boolean;
  readOnly?: boolean;
  onChange?: (selection: any) => void;
  options?: ComboOption[];
  matrixFilter?: MatrixFilter;
  matrixPrefilter?: unknown[];
  matrixOrder?: MatrixOrder;
  multiSelect?: boolean;
  processData?: (data: any[], input: string) => any[];
  dataFormat?: (data: any) => string;
  addon?: React.ReactNode;
  placeholder?: string;
  T: Polyglot;
}

const defaultFilter: MatrixFilter = value =>
  [{ property: 'disabled', operator: 'eq', value: false }].concat(
    value === '' ? [] : ({ property: 'name', operator: 'ts', value } as any)
  );

const defaultOrder: MatrixOrder = () => ({
  property: 'name',
  direction: 'asc',
});

const AdminFormCombobox: React.FC<AdminFormComboboxProps> = props => {
  const {
    addon,
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
    multiSelect = false,
    autoFocus,
    inputOnly,
    labelWidth = 2,
    targetEntity,
    matrixFilter = defaultFilter,
    matrixPrefilter: prefilter = [],
    matrixOrder = defaultOrder,
    processData = (data: any[]) => data,
    dataFormat = (data: any) => data.displayString || data.name,
  } = props;

  const [selection, setSelection] = useState<ComboOption | null | undefined>(
    props.value && { ...props.value, displayString: dataFormat(props.value) }
  );
  const [menuOpen, setMenuOpen] = useState(false);
  const menuWasOpen = useRef(false);

  useEffect(() => {
    setSelection(props.value && { ...props.value, displayString: dataFormat(props.value) });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [props.value]);

  // The debounced loader is created once, but it must use the LATEST props each
  // call (the original class read `this.props` at call time). Capturing them in
  // the closure would use stale values — e.g. an outer searchBy toggled after
  // first render — and query the wrong field, yielding no results. So route the
  // actual work through a ref that is refreshed every render.
  const loadOptionsRef = useRef<(input: string) => Promise<any[]>>(() => Promise.resolve([]));
  loadOptionsRef.current = (input: string) => {
    const filter = matrixFilter(input);
    const order = matrixOrder(input);
    const matrix = encodeQuery({ offset: 0, limit: MaxResults, filter, prefilter, order });
    if (!targetEntity) {
      return Promise.resolve([]);
    }
    return axios.get(`/api/v2/${targetEntity};${matrix}`).then(res => {
      const opts = processData(res.data.objects, input).map((data: any) => ({
        ...data,
        displayString: dataFormat(data),
      }));
      const more = {
        id: -1,
        displayString: T.t('adminForm.combobox.more'),
        isDisabled: true,
      };
      return res.data.filterCount > res.data.count ? [...opts, more] : opts;
    });
  };

  const standardLoadOptions = useRef(
    debounce((input: string) => loadOptionsRef.current(input), DebounceInterval, { leading: true })
  ).current;

  const renderInput = () => {
    const id = `${entity}-${field}`;
    const SelectComponent: React.ComponentType<any> = options ? Select : AsyncSelect;
    const customStyles = {
      control: (provided: any, state: any) => ({
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
      menu: (provided: any) => ({ ...provided, zIndex: 100, minWidth: '320px' }),
      placeholder: (provided: any) => ({
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
            styles={customStyles as any}
            style={{ height: '38px' }} // Same as bootstrap Input
            name={inputName || field}
            value={selection}
            isClearable={!required}
            isMulti={multiSelect}
            getOptionValue={(o: ComboOption) => '' + o.id}
            getOptionLabel={(o: ComboOption) => o.displayString ?? ''}
            isOptionDisabled={(o: ComboOption) => !!o.isDisabled}
            loadingPlaceholder={T.t('adminForm.combobox.loadingPlaceholder')}
            placeholder={placeholder || T.t('adminForm.combobox.selectPlaceholder')}
            clearValueText={T.t('adminForm.combobox.clear')}
            loadOptions={loadOptions || standardLoadOptions}
            defaultOptions
            // For the async case the server already filters by the typed input, so
            // don't let react-select re-filter (and wrongly drop) the results.
            filterOption={options ? undefined : null}
            options={options}
            disabled={disabled}
            onChange={(sel: any) => {
              onChange && onChange(sel);
              setSelection(sel);
            }}
            onMenuOpen={() => setMenuOpen(true)}
            onMenuClose={() => setMenuOpen(false)}
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

  const id = `${entity}-${field}`;
  const input = renderInput();
  if (inputOnly) return <>{input}</>;
  // Key down triggers the select menu to close
  const onKD = (e: React.KeyboardEvent) => {
    if (e.which === 27) menuWasOpen.current = menuOpen;
  };
  // Key up triggers the modal to close
  const onKU = (e: React.KeyboardEvent) => {
    // Trap Escape if it is just intended to close a select list
    // to not also close modal
    if (e.which === 27 && menuWasOpen.current) {
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
};

export default AdminFormCombobox;
