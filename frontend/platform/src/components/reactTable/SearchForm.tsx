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

import Polyglot from 'node-polyglot';
import React, { useState } from 'react';
import { Col, Input, Row } from 'reactstrap';
import { DropdownItem, DropdownMenu, DropdownToggle, InputGroup, Dropdown } from 'reactstrap';

import WaitDotGif from '../WaitDotGif';

interface Column {
  dataField: string;
  searchable?: boolean;
  searchOperator?: string;
  searchLabel?: string;
  SearchInputComponent?: React.ComponentType<any>;
  filterable?: boolean;
  filterOptions?: React.ReactNode[];
  filterProperty?: string;
  filterOperator?: string;
  prefilter?: boolean;
  baseFilter?: React.ReactNode;
  DropdownItem?: React.ComponentType<any>;
  FilterInput?: React.ComponentType<any>;
  FilterComponent?: React.ComponentType<any>;
  onFilterChange?: (e: any, currentFilters: any[]) => any[];
  [key: string]: unknown;
}

interface SearchFormProps {
  T: Polyglot;
  entity: string;
  defaultValue?: string;
  setSearchField: (field: string) => void;
  setSearchValue: (value: string) => void;
  onSearchChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  columns: Column[];
  fetching: boolean;
  spinning: boolean;
  searchField: string;
  customFilterOnChange: (
    evt: React.ChangeEvent<HTMLInputElement>,
    onChange: (e: any, currentFilters: any[]) => any[]
  ) => void;
  filterWidth: number;
  searchBarWidth: number;
  defaultFilters: Record<string, string>;
}

const SearchForm: React.FC<SearchFormProps> = props => {
  const {
    T,
    entity,
    defaultValue,
    setSearchField,
    setSearchValue,
    onSearchChange,
    columns,
    fetching,
    spinning,
    searchField,
    customFilterOnChange,
    filterWidth,
    searchBarWidth,
    defaultFilters,
  } = props;

  const [dropdownOpen, setDropdownOpen] = useState(false);

  const toggle = () => setDropdownOpen(open => !open);

  const toDropdownItem = (col: Column, index: number) => {
    const baseName = `adminPage.${entity}.fieldName`;
    const Component: React.ComponentType<any> = col.DropdownItem || DropdownItem;
    const extraProps = col.DropdownItem
      ? {
          setSearchField: setSearchField,
          setSearchValue: setSearchValue,
          toggle: toggle,
        }
      : {};
    const itemProps = {
      id: `crudTable-search-dropdown-${col.dataField}`,
      onClick: () => setSearchField(col.dataField),
      ...extraProps,
    };
    const child = col.DropdownItem ? null : T.t(`${baseName}.${col.dataField}`);
    return (
      <Component
        key={index}
        {...itemProps}
      >
        {child}
      </Component>
    );
  };

  const renderSearchColumns = () => {
    const searchColumns = columns.filter(col => col.searchable);
    if (!searchColumns.length) return null;
    const baseName = `adminPage.${entity}.fieldName`;
    const filterColumns = columns.filter(col => col.filterable && col.filterOptions);
    const searchColumn = columns.filter(col => col.dataField === searchField);
    const searchOperator = (searchColumn.length && searchColumn[0].searchOperator) || 'co';
    const colMd = {
      size: filterColumns.length < 2 ? 9 : 6,
      offset: filterColumns.length ? 0 : 3,
    };
    const col = columns.find(c => c.dataField === searchField);
    const InputComponent: React.ComponentType<any> = (col && col.SearchInputComponent) || Input;
    const dropdownToggle = () => searchColumns.length > 1 && toggle();
    const searchLabel =
      col && col.searchLabel ? col.searchLabel : T.t(`${baseName}.${searchField}`);
    return (
      <Col
        xs={12}
        md={colMd}
      >
        <InputGroup>
          <Dropdown
            addonType="prepend"
            isOpen={dropdownOpen}
            toggle={dropdownToggle}
          >
            <DropdownToggle
              id="crudTable-searchBy"
              caret={searchColumns.length > 1}
            >
              {searchLabel}
            </DropdownToggle>
            <DropdownMenu id="crudTable-searchMenu">
              {searchColumns.map(toDropdownItem)}
            </DropdownMenu>
          </Dropdown>
          {(fetching || spinning) && (
            <WaitDotGif
              id="crudTable-searchSpinner"
              className="spinning"
              color="dark"
              size={16}
            />
          )}
          <InputComponent
            id="crudTable-search"
            name="search"
            className="form-control input-wide"
            placeholder={T.t(`crudTable.searchPlaceholder.${searchOperator}`)}
            defaultValue={defaultValue}
            onChange={onSearchChange}
            onKeyUp={(e: React.KeyboardEvent<HTMLInputElement>) => {
              if (e.key === 'Enter') setSearchValue((e.target as HTMLInputElement).value);
            }}
          />
        </InputGroup>
      </Col>
    );
  };

  const onFilterChange =
    (property: string, operator: string, prefilter?: boolean) =>
    (e: React.ChangeEvent<HTMLInputElement>, currentFilters: any[]) => {
      const filters = [...currentFilters];
      const index = filters.findIndex(filter => filter.property === property);
      if (e.target.value !== '') {
        filters[index < 0 ? filters.length : index] = {
          property,
          operator,
          value: e.target.value,
          prefilter,
        };
      } else if (index !== -1) {
        filters.splice(index, 1);
      }
      return filters;
    };

  const renderFilterInput = (col: Column) => {
    const FilterInput: React.ComponentType<any> = col.FilterInput || Input;
    const baseProps = {
      id: `${col.dataField}-select`,
      type: 'select',
      disabled: !col.filterOptions!.length,
      onChange: (evt: React.ChangeEvent<HTMLInputElement>) =>
        customFilterOnChange(
          evt,
          col.onFilterChange ||
            onFilterChange(col.filterProperty!, col.filterOperator || 'eq', col.prefilter)
        ),
      defaultValue: defaultFilters[col.dataField] || '',
    };
    const inputProps = col.FilterInput ? { baseProps: baseProps } : baseProps;
    return (
      <FilterInput {...inputProps}>
        {!col.FilterComponent ? (
          <React.Fragment>
            <option value="">{col.baseFilter}</option>
            {col.filterOptions}
          </React.Fragment>
        ) : null}
      </FilterInput>
    );
  };

  const renderFilterColumns = () => {
    const filterColumns = columns.filter(col => col.filterable && col.filterOptions);
    return filterColumns.map((col, i, a) => (
      <Col
        xs={12 / filterColumns.length}
        md={filterWidth}
        key={col.dataField}
        className={i >= a.length - 1 ? 'pe-md-2' : 'pe-2'}
      >
        {renderFilterInput(col)}
      </Col>
    ));
  };

  const filterColumns = columns.filter(col => col.filterable && col.filterOptions);
  return (
    <Col
      xs={12}
      md={filterColumns.length ? 12 : searchBarWidth}
      lg={searchBarWidth}
    >
      <Row className="g-0">
        {renderFilterColumns()}
        {renderSearchColumns()}
      </Row>
    </Col>
  );
};

export default SearchForm;
