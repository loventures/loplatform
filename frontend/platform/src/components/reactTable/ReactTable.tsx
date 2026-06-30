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
import serialize from 'form-serialize';
import Polyglot from 'node-polyglot';
import React, {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
} from 'react';
import { useDispatch } from 'react-redux';
import { debounce } from 'throttle-debounce';

import { setPortalAlertStatus } from '../../redux/actions/MainActions';
import { useLoPlatform } from '../../redux/state';
import { LoPlatform } from '../../types/loPlatform';
import encodeQuery from '../matrix.js';
import ButtonBar from './ButtonBar';
import ReactTableModal, { ModalState } from './ReactTableModal';
import SearchForm from './SearchForm';
import Table from './Table';

const INITIAL_PAGE_SIZE = 17; // this will never be seen unless something breaks

export const getSavedTableState = (entity?: string, attr?: string, dflt?: any): any => {
  const stored = window.sessionStorage.getItem(`RT:${entity}:${attr}`);
  const numeric = typeof dflt === 'number';
  return !stored ? dflt : numeric ? parseInt(stored, 10) : stored;
};

export const clearSavedTableState = (entity?: string): void => {
  const prefix = entity ? `RT:${entity}:` : 'RT:';
  Object.keys(window.sessionStorage)
    .filter(key => key.startsWith(prefix))
    .forEach(key => window.sessionStorage.removeItem(key));
};

interface Row {
  id: number;
  [key: string]: any;
}

interface Filter {
  property: string;
  operator?: string;
  value: any;
  prefilter?: boolean;
}

interface Column {
  dataField: string;
  filterProperty?: string;
  filterOperator?: string;
  prefilter?: boolean;
  searchOperator?: string;
  nullsOpposite?: boolean;
  [key: string]: any;
}

export interface ReactTableProps {
  afterCreateOrUpdate?: (response: any, extras?: any) => any;
  autoComplete?: string;
  autoSelect?: boolean;
  baseUrl?: string;
  postUrl?: string;
  beforeCreateOrUpdate?: (row: any) => void;
  searchBarWidth?: number;
  canDeleteRow?: (row: Row) => boolean;
  canUpdateRow?: (row: Row) => boolean;
  columns: Column[];
  createButton?: boolean;
  createDropdown?: boolean;
  customFilters?: Filter[];
  defaultSearchField?: string;
  defaultSortField?: string;
  defaultSortOrder?: string;
  deleteButton?: boolean;
  dropdownItems?: any[];
  embed?: string;
  entity: string;
  filterWidth?: number;
  getButtons?: (...args: any[]) => any[];
  footerExtra?: (...args: any[]) => React.ReactNode;
  headerExtra?: (...args: any[]) => React.ReactNode;
  initModal?: boolean;
  multiDelete?: boolean;
  multiSelect?: boolean;
  onDismissModal?: () => void;
  onSearchFieldChange?: (field: string) => void;
  openRow?: (row: Row) => void;
  paginate?: boolean;
  parseEntity?: (entity: any) => any;
  refreshRef?: (refresh: () => void) => void;
  renderForm?: (...args: any[]) => React.ReactNode;
  schema?: string;
  submitForm?: (args: any) => Promise<any>;
  translations: Polyglot;
  trClassFormat?: (row: Row) => string;
  tdClassFormat?: (value: any, col: string, row: Row) => string;
  updateButton?: boolean;
  filter?: boolean;
  // Dispatched internally now; still accepted as a (harmless) prop for callers
  // that pass it through from the pre-hooks era.
  setPortalAlertStatus?: (error: any, success: boolean, message: string) => void;
  validateForm?: (data: any, row: any, form: HTMLFormElement) => any;
  getModalTitle?: (type: string | null) => string | undefined;
  onCreate?: () => void;
  onModalError?: (error: any) => { modalError: string; validationErrors: Record<string, string> };
  initFilter?: any;
  createDeleteDTO?: any;
  handleDelete?: {
    createDeleteDTO: (id: any) => { data: any; headers?: any };
    deleteMethod: string;
    getDeleteUrl: (id: any) => string;
  };
  updateUrl?: string;
  updateMethod?: string;
}

interface ReactTableState {
  loaded: boolean;
  fetching: boolean;
  generation: number;
  currentData: Row[];
  totalSize: number;
  autoPageSize: number;
  currentPage: number;
  pageSize: number;
  searchField: string;
  searchValue: string;
  orderField: string;
  orderDir: string;
  modalState: ModalState;
  errorCount: number;
  spinning: boolean;
  selectedRows: Array<{ id: number; [key: string]: any }>;
  stats: any;
  customFilters: Filter[];
  defaultFilters: Record<string, any>;
  searchBarWidth?: number;
}

export interface ReactTableHandle {
  onError: (str: string) => void;
  onInfo: (str: string) => void;
  refresh: () => void;
}

const DEFAULTS = {
  afterCreateOrUpdate: (res: any) => res,
  autoComplete: 'on',
  autoSelect: false,
  beforeCreateOrUpdate: () => null,
  searchBarWidth: 6,
  canDeleteRow: () => true,
  createButton: true,
  createDropdown: false,
  customFilters: [] as Filter[],
  defaultSortOrder: 'asc',
  deleteButton: true,
  dropdownItems: [] as any[],
  filterWidth: 3,
  getButtons: () => [] as any[],
  footerExtra: () => null,
  headerExtra: () => null,
  multiDelete: false,
  multiSelect: false,
  onDismissModal: () => null,
  onSearchFieldChange: () => null,
  openRow: undefined,
  parseEntity: (entity: any) => entity,
  paginate: true,
  refreshRef: () => null,
  renderForm: () => null,
  updateButton: true,
  canUpdateRow: () => true,
  trClassFormat: () => '',
  tdClassFormat: () => '',
  validateForm: () => null,
};

const ReactTable = forwardRef<ReactTableHandle, ReactTableProps>((rawProps, ref) => {
  const props = { ...DEFAULTS, ...rawProps } as Required<
    Omit<
      ReactTableProps,
      | 'baseUrl'
      | 'postUrl'
      | 'embed'
      | 'schema'
      | 'submitForm'
      | 'getModalTitle'
      | 'onModalError'
      | 'initFilter'
      | 'createDeleteDTO'
      | 'handleDelete'
      | 'updateUrl'
      | 'updateMethod'
      | 'defaultSearchField'
      | 'defaultSortField'
      | 'initModal'
    >
  > &
    ReactTableProps;

  const dispatch = useDispatch();
  const lo_platform: LoPlatform = useLoPlatform();
  const T = props.translations;

  const getBaseUrl = () => props.baseUrl || `/api/v2/${props.entity}`;
  const getPostUrl = () => props.postUrl || `/api/v2/${props.entity}`;

  const setStored = (attr: string, value: string | number) =>
    window.sessionStorage.setItem(`RT:${props.entity}:${attr}`, value.toString());

  const getFilters = (state: ReactTableState) => {
    if (state.customFilters.length || state.searchValue) {
      return {
        filter: state.customFilters.filter(filter => !filter.prefilter),
        prefilter: state.customFilters.filter(filter => filter.prefilter),
      };
    }
    return {} as { filter?: Filter[]; prefilter?: Filter[] };
  };

  const stateQuery = (state: ReactTableState): string => {
    const { filter, prefilter } = getFilters(state);
    if (state.searchValue) {
      const col = props.columns.find(col => col.dataField === state.searchField);
      const operator = col?.searchOperator ? col.searchOperator : 'co';
      filter!.push({ property: state.searchField, operator, value: state.searchValue });
    }
    const query: any = {
      offset: (state.currentPage - 1) * state.pageSize,
      limit: state.pageSize,
      filter: filter && filter.length ? filter : null,
      prefilter: prefilter && prefilter.length ? prefilter : null,
    };
    if (state.orderField && state.orderDir) {
      const col = props.columns.find(col => col.dataField === state.orderField);
      const direction = !col?.nullsOpposite
        ? state.orderDir
        : state.orderDir === 'asc'
          ? 'ascNullsFirst'
          : 'descNullsLast';
      query.order = { property: state.orderField, direction };
    }
    if (props.paginate === false) {
      delete query.offset;
      delete query.limit;
    }
    if (props.embed) query.embed = props.embed;
    return encodeQuery(query);
  };

  const buildInitialState = (): ReactTableState => {
    const selectedRow = getSavedTableState(props.entity, 'selectedRow', 0);
    const savedColumns = props.columns.filter(column => column.filterProperty);
    const defaultFilters = savedColumns.reduce(
      (o, { dataField }) => ({ ...o, [dataField]: getSavedTableState(props.entity, dataField) }),
      {} as Record<string, any>
    );
    const customFilters = savedColumns.reduce(
      (filters: Filter[], { dataField, filterProperty, filterOperator, prefilter }) => {
        const value = defaultFilters[dataField];
        return !value
          ? filters
          : [
              ...filters.filter(f => f.property !== filterProperty),
              {
                property: filterProperty!,
                operator: filterOperator || 'eq',
                prefilter,
                value,
              },
            ];
      },
      props.customFilters || []
    );
    return {
      loaded: false,
      fetching: false,
      generation: 0,
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
      errorCount: 0,
      spinning: false,
      selectedRows: selectedRow ? [{ id: selectedRow }] : [],
      stats: {},
      customFilters,
      defaultFilters,
    };
  };

  const [state, setReactState] = useState<ReactTableState>(buildInitialState);
  const stateRef = useRef(state);
  stateRef.current = state;
  const prevStateRef = useRef<ReactTableState | null>(null);
  const tableRowEl = useRef<HTMLDivElement | null>(null);

  // class-style setState supporting object or updater function
  const setState = useCallback(
    (update: Partial<ReactTableState> | ((prev: ReactTableState) => Partial<ReactTableState>)) => {
      setReactState(prev => {
        const patch = typeof update === 'function' ? update(prev) : update;
        return { ...prev, ...patch };
      });
    },
    []
  );

  const refresh = useCallback(() => {
    setState(prev => ({ generation: prev.generation + 1 }));
  }, [setState]);

  const setModalState = useCallback(
    (modalState: Partial<ModalState>) => {
      setState(prev => ({
        modalState: {
          type: Object.prototype.hasOwnProperty.call(modalState, 'type')
            ? (modalState.type as ModalState['type'])
            : prev.modalState.type,
          submitting: Object.prototype.hasOwnProperty.call(modalState, 'submitting')
            ? (modalState.submitting as boolean)
            : prev.modalState.submitting,
          error: Object.prototype.hasOwnProperty.call(modalState, 'error')
            ? (modalState.error as string | null)
            : prev.modalState.error,
          info: Object.prototype.hasOwnProperty.call(modalState, 'info')
            ? (modalState.info as string | null)
            : prev.modalState.info,
          validationErrors: Object.prototype.hasOwnProperty.call(modalState, 'validationErrors')
            ? (modalState.validationErrors as Record<string, string>)
            : prev.modalState.validationErrors,
        },
      }));
    },
    [setState]
  );

  const computeAutoPageSize = useCallback(() => {
    let rows = 16;
    if (tableRowEl.current) {
      const tableBody = tableRowEl.current.getElementsByClassName('react-bs-container-body')[0];
      if (tableBody) {
        const firstRow = tableBody.getElementsByTagName('tr')[0];
        const pager = document.getElementById('page-size-col');
        if (firstRow && pager) {
          let offset = pager.offsetHeight;
          for (
            let current: HTMLElement | null = tableBody as HTMLElement;
            current;
            current = current.offsetParent as HTMLElement | null
          ) {
            offset += current.offsetTop;
          }
          rows = Math.max(
            Math.floor((window.innerHeight - offset - 15) / firstRow.offsetHeight) || 16,
            4
          );
        }
      }
    }
    setState(({ pageSize, autoPageSize }) => ({
      autoPageSize: rows,
      pageSize: pageSize === autoPageSize ? rows : pageSize,
    }));
  }, [setState]);

  const loadData = useCallback(() => {
    const { autoSelect, parseEntity, paginate } = props;
    const cur = stateRef.current;
    const { generation, loaded, currentPage: page } = cur;
    const matrixQuery = stateQuery(cur);
    setState({ fetching: true });
    const matrix = matrixQuery ? `;${matrixQuery}` : '';
    axios
      .get(`${getBaseUrl()}${matrix}`, { hideProgress: true } as any)
      .then(response => {
        const latest = stateRef.current;
        const { generation: gen, selectedRows: sels } = latest;
        const data = response.data.objects.map(parseEntity);
        const stale = generation !== gen || matrixQuery !== stateQuery(stateRef.current);
        const selectAll = autoSelect && !loaded;
        const reselect = data.filter(
          (row: Row) => selectAll || sels.map(sel => sel.id).includes(row.id)
        );
        setState(prev => ({
          loaded: true,
          fetching: false,
          currentData: data,
          totalSize: !paginate ? 16 : response.data.filterCount,
          stats: response.data,
          spinning: false,
          selectedRows: reselect,
          currentPage: page - (page > 1 && !data.length ? 1 : 0),
          generation: prev.generation + (stale ? 1 : 0),
        }));
      })
      .catch(error => {
        console.log(error);
        setState({ fetching: false, loaded: true });
        dispatch(setPortalAlertStatus(true, false, T.t('error.unexpectedError')));
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [setState, dispatch, T]);

  // componentDidMount / componentWillUnmount
  useEffect(() => {
    const computePageSizeDebounced = debounce(300, computeAutoPageSize);
    window.addEventListener('resize', computePageSizeDebounced);
    computeAutoPageSize();
    props.refreshRef(refresh);
    return () => {
      window.removeEventListener('resize', computePageSizeDebounced);
      dispatch(setPortalAlertStatus(false, false, ''));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // UNSAFE_componentWillReceiveProps(initModal)
  const prevInitModal = useRef(rawProps.initModal);
  useEffect(() => {
    if (rawProps.initModal && rawProps.initModal !== prevInitModal.current) {
      setModalState({ type: 'create' });
    }
    prevInitModal.current = rawProps.initModal;
  }, [rawProps.initModal, setModalState]);

  // componentDidUpdate
  useEffect(() => {
    const prevState = prevStateRef.current;
    if (
      !state.fetching &&
      (prevState === null ||
        stateQuery(state) !== stateQuery(prevState) ||
        state.generation !== prevState.generation ||
        !state.loaded)
    ) {
      loadData();
    }
    (
      ['currentPage', 'pageSize', 'searchField', 'searchValue', 'orderField', 'orderDir'] as const
    ).forEach(attr => setStored(attr, (state[attr] as any) || ''));
    const selectedRows = state.selectedRows;
    setStored('selectedRow', selectedRows.length === 1 ? selectedRows[0].id : '');
    props.columns
      .filter(column => column.filterProperty)
      .forEach(({ dataField, filterProperty }) => {
        const filter = state.customFilters.find(filter => filter.property === filterProperty);
        setStored(dataField, filter ? filter.value : '');
      });
    prevStateRef.current = state;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state]);

  const customFilterOnChange = (evt: any, onChange: (e: any, filters: Filter[]) => Filter[]) => {
    const customFilters = onChange(evt, stateRef.current.customFilters);
    setState({ customFilters });
  };

  const setSearchField = (field: string) => {
    props.onSearchFieldChange(field);
    setState({ currentPage: 1, searchField: field });
  };

  const setSearchValue = (value: string) => {
    setState({ currentPage: 1, searchValue: value });
  };

  const setSearchDebounced = useRef(debounce(300, setSearchValue)).current;

  const onSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const spinning = e.target.value !== stateRef.current.searchValue;
    if (spinning !== stateRef.current.spinning) {
      setState({ spinning });
    }
    setSearchDebounced(e.target.value);
  };

  const onSortChange = (sortName: string, sortOrder: string) => {
    setState({ orderField: sortName, orderDir: sortOrder });
  };

  const onPageChange = (page: number, pageSize: number) => {
    setState({ currentPage: page, pageSize: pageSize });
  };

  const setPageSize = (pageSize: number) => {
    const cur = stateRef.current;
    const newPageSize = pageSize || cur.autoPageSize;
    const newPage = 1 + parseInt('' + ((cur.currentPage - 1) * cur.pageSize) / newPageSize, 10);
    setState({ currentPage: newPage, pageSize: newPageSize });
  };

  const onRowSelect = (row: Row, isSelected: boolean, e: React.MouseEvent) => {
    const selectedRows = [...stateRef.current.selectedRows];
    const { multiSelect } = props;
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
    setState({ selectedRows });
  };

  const showModal = (style: string) => {
    if (style !== 'delete') {
      const { selectedRows } = stateRef.current;
      const selectedRow = selectedRows && selectedRows.length === 1 && selectedRows[0];
      props.beforeCreateOrUpdate(style === 'update' ? selectedRow : {});
    }
    setModalState({
      info: null,
      error: null,
      submitting: false,
      type: style as ModalState['type'],
      validationErrors: {},
    });
  };

  const hideModal = () => {
    props.onDismissModal();
    setModalState({ type: null });
  };

  const standardSubmitForm = ({ data, id, config, create }: any) => {
    const { updateUrl, updateMethod } = props;
    if (create) return axios.post(getPostUrl(), data, config);
    else {
      return axios({
        method: updateMethod || 'put',
        url: updateUrl || getBaseUrl() + '/' + id,
        data: data,
        ...config,
      });
    }
  };

  const submitRow = (form: HTMLFormElement, create: boolean) => {
    const { entity, schema, submitForm: customSubmitForm } = props;
    const { selectedRows } = stateRef.current;
    const selectedRow = selectedRows && selectedRows.length === 1 && selectedRows[0];
    const baseName = `adminPage.${entity}`;
    const dtoPromise = Promise.resolve(
      props.validateForm(serialize(form, { hash: true }), create ? {} : selectedRow, form)
    );
    dtoPromise.then((dto: any) => {
      setState(prev => ({ errorCount: 1 + prev.errorCount }));
      if (dto.validationErrors) {
        const error = dto.error || T.t(`${baseName}.alert.formError`);
        setModalState({ error: error, validationErrors: dto.validationErrors });
      } else {
        setModalState({ submitting: true, error: null, info: null, validationErrors: {} });
        const requestConfig = schema
          ? {
              headers: {
                'Content-Type': `application/json;profile="/api/v2/schema/${schema}"`,
              },
            }
          : dto.headers || {};

        const promise = (customSubmitForm || standardSubmitForm)({
          data: dto.data,
          id: selectedRow && selectedRow.id,
          config: requestConfig,
          create,
        });

        promise
          .then((response: any) => {
            if (create && typeof response.data.id === 'number') {
              setStored('selectedRow', response.data.id);
              setState({ selectedRows: [{ id: response.data.id }] });
            }
            return response;
          })
          .then((response: any) => props.afterCreateOrUpdate(response, dto.extras))
          .then((response: any) => {
            if (response === false) return;
            const alertMessage = create
              ? T.t(`${baseName}.createdAlert`, response.data)
              : T.t(`${baseName}.updatedAlert`, response.data);
            setModalState({ type: null });
            dispatch(setPortalAlertStatus(false, true, alertMessage));
            refresh();
          })
          .catch((error: any) => {
            console.log(error);
            const errorState = props.onModalError && props.onModalError(error);
            const modalState: any = {
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
                  (result: any, { property, message }: any) => ({
                    [property]: message,
                    ...result,
                  }),
                  {}
                );
                modalState.error = T.t(`${baseName}.alert.formError`);
                modalState.validationErrors = validationErrors;
              } else if (data && data.message && data.type === 'ModalError') {
                modalState.error = data.message;
              }
            }
            setModalState(modalState);
          });
      }
    });
  };

  const getDeleteUrl = () => {
    const { multiSelect, multiDelete } = props;
    const { selectedRows } = stateRef.current;
    const multiDeleteUrl = `${getBaseUrl()}?${selectedRows.map(row => `id=${row.id}`).join('&')}`;
    const singleDeleteUrl = `${getBaseUrl()}/${selectedRows[0].id}`;
    return multiSelect && multiDelete && selectedRows.length > 1 ? multiDeleteUrl : singleDeleteUrl;
  };

  const deleteRow = () => {
    const { entity, handleDelete } = props;
    const { selectedRows } = stateRef.current;
    const baseName = `adminPage.${entity}`;
    const { createDeleteDTO, deleteMethod, getDeleteUrl: handleGetDeleteUrl } = handleDelete || {};
    const oneSelected = selectedRows && selectedRows.length === 1;
    const parentHandlingDelete =
      oneSelected && createDeleteDTO && deleteMethod && handleGetDeleteUrl;
    const selectedRow: any = parentHandlingDelete && selectedRows[0];
    const data = parentHandlingDelete ? createDeleteDTO(selectedRow.id).data : {};
    const headers = parentHandlingDelete ? createDeleteDTO(selectedRow.id).headers : {};
    setModalState({ submitting: true, error: null });
    axios({
      method: parentHandlingDelete ? deleteMethod : 'delete',
      url: parentHandlingDelete ? handleGetDeleteUrl(selectedRow.id) : getDeleteUrl(),
      data: data,
      headers: headers,
    })
      .then(() => {
        setModalState({ type: null });
        const params = { ...selectedRows[0], smart_count: selectedRows.length };
        dispatch(setPortalAlertStatus(false, true, T.t(`${baseName}.deletedAlert`, params)));
        refresh();
      })
      .catch(error => {
        console.log(error);
        const errorState = (props.onModalError && props.onModalError(error)) || ({} as any);
        setModalState({
          error: errorState.modalError || T.t('error.unexpectedError'),
          validationErrors: errorState.validationErrors || {},
          submitting: false,
        });
      });
  };

  const onModalSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const { modalState } = stateRef.current;
    switch (modalState.type) {
      case 'create':
        return submitRow(e.target as HTMLFormElement, true);
      case 'update':
        return submitRow(e.target as HTMLFormElement, false);
      case 'delete':
        return deleteRow();
      default:
        return;
    }
  };

  const onInfo = (str: string) => setModalState({ info: str });
  const onError = (str: string) => setModalState({ error: str });

  useImperativeHandle(ref, () => ({ onError, onInfo, refresh }));

  const renderButtonBar = () => {
    const { columns } = props;
    const filterColumns = columns.filter(col => col.filterable && col.filterOptions);
    return (
      <ButtonBar
        xs={12}
        md={filterColumns.length ? 12 : 12 - (state.searchBarWidth as number)}
        lg={12 - props.searchBarWidth}
        baseName={`adminPage.${props.entity}.toolBar`}
        createButton={props.createButton}
        T={props.translations}
        createDropdown={props.createDropdown}
        dropdownItems={props.dropdownItems}
        updateButton={props.updateButton}
        selectedRows={state.selectedRows as any}
        getButtons={props.getButtons}
        multiSelect={props.multiSelect}
        canDeleteRow={props.canDeleteRow}
        canUpdateRow={props.canUpdateRow}
        deleteButton={props.deleteButton}
        multiDelete={props.multiDelete}
        onJefreshClicked={refresh}
        fetching={state.fetching}
        loaded={state.loaded}
        lo_platform={lo_platform}
        refresh={refresh}
        showModal={showModal}
        onCreate={props.onCreate as any}
        renderForm={props.renderForm}
        filterColsLength={filterColumns.length}
      />
    );
  };

  const renderSearchForm = () => (
    <SearchForm
      T={props.translations}
      entity={props.entity}
      setSearchField={setSearchField}
      setSearchValue={setSearchValue}
      defaultValue={state.searchValue}
      columns={props.columns}
      fetching={state.fetching}
      spinning={state.spinning}
      searchField={state.searchField}
      customFilterOnChange={customFilterOnChange}
      filterWidth={props.filterWidth}
      searchBarWidth={props.searchBarWidth}
      onSearchChange={onSearchChange}
      defaultFilters={state.defaultFilters}
    />
  );

  const renderTable = () => (
    <Table
      entity={props.entity}
      pageSize={state.pageSize}
      totalSize={state.totalSize}
      stats={state.stats}
      currentData={state.currentData}
      csvUrl={`${getBaseUrl()}.csv;${stateQuery(state)}`}
      setPageSize={setPageSize}
      columns={props.columns as any}
      T={props.translations}
      orderField={state.orderField}
      orderDir={state.orderDir}
      onSortChange={onSortChange}
      currentPage={state.currentPage}
      onPageChange={onPageChange}
      openRow={props.openRow}
      loaded={state.loaded}
      fetching={state.fetching}
      onRowSelect={onRowSelect}
      selectedRows={state.selectedRows as any}
      trClassFormat={props.trClassFormat}
      tdClassFormat={props.tdClassFormat}
      multiSelect={props.multiSelect}
    />
  );

  const renderModal = () => {
    const {
      entity,
      autoComplete,
      renderForm,
      translations,
      getModalTitle,
      footerExtra,
      headerExtra,
    } = props;
    const { modalState, selectedRows, errorCount } = state;
    return (
      modalState.type && (
        <ReactTableModal
          autoComplete={autoComplete}
          entity={entity}
          T={translations}
          getModalTitle={getModalTitle}
          footerExtra={footerExtra}
          headerExtra={headerExtra}
          modalState={modalState}
          selectedRows={selectedRows as any}
          renderForm={renderForm as any}
          errorCount={errorCount}
          onModalSubmit={onModalSubmit}
          hideModal={hideModal}
        />
      )
    );
  };

  const { entity } = props;
  const { loaded } = state;
  const className = `crudTable-${entity}`.replace('/', '-');
  return (
    <div className={classNames(className, { 'crudTable-loaded': loaded })}>
      <div className="container-fluid">
        <div className="row reactTable-buttonBar">
          {renderButtonBar()}
          {renderSearchForm()}
        </div>
        <div
          className="row"
          ref={el => {
            (tableRowEl.current = el);
          }}
        >
          <div
            className="col"
            id={`crudTable-${props.entity}`}
          >
            {renderTable()}
          </div>
        </div>
      </div>
      {renderModal()}
    </div>
  );
});

ReactTable.displayName = 'ReactTable';

export default ReactTable;
