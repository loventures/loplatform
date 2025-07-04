import { ColumnMeta, RowData } from '@tanstack/react-table';
import { CSSProperties, ReactNode } from 'react';

declare module '@tanstack/react-table' {
  interface ColumnMeta<TData extends RowData, TValue> {
    hidden?: boolean;
    sortable?: boolean;
    defaultSort?: 'asc' | 'desc';
    tdStyle?: CSSProperties;
    thStyle?: CSSProperties;
    width?: string;
    prepend?: ReactNode;
  }
}
