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
import serialize from 'form-serialize';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { debounce } from 'throttle-debounce';

import LoPropTypes from '../../react/loPropTypes';
import encodeQuery from '../matrix.js';
import ButtonBar from './ButtonBar';
import ReactTableModal from './ReactTableModal';
import SearchForm from './SearchForm';
import Table from './Table';

const INITIAL_PAGE_SIZE = 17; // this will never be seen unless something breaks

export const getSavedTableState = (entity, attr, dflt) => {
  const stored = window.sessionStorage.getItem(`RT:${entity}:${attr}`);
  const numeric = typeof dflt === 'number';
  return !stored ? dflt : numeric ? parseInt(stored, 10) : stored;
};

class ReactTable extends React.Component {
  constructor(props) {
    super(props);
    const selectedRow = getSavedTableState(props.entity, 'selectedRow', 0);
    const savedColumns = props.columns.filter(column => column.filterProperty);
    const defaultFilters = // load up the saved filter values
      savedColumns.reduce(
        (o, { dataField }) => ({ ...o, [dataField]: getSavedTableState(props.entity, dataField) }),
        {}
      );
    const customFilters = // then apply the saved values to the initial matrix query
      savedColumns.reduce((filters, { dataField, filterProperty, filterOperator, prefilter }) => {
        const value = defaultFilters[dataField];
        return !value
          ? filters
          : [
              ...filters.filter(f => f.property !== filterProperty),
              { property: filterProperty, operator: filterOperator || 'eq', prefilter, value },
            ];
      }, props.customFilters);
    this.state = {
      loaded: false,
      fetching: false, // prevent concurrent fetches
      generation: 0, // allows state-based force reload
      currentData: [],
      totalSize: 0,
      autoPageSize: INITIAL_PAGE_SIZE,
      currentPage: getSavedTableState(props.entity, 'currentPage', 1),
      pageSize: getSavedTableState(props.entity, 'pageSize', INITIAL_PAGE_SIZE),
      searchField: getSavedTableState(props.entity, 'searchField', props.defaultSearchField),
      searchValue: getSavedTableState(props.entity, 'searchValue', ''),
      orderField: getSavedTableState(props.entity, 'orderField', props.defaultSortField),
      orderDir: getSavedTableState(props.entity, 'orderDir', props.defaultSortOrder),
      modalState: {
        info: null,
        error: null,
        type: props.initModal ? 'create' : null,
        submitting: false,
        validationErrors: {},
      },
      errorCount: 0, // to force new alerts to rerender
      spinning: false,
      selectedRows: selectedRow ? [{ id: selectedRow }] : [],
      stats: {},
      customFilters,
      defaultFilters,
    };
    this.setSearchDebounced = debounce(300, this.setSearchValue);
    this.computePageSizeDebounced = debounce(300, this.computeAutoPageSize);
  }

  getBaseUrl = () => this.props.baseUrl || `/api/v2/${this.props.entity}`;

  getPostUrl = () => this.props.postUrl || `/api/v2/${this.props.entity}`;

  UNSAFE_componentWillReceiveProps(nextProps) {
    if (nextProps.initModal) {
      // could be more flexible
      this.setModalState({ type: 'create' });
    }
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this.computePageSizeDebounced);
    this.props.setPortalAlertStatus(false, false, '');
  }

  componentDidMount() {
    window.addEventListener('resize', this.computePageSizeDebounced);
    this.computeAutoPageSize();
    this.props.refreshRef(this.refresh);
  }

  computeAutoPageSize = () => {
    let rows = 16;
    if (this.tableRowEl) {
      const tableBody = this.tableRowEl.getElementsByClassName('react-bs-container-body')[0];
      if (tableBody) {
        const firstRow = tableBody.getElementsByTagName('tr')[0];
        const pager = document.getElementById('page-size-col');
        if (firstRow && pager) {
          let offset = pager.offsetHeight;
          for (let current = tableBody; current; current = current.offsetParent) {
            offset += current.offsetTop;
          }
          rows = Math.max(
            Math.floor((window.innerHeight - offset - 15) / firstRow.offsetHeight) || 16,
            4
          );
        }
      }
    }
    this.setState(({ pageSize, autoPageSize }) => ({
      autoPageSize: rows,
      pageSize: pageSize === autoPageSize ? rows : pageSize,
    }));
  };

  setStored = (attr, value) =>
    window.sessionStorage.setItem(`RT:${this.props.entity}:${attr}`, value.toString());

  componentDidUpdate(prevProps, prevState) {
    if (
      !this.state.fetching &&
      (this.stateQuery(this.state) !== this.stateQuery(prevState) ||
        this.state.generation !== prevState.generation ||
        !this.state.loaded)
    ) {
      this.loadData();
    }
    ['currentPage', 'pageSize', 'searchField', 'searchValue', 'orderField', 'orderDir'].forEach(
      attr => this.setStored(attr, this.state[attr] || '')
    );
    const selectedRows = this.state.selectedRows;
    this.setStored('selectedRow', selectedRows.length === 1 ? selectedRows[0].id : '');
    this.props.columns
      .filter(column => column.filterProperty)
      .forEach(({ dataField, filterProperty }) => {
        const filter = this.state.customFilters.find(filter => filter.property === filterProperty);
        this.setStored(dataField, filter ? filter.value : '');
      });
  }

  loadData = () => {
    const { autoSelect, parseEntity, setPortalAlertStatus, translations: T, paginate } = this.props;
    // capture current state so we can refetch if the state has changed once we receive data
    const { generation, loaded, currentPage: page } = this.state;
    const matrixQuery = this.stateQuery(this.state);
    this.setState({ fetching: true }); // this is racy in that there could be pending state changes that happen before this, no?
    const matrix = matrixQuery ? `;${matrixQuery}` : '';
    axios
      .get(`${this.getBaseUrl()}${matrix}`, { hideProgress: true })
      .then(response => {
        const { generation: gen, selectedRows: sels } = this.state;
        const data = response.data.objects.map(parseEntity);
        const stale = generation !== gen || matrixQuery !== this.stateQuery(this.state); // have the query params changed
        const selectAll = autoSelect && !loaded;
        const reselect = data.filter(row => selectAll || sels.map(sel => sel.id).includes(row.id)); // deselect if the selection is no longer present
        this.setState({
          loaded: true,
          fetching: false,
          currentData: data,
          totalSize: !paginate ? 16 : response.data.filterCount,
          stats: response.data,
          spinning: false, // reset the search spinner
          selectedRows: reselect,
          currentPage: page - (page > 1 && !data.length ? 1 : 0), // if you delete the last row go back a page
          generation: this.state.generation + (stale ? 1 : 0), // if state has changed then force refetch
        });
      })
      .catch(error => {
        console.log(error);
        this.setState({ fetching: false, loaded: true });
        setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
  };

  getFilters = state => {
    if (state.customFilters.length || state.searchValue) {
      return {
        filter: state.customFilters.filter(filter => !filter.prefilter),
        prefilter: state.customFilters.filter(filter => filter.prefilter),
      };
    }
    return {};
  };

  stateQuery = state => {
    const { filter, prefilter } = this.getFilters(state);
    if (state.searchValue) {
      const col = this.props.columns.find(col => col.dataField === state.searchField);
      const operator = col.searchOperator ? col.searchOperator : 'co';
      filter.push({ property: state.searchField, operator, value: state.searchValue });
    }
    const query = {
      offset: (state.currentPage - 1) * state.pageSize,
      limit: state.pageSize,
      filter: filter && filter.length ? filter : null,
      prefilter: prefilter && prefilter.length ? prefilter : null,
    };
    if (state.orderField && state.orderDir) {
      const col = this.props.columns.find(col => col.dataField === state.orderField);
      const direction = !col?.nullsOpposite
        ? state.orderDir
        : state.orderDir === 'asc'
          ? 'ascNullsFirst'
          : 'descNullsLast';
      query.order = { property: state.orderField, direction };
    }
    if (this.props.paginate === false) {
      delete query.offset;
      delete query.limit;
    }
    if (this.props.embed) query.embed = this.props.embed;
    return encodeQuery(query);
  };

  refresh = () => this.setState({ generation: this.state.generation + 1 });

  renderButtonBar = () => {
    const { columns } = this.props;
    const filterColumns = columns.filter(col => col.filterable && col.filterOptions);
    return (
      <ButtonBar
        xs={12}
        md={filterColumns.length ? 12 : 12 - this.state.searchBarWidth}
        lg={12 - this.props.searchBarWidth}
        baseName={`adminPage.${this.props.entity}.toolBar`}
        createButton={this.props.createButton}
        T={this.props.translations}
        createDropdown={this.props.createDropdown}
        dropdownItems={this.props.dropdownItems}
        updateButton={this.props.updateButton}
        selectedRows={this.state.selectedRows}
        getButtons={this.props.getButtons}
        multiSelect={this.props.multiSelect}
        canDeleteRow={this.props.canDeleteRow}
        canUpdateRow={this.props.canUpdateRow}
        deleteButton={this.props.deleteButton}
        multiDelete={this.props.multiDelete}
        onJefreshClicked={this.refresh}
        fetching={this.state.fetching}
        loaded={this.state.loaded}
        lo_platform={this.props.lo_platform}
        refresh={this.refresh}
        showModal={this.showModal}
        onCreate={this.props.onCreate}
        renderForm={this.props.renderForm}
        filterColsLength={filterColumns.length}
      />
    );
  };

  customFilterOnChange = (evt, onChange) => {
    const customFilters = onChange(evt, this.state.customFilters);
    this.setState({ customFilters: customFilters });
  };

  renderSearchForm = () => {
    return (
      <SearchForm
        T={this.props.translations}
        entity={this.props.entity}
        setSearchField={this.setSearchField}
        setSearchValue={this.setSearchValue}
        defaultValue={this.state.searchValue}
        columns={this.props.columns}
        fetching={this.state.fetching}
        spinning={this.state.spinning}
        searchField={this.state.searchField}
        customFilterOnChange={this.customFilterOnChange}
        filterWidth={this.props.filterWidth}
        searchBarWidth={this.props.searchBarWidth}
        onSearchChange={this.onSearchChange}
        defaultFilters={this.state.defaultFilters}
      />
    );
  };

  setSearchField = field => {
    const { onSearchFieldChange } = this.props;
    onSearchFieldChange(field);
    this.setState({ currentPage: 1, searchField: field });
    this.searchInput && this.searchInput.focus();
  };

  onSearchChange = e => {
    const spinning = e.target.value !== this.state.searchValue;
    if (spinning !== this.state.spinning) {
      this.setState({ spinning });
    }
    this.setSearchDebounced(e.target.value);
  };

  setSearchValue = value => {
    if (value !== this.searchValue) {
      this.setState({ currentPage: 1, searchValue: value });
    }
  };

  renderTable = () => (
    <Table
      entity={this.props.entity}
      pageSize={this.state.pageSize}
      totalSize={this.state.totalSize}
      stats={this.state.stats}
      currentData={this.state.currentData}
      csvUrl={`${this.getBaseUrl()}.csv;${this.stateQuery(this.state)}`}
      setPageSize={this.setPageSize}
      columns={this.props.columns}
      T={this.props.translations}
      orderField={this.state.orderField}
      orderDir={this.state.orderDir}
      onSortChange={this.onSortChange}
      currentPage={this.state.currentPage}
      onPageChange={this.onPageChange}
      openRow={this.props.openRow}
      loaded={this.state.loaded}
      fetching={this.state.fetching}
      onRowSelect={this.onRowSelect}
      selectedRows={this.state.selectedRows}
      trClassFormat={this.props.trClassFormat}
      tdClassFormat={this.props.tdClassFormat}
      multiSelect={this.props.multiSelect}
    />
  );

  onSortChange = (sortName, sortOrder) => {
    this.setState({ orderField: sortName, orderDir: sortOrder });
  };

  onPageChange = (page, pageSize) => {
    this.setState({ currentPage: page, pageSize: pageSize });
  };

  setPageSize = pageSize => {
    const newPageSize = pageSize || this.state.autoPageSize;
    const newPage =
      1 + parseInt(((this.state.currentPage - 1) * this.state.pageSize) / newPageSize, 10);
    this.setState({ currentPage: newPage, pageSize: newPageSize });
  };

  onRowSelect = (row, isSelected, e) => {
    const { selectedRows } = this.state;
    const { multiSelect } = this.props;
    const ctrlKeyPressed = e.ctrlKey || e.metaKey;
    if (row && isSelected) {
      if (multiSelect && ctrlKeyPressed) {
        selectedRows.push(row);
      } else {
        selectedRows.length = 0;
        selectedRows.push(row);
      }
    } else {
      const index = selectedRows.map(elt => elt.id).indexOf(row.id);
      if (multiSelect && !ctrlKeyPressed) {
        selectedRows.length = 0;
        selectedRows.push(row);
      } else {
        index !== -1 && selectedRows.splice(index, 1);
      }
    }
    this.setState({ selectedRows: selectedRows });
  };

  setModalState = modalState => {
    const { type, info, error, submitting, validationErrors } = modalState;
    this.setState(state => ({
      modalState: {
        type: modalState.hasOwnProperty('type') ? type : state.modalState.type,
        submitting: modalState.hasOwnProperty('submitting')
          ? submitting
          : state.modalState.submitting,
        error: modalState.hasOwnProperty('error') ? error : state.modalState.error,
        info: modalState.hasOwnProperty('info') ? info : state.modalState.info,
        validationErrors: modalState.hasOwnProperty('validationErrors')
          ? validationErrors
          : state.modalState.validationErrors,
      },
    }));
  };

  renderModal = () => {
    const {
      entity,
      autoComplete,
      renderForm,
      translations: T,
      getModalTitle,
      footerExtra,
      headerExtra,
    } = this.props;
    const { modalState, selectedRows, errorCount } = this.state;
    return (
      modalState.type && (
        <ReactTableModal
          autoComplete={autoComplete}
          entity={entity}
          T={T}
          getModalTitle={getModalTitle}
          footerExtra={footerExtra}
          headerExtra={headerExtra}
          modalState={modalState}
          selectedRows={selectedRows}
          renderForm={renderForm}
          errorCount={errorCount}
          onModalSubmit={this.onModalSubmit}
          hideModal={this.hideModal}
        />
      )
    );
  };

  showModal = style => {
    if (style !== 'delete') {
      const { selectedRows } = this.state;
      const selectedRow = selectedRows && selectedRows.length === 1 && selectedRows[0];
      this.props.beforeCreateOrUpdate(style === 'update' ? selectedRow : {});
    }
    this.setModalState({
      info: null,
      error: null,
      submitting: false,
      type: style,
      validationErrors: {},
    });
  };

  hideModal = () => {
    this.props.onDismissModal();
    this.setModalState({ type: null });
  };

  onModalSubmit = e => {
    e.preventDefault();
    const { modalState } = this.state;
    switch (modalState.type) {
      case 'create':
        return this.submitRow(e.target, true);
      case 'update':
        return this.submitRow(e.target, false);
      case 'delete':
        return this.deleteRow();
      default:
        return;
    }
  };

  standardSubmitForm = ({ data, id, config, create }) => {
    const { updateUrl, updateMethod } = this.props;
    if (create) return axios.post(this.getPostUrl(), data, config);
    else {
      return axios({
        method: updateMethod || 'put',
        url: updateUrl || this.getBaseUrl() + '/' + id,
        data: data,
        ...config,
      });
    }
  };

  submitRow = (form, create) => {
    const { entity, schema, submitForm: customSubmitForm, translations: T } = this.props;
    const { selectedRows } = this.state;
    const selectedRow = selectedRows && selectedRows.length === 1 && selectedRows[0];
    const baseName = `adminPage.${entity}`;
    const dtoPromise = Promise.resolve(
      this.props.validateForm(serialize(form, { hash: true }), create ? {} : selectedRow, form)
    );
    dtoPromise.then(dto => {
      this.setState(state => ({ errorCount: 1 + state.errorCount }));
      if (dto.validationErrors) {
        const error = dto.error || T.t(`${baseName}.alert.formError`);
        this.setModalState({ error: error, validationErrors: dto.validationErrors });
      } else {
        this.setModalState({ submitting: true, error: null, info: null, validationErrors: {} });
        const requestConfig = schema
          ? {
              headers: { 'Content-Type': `application/json;profile="/api/v2/schema/${schema}"` },
            }
          : dto.headers || {};

        const promise = (customSubmitForm || this.standardSubmitForm)({
          data: dto.data,
          id: selectedRow && selectedRow.id,
          config: requestConfig,
          create,
        });

        promise
          .then(response => {
            if (create && typeof response.data.id === 'number') {
              // if we have created a new row, select it and save that as the stored selection
              // we do this before afterCreateOrUpdate because we want this new state saved even
              // if that callback redirects the user into the roster
              this.setStored('selectedRow', response.data.id);
              this.setState({ selectedRows: [{ id: response.data.id }] });
            }
            return response;
          })
          .then(response => this.props.afterCreateOrUpdate(response, dto.extras))
          .then(response => {
            if (response === false) return;
            const alertMessage = create
              ? T.t(`${baseName}.createdAlert`, response.data)
              : T.t(`${baseName}.updatedAlert`, response.data);
            this.setModalState({ type: null });
            this.props.setPortalAlertStatus(false, true, alertMessage);
            this.refresh();
          })
          .catch(error => {
            console.log(error);
            const errorState = this.props.onModalError && this.props.onModalError(error);
            const modalState = {
              error: T.t('error.unexpectedError'),
              validationErrors: {},
              submitting: false,
            };
            if (errorState) {
              modalState.error = errorState.modalError;
              modalState.validationErrors = errorState.validationErrors;
            } else {
              const data = error && error.response && error.response.data;
              if (data && data._type === 'ValidationError') {
                modalState.error = T.t(`${baseName}.alert.formError`);
                modalState.validationErrors = { [data.property]: data.message };
              } else if (data && data.type === 'VALIDATION_ERROR' && Array.isArray(data.messages)) {
                const validationErrors = data.messages.reduce(
                  (result, { property, message }) => ({ [property]: message, ...result }),
                  {}
                );
                modalState.error = T.t(`${baseName}.alert.formError`);
                modalState.validationErrors = validationErrors;
              } else if (data && data.message && data.type === 'ModalError') {
                modalState.error = data.message;
              }
            }
            this.setModalState(modalState);
          });
      }
    });
  };

  onInfo = str => this.setModalState({ info: str });

  onError = str => this.setModalState({ error: str });

  getDeleteUrl = () => {
    const { multiSelect, multiDelete } = this.props;
    const { selectedRows } = this.state;
    const multiDeleteUrl = `${this.getBaseUrl()}?${selectedRows
      .map(row => `id=${row.id}`)
      .join('&')}`;
    const singleDeleteUrl = `${this.getBaseUrl()}/${selectedRows[0].id}`;
    return multiSelect && multiDelete && selectedRows.length > 1 ? multiDeleteUrl : singleDeleteUrl;
  };

  deleteRow = () => {
    const { entity, translations: T, handleDelete } = this.props;
    const { selectedRows } = this.state;
    const baseName = `adminPage.${entity}`;
    const { createDeleteDTO, deleteMethod, getDeleteUrl } = handleDelete || {};
    const oneSelected = selectedRows && selectedRows.length === 1;
    const parentHandlingDelete = oneSelected && createDeleteDTO && deleteMethod && getDeleteUrl;
    const selectedRow = parentHandlingDelete && selectedRows[0];
    const data = parentHandlingDelete ? createDeleteDTO(selectedRow.id).data : {};
    const headers = parentHandlingDelete ? createDeleteDTO(selectedRow.id).headers : {};
    this.setModalState({ submitting: true, error: null });
    axios({
      method: parentHandlingDelete ? deleteMethod : 'delete',
      url: parentHandlingDelete ? getDeleteUrl(selectedRow.id) : this.getDeleteUrl(),
      data: data,
      headers: headers,
    })
      .then(() => {
        this.setModalState({ type: null });
        const params = { ...selectedRows[0], smart_count: selectedRows.length };
        this.props.setPortalAlertStatus(false, true, T.t(`${baseName}.deletedAlert`, params));
        this.refresh();
      })
      .catch(error => {
        console.log(error);
        const errorState = (this.props.onModalError && this.props.onModalError(error)) || {};
        this.setModalState({
          error: errorState.modalError || T.t('error.unexpectedError'),
          validationErrors: errorState.validationErrors || {},
          submitting: false,
        });
      });
  };

  render() {
    const { entity } = this.props;
    const { loaded } = this.state;
    const className = `crudTable-${entity}`.replace('/', '-');
    return (
      <div className={classNames(className, { 'crudTable-loaded': loaded })}>
        <div className="container-fluid">
          <div className="row reactTable-buttonBar">
            {this.renderButtonBar()}
            {this.renderSearchForm()}
          </div>
          <div
            className="row"
            ref={el => (this.tableRowEl = el)}
          >
            <div
              className="col"
              id={`crudTable-${this.props.entity}`}
            >
              {this.renderTable()}
            </div>
          </div>
        </div>
        {this.renderModal()}
      </div>
    );
  }
}

ReactTable.propTypes = {
  afterCreateOrUpdate: PropTypes.func,
  autoComplete: PropTypes.string,
  autoSelect: PropTypes.bool,
  baseUrl: PropTypes.string,
  postUrl: PropTypes.string,
  beforeCreateOrUpdate: PropTypes.func,
  searchBarWidth: PropTypes.number,
  canDeleteRow: PropTypes.func,
  canUpdateRow: PropTypes.func,
  columns: PropTypes.array.isRequired,
  createButton: PropTypes.bool,
  createDropdown: PropTypes.bool,
  customFilters: PropTypes.array,
  defaultSearchField: PropTypes.string,
  defaultSortField: PropTypes.string,
  defaultSortOrder: PropTypes.string,
  deleteButton: PropTypes.bool,
  dropdownItems: PropTypes.array,
  embed: PropTypes.string,
  entity: PropTypes.string.isRequired,
  filterWidth: PropTypes.number,
  getButtons: PropTypes.func,
  footerExtra: PropTypes.func,
  headerExtra: PropTypes.func,
  initModal: PropTypes.bool,
  multiDelete: PropTypes.bool,
  multiSelect: PropTypes.bool,
  onDismissModal: PropTypes.func,
  onSearchFieldChange: PropTypes.func,
  openRow: PropTypes.func,
  paginate: PropTypes.bool,
  parseEntity: PropTypes.func,
  refreshRef: PropTypes.func,
  renderForm: PropTypes.func,
  schema: PropTypes.string,
  setPortalAlertStatus: PropTypes.func.isRequired,
  submitForm: PropTypes.func,
  translations: LoPropTypes.translations.isRequired,
  trClassFormat: PropTypes.func,
  tdClassFormat: PropTypes.func,
  updateButton: PropTypes.bool,
  validateForm: PropTypes.func,
  getModalTitle: PropTypes.func,
  onModalError: PropTypes.func,
};

/* https://reactjs.org/docs/typechecking-with-proptypes.html#default-prop-values */
ReactTable.defaultProps = {
  afterCreateOrUpdate: res => res,
  autoComplete: 'on',
  autoSelect: false,
  // baseUrl defaulted in constructor
  beforeCreateOrUpdate: () => null,
  searchBarWidth: 6,
  canDeleteRow: () => true,
  createButton: true,
  createDropdown: false,
  customFilters: [],
  defaultSortOrder: 'asc',
  deleteButton: true,
  dropdownItems: [],
  filterWidth: 3,
  getButtons: () => [],
  footerExtra: () => null,
  headerExtra: () => null,
  multiDelete: false,
  multiSelect: false,
  onDismissModal: () => null,
  onSearchFieldChange: () => null,
  openRow: null,
  parseEntity: entity => entity,
  paginate: true,
  refreshRef: () => null,
  renderForm: () => null,
  updateButton: true,
  canUpdateRow: () => true,
  trClassFormat: () => '',
  tdClassFormat: () => '',
  validateForm: () => null,
  // submitForm defaults to _standardSubmitForm
};

function mapStateToProps(state) {
  return {
    lo_platform: state.main.lo_platform,
  };
}

export const clearSavedTableState = entity => {
  const prefix = entity ? `RT:${entity}:` : 'RT:';
  Object.keys(window.sessionStorage)
    .filter(key => key.startsWith(prefix))
    .forEach(key => window.sessionStorage.removeItem(key));
};

export default connect(mapStateToProps, null, null, { forwardRef: true })(ReactTable);
