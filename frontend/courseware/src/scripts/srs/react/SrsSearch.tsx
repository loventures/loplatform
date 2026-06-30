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

import { useTranslation } from '../../i18n/translationContext';
import { SrsStoreController } from './useSrsStore';
import { debounce, values } from 'lodash';
import React, { useMemo, useState } from 'react';

/**
 * React port of the `srsSearch` directive. DOM preserved: `.input-group` with a
 * search icon, an `input[type=text]` (the Selenide `searchInput` hook), and a
 * `.icon-cancel-circle` clear button (the `clearSearchButton` hook). Debounced
 * 500ms like the original; drives `store.search(value, props)`.
 */

interface SrsSearchProps {
  controller: SrsStoreController;
  props?: Record<string, any> | string[];
  placeholder?: string;
  inputId?: string;
}

export const SrsSearch: React.FC<SrsSearchProps> = ({ controller, props, placeholder, inputId }) => {
  const { store, run } = controller;
  const translate = useTranslation();
  const [field, setField] = useState('');

  const searchProps = props ?? store.searchByProps ?? {};
  const propValues = Array.isArray(searchProps) ? searchProps : values(searchProps);
  const propKeys = Array.isArray(searchProps) ? searchProps : Object.keys(searchProps);

  const placeholderText = placeholder
    ? translate(placeholder)
    : propKeys.map(k => translate('SEARCH_' + k)).join(' or ');

  const doSearch = (value: string) => run(store.search!(value, propValues));
  const debounced = useMemo(() => debounce(doSearch, 500), [store]); // eslint-disable-line react-hooks/exhaustive-deps

  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setField(e.target.value);
    debounced(e.target.value);
  };

  const clear = () => {
    setField('');
    debounced.cancel();
    doSearch('');
  };

  return (
    <div className="input-group flex-shrink-1">
      <span
        className="input-group-text icon icon-search"
        role="presentation"
      />
      <input
        className="form-control hang-next hang-icon-btn"
        id={inputId || undefined}
        type="text"
        role="search"
        aria-label={placeholderText}
        placeholder={placeholderText}
        value={field}
        onChange={onChange}
      />
      <div className="input-hang-end">
        {!!field && (
          <button
            className="icon-btn icon-btn-danger"
            title={translate('SRS_SEARCH_CLEAR')}
            onClick={clear}
          >
            <span className="icon icon-cancel-circle" />
          </button>
        )}
      </div>
    </div>
  );
};
