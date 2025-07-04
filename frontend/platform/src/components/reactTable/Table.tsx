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

import React from 'react';

import {
  useReactTable,
  getCoreRowModel,
  flexRender,
  ColumnDef,
  AccessorKeyColumnDef,
} from '@tanstack/react-table';

import PaginationPanel from './PaginationPanel';
import Polyglot from 'node-polyglot';
import { SrsCollectionInfo } from '../../srs';

type DataFormat<T> = (value: any, row: T) => any;

type TableCol<T> = {
  dataField: string;
  isKey?: boolean;
  sortable?: boolean;
  hidden?: boolean;
  width?: string;
  dataFormat?: DataFormat<T>;
  defaultSort?: 'asc' | 'desc';
  thStyle?: React.CSSProperties;
  prepend?: React.ReactNode;
};

type Id = {
  id: number;
};

type TableProps<T extends Id = Id> = {
  entity: string;
  pageSize: number;
  totalSize: number;
  stats: SrsCollectionInfo;
  currentData: T[];
  csvUrl: string;
  setPageSize?: (pageSize: number) => void;
  columns: TableCol<T>[];
  T: Polyglot;
  orderField: string;
  orderDir: string;
  onSortChange?: (col: string, order: 'asc' | 'desc') => void;
  currentPage: number;
  onPageChange?: (page: number, pageSize: number) => void;
  openRow?: (t: T) => void;
  loaded: boolean;
  fetching: boolean;
  multiSelect: boolean;
  onRowSelect: (row: T, isSelected: boolean, e: React.MouseEvent) => void;
  selectedRows: T[];
  trClassFormat?: (t: T) => string;
  tdClassFormat?: (value: any, col: string, t: T) => string;
};

function getColumns<T extends Id>(props: TableProps<T>): ColumnDef<T>[] {
  const { columns, entity, T } = props;
  const baseName = `adminPage.${entity}.fieldName`;

  const cols = columns.map((col): ColumnDef<T> => {
    const { prepend, dataField, dataFormat } = col;
    return {
      accessorKey: dataField,
      header: T.t(`${baseName}.${dataField}`),
      cell: info => {
        const value = info.getValue();
        if (dataFormat) {
          return dataFormat(value, info.row.original);
        } else if (value == null) {
          return '';
        } else if (typeof value !== 'object') {
          return value.toString();
        }
      },
      meta: {
        hidden: col.hidden || col.isKey,
        sortable: col.sortable,
        defaultSort: col.defaultSort,
        width: col.width,
        thStyle: {
          display: col.hidden || col.isKey ? 'none' : undefined,
          ...(col.thStyle ?? {}),
        },
        tdStyle: {
          display: col.hidden || col.isKey ? 'none' : undefined,
        },
        prepend,
      },
    };
  });

  /* Put the key column second so the css :first-child selector works for the first column left border,
   * or else the hidden first PK column will get selected and we'll have a double left border. */
  cols.splice(0, 2, cols[1], cols[0]);
  return cols;
}

function Table<T extends Id>(props: TableProps<T>): React.ReactNode {
  const {
    currentData,
    selectedRows,
    trClassFormat,
    tdClassFormat,
    loaded,
    fetching,
    T,
    openRow,
    onRowSelect,
    orderField,
    orderDir,
    onSortChange,
    entity,
    pageSize,
    multiSelect,
  } = props;

  const columns = getColumns(props);
  const table = useReactTable({
    data: currentData,
    columns: columns,
    getCoreRowModel: getCoreRowModel(),
  });

  const isSelected = (row: T) => selectedRows.includes(row);

  const trClassName = (row: T) =>
    `${trClassFormat?.(row) ?? ''} data ${isSelected(row) ? 'table-info table-active' : ''}`;

  const columnKey = (col: ColumnDef<T>): string =>
    (col as AccessorKeyColumnDef<T>).accessorKey as string;

  const rowHeight = document
    .getElementById(`crudTable-${entity}`)
    ?.getElementsByClassName('react-bs-container-body')?.[0]
    ?.getElementsByTagName('tr')?.[0]?.offsetHeight;

  return (
    <div className="react-bs-table-container reactTable-bsTable">
      <div
        className="react-bs-table"
        style={{ height: rowHeight ? rowHeight * (1 + pageSize) : undefined }}
      >
        <table className="table table-hover table-condensed">
          {table.getHeaderGroups().map(headerGroup => (
            <colgroup key={headerGroup.id}>
              {headerGroup.headers.map(header => (
                <col
                  key={header.id}
                  style={{
                    display: header.column.columnDef.meta?.hidden ? 'none' : undefined,
                    width: header.column.columnDef.meta?.width,
                    minWidth: header.column.columnDef.meta?.width,
                  }}
                />
              ))}
            </colgroup>
          ))}
          <thead className="react-bs-container-header">
            {table.getHeaderGroups().map(headerGroup => (
              <tr key={headerGroup.id}>
                {headerGroup.headers.map(header => (
                  <th
                    colSpan={header.colSpan}
                    key={header.id}
                    className={header.column.columnDef.meta?.sortable ? 'sort-column' : undefined}
                    style={header.column.columnDef.meta?.thStyle}
                    data-field={columnKey(header.column.columnDef)}
                    onClick={() => {
                      if (header.column.columnDef.meta?.sortable) {
                        const key = columnKey(header.column.columnDef);
                        const order =
                          key === orderField
                            ? orderDir === 'asc'
                              ? 'desc'
                              : 'asc'
                            : (header.column.columnDef.meta?.defaultSort ?? 'asc');
                        onSortChange?.(key, order);
                      }
                    }}
                  >
                    {header.column.columnDef.meta?.prepend ?? null}
                    {header.isPlaceholder
                      ? null
                      : flexRender(
                          header.column.columnDef.header, // Header definition
                          header.getContext() // Context for the header
                        )}
                    {!header.column.columnDef.meta?.sortable ? null : columnKey(
                        header.column.columnDef
                      ) === orderField ? (
                      <span className={orderDir === 'asc' ? 'order dropup' : 'order'}>
                        <span
                          className="caret"
                          style={{ margin: '10px 5px' }}
                        ></span>
                      </span>
                    ) : (
                      <span className="order text-lightGrey">
                        <span className="dropdown">
                          <span
                            className="caret"
                            style={{ margin: '10px 0px 10px 5px' }}
                          ></span>
                        </span>
                        <span className="dropup">
                          <span
                            className="caret"
                            style={{ margin: '10px 0px' }}
                          ></span>
                        </span>
                      </span>
                    )}
                  </th>
                ))}
              </tr>
            ))}
          </thead>
          <tbody className="react-bs-container-body">
            {!table.getRowModel().rows.length && (
              <tr>
                <td
                  data-toggle="collapse"
                  colSpan={columns.reduce((count, col) => count + (col.meta?.hidden ? 0 : 1), 0)}
                  className="react-bs-table-no-data"
                >
                  {T.t(
                    loaded && !fetching
                      ? 'crudTable.noResultsPlaceholder'
                      : 'crudTable.loadingPlaceholder'
                  )}
                </td>
              </tr>
            )}
            {table.getRowModel().rows.map(row => (
              <tr
                key={row.id}
                className={trClassName(row.original)}
                onClick={e =>
                  onRowSelect(row.original, !isSelected(row.original) || !multiSelect, e)
                }
                onDoubleClick={() => openRow?.(row.original)}
              >
                {row.getVisibleCells().map(cell => (
                  <td
                    key={cell.id}
                    style={cell.column.columnDef.meta?.tdStyle}
                    className={tdClassFormat?.(
                      cell.getValue(),
                      columnKey(cell.column.columnDef),
                      row.original
                    )}
                  >
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {props.onPageChange && (
        <div className="react-bs-table-pagination">
          <div
            className="row"
            style={{ marginTop: 15 }}
          >
            <PaginationPanel
              entity={props.entity}
              pageSize={props.pageSize}
              dataSize={currentData.length}
              csvUrl={props.csvUrl}
              setPageSize={props.setPageSize}
              onPageChange={props.onPageChange}
              stats={props.stats}
              T={T}
            />
          </div>
        </div>
      )}
    </div>
  );
}

export default Table;
