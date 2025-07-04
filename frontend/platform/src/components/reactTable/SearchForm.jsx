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

import PropTypes from 'prop-types';
import React from 'react';
import { Col, Input, Row } from 'reactstrap';
import { DropdownItem, DropdownMenu, DropdownToggle, InputGroup, Dropdown } from 'reactstrap';

import WaitDotGif from '../WaitDotGif';

class SearchForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      dropdownOpen: false,
    };
  }

  toggle = () => this.setState(state => ({ dropdownOpen: !state.dropdownOpen }));

  toDropdownItem = (col, index) => {
    const { T, entity, setSearchField, setSearchValue } = this.props;
    const baseName = `adminPage.${entity}.fieldName`;
    const Component = col.DropdownItem || DropdownItem;
    const extraProps = col.DropdownItem
      ? {
          setSearchField: setSearchField,
          setSearchValue: setSearchValue,
          toggle: this.toggle,
        }
      : {};
    const props = {
      id: `crudTable-search-dropdown-${col.dataField}`,
      onClick: () => setSearchField(col.dataField),
      ...extraProps,
    };
    const child = col.DropdownItem ? null : T.t(`${baseName}.${col.dataField}`);
    return (
      <Component
        key={index}
        {...props}
      >
        {child}
      </Component>
    );
  };

  renderSearchColumns = () => {
    const {
      T,
      columns,
      defaultValue,
      entity,
      fetching,
      spinning,
      searchField,
      setSearchValue,
      onSearchChange,
    } = this.props;
    const searchColumns = columns.filter(col => col.searchable);
    if (!searchColumns.length) return null;
    const { dropdownOpen } = this.state;
    const baseName = `adminPage.${entity}.fieldName`;
    const filterColumns = columns.filter(col => col.filterable && col.filterOptions);
    const searchColumn = columns.filter(col => col.dataField === searchField);
    const searchOperator = (searchColumn.length && searchColumn[0].searchOperator) || 'co';
    const colMd = {
      size: filterColumns.length < 2 ? 9 : 6,
      offset: filterColumns.length ? 0 : 3,
    };
    const col = columns.find(c => c.dataField === searchField);
    const InputComponent = (col && col.SearchInputComponent) || Input;
    const dropdownToggle = () => searchColumns.length > 1 && this.toggle();
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
              {searchColumns.map(this.toDropdownItem)}
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
            innerRef={input => {
              this.searchInput = input;
            }}
            name="search"
            className="form-control input-wide"
            placeholder={T.t(`crudTable.searchPlaceholder.${searchOperator}`)}
            defaultValue={defaultValue}
            onChange={onSearchChange}
            onKeyUp={e => {
              if (e.key === 'Enter') setSearchValue(e.target.value);
            }}
          />
        </InputGroup>
      </Col>
    );
  };

  onFilterChange = (property, operator, prefilter) => (e, currentFilters) => {
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

  renderFilterInput = col => {
    const { customFilterOnChange, defaultFilters } = this.props;
    const FilterInput = col.FilterInput || Input;
    const baseProps = {
      id: `${col.dataField}-select`,
      type: 'select',
      disabled: !col.filterOptions.length,
      onChange: evt =>
        customFilterOnChange(
          evt,
          col.onFilterChange ||
            this.onFilterChange(col.filterProperty, col.filterOperator || 'eq', col.prefilter)
        ),
      defaultValue: defaultFilters[col.dataField] || '',
    };
    const props = col.FilterInput ? { baseProps: baseProps } : baseProps;
    return (
      <FilterInput {...props}>
        {!col.FilterComponent ? (
          <React.Fragment>
            <option value="">{col.baseFilter}</option>
            {col.filterOptions}
          </React.Fragment>
        ) : null}
      </FilterInput>
    );
  };

  renderFilterColumns = () => {
    const { columns, filterWidth } = this.props;
    const filterColumns = columns.filter(col => col.filterable && col.filterOptions);
    return filterColumns.map((col, i, a) => (
      <Col
        xs={12 / filterColumns.length}
        md={filterWidth}
        key={col.dataField}
        className={i >= a.length - 1 ? 'pe-md-2' : 'pe-2'}
      >
        {this.renderFilterInput(col)}
      </Col>
    ));
  };

  render() {
    const { columns, searchBarWidth } = this.props;
    const filterColumns = columns.filter(col => col.filterable && col.filterOptions);
    return (
      <Col
        xs={12}
        md={filterColumns.length ? 12 : searchBarWidth}
        lg={searchBarWidth}
      >
        <Row className="g-0">
          {this.renderFilterColumns()}
          {this.renderSearchColumns()}
        </Row>
      </Col>
    );
  }
}

SearchForm.propTypes = {
  T: PropTypes.object.isRequired,
  entity: PropTypes.string.isRequired,
  defaultValue: PropTypes.string,
  setSearchField: PropTypes.func.isRequired,
  setSearchValue: PropTypes.func.isRequired,
  onSearchChange: PropTypes.func.isRequired,
  columns: PropTypes.array.isRequired,
  fetching: PropTypes.bool.isRequired,
  spinning: PropTypes.bool.isRequired,
  searchField: PropTypes.string.isRequired,
  customFilterOnChange: PropTypes.func.isRequired,
  filterWidth: PropTypes.number.isRequired,
  searchBarWidth: PropTypes.number.isRequired,
  defaultFilters: PropTypes.object.isRequired,
};

export default SearchForm;
