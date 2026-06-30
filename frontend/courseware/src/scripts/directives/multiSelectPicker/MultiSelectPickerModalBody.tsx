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
import type { ModalControls } from '../modalHost/reactModalHost';
import { SrsList } from '../../srs/react/SrsList';
import { SrsStore } from '../../srs/react/useSrsStore';
import { filter, identity } from 'lodash';
import React, { useState } from 'react';

/**
 * React port of the `multiSelectPickerModal` component (used by the recipient
 * picker). Renders the srs list of items with a checkbox per row and a confirm
 * footer; `close` resolves the selected items array — the same value the old
 * `close({ $value })` passed to `.result`. DOM preserved for the Selenide
 * `RecipientPickerModal`: `ul.card-list-striped-body > li`, the search input, and
 * the `.modal-footer button.btn-primary` confirm.
 */

interface MultiSelectPickerProps {
  store: SrsStore;
  selected?: any[];
}

export const MultiSelectPickerModalBody: React.FC<ModalControls<any[]> & MultiSelectPickerProps> = ({
  close,
  store,
  selected,
}) => {
  const translate = useTranslation();
  const [selection, setSelection] = useState<Record<string | number, any>>(() => {
    const init: Record<string | number, any> = {};
    (selected || []).forEach(item => {
      init[item.id] = item;
    });
    return init;
  });

  const toggle = (item: any, checked: boolean) =>
    setSelection(prev => ({ ...prev, [item.id]: checked ? item : null }));

  const renderItem = (item: any) => (
    <li>
      <label
        className="flex-row-content"
        htmlFor={`list-item-${item.id}`}
      >
        <input
          type="checkbox"
          id={`list-item-${item.id}`}
          checked={!!selection[item.id]}
          onChange={e => toggle(item, e.target.checked)}
        />
        <span
          className="flex-col-fluid text-truncate"
          title={item.getName()}
        >
          {item.getName()}
        </span>
      </label>
    </li>
  );

  return (
    <>
      <SrsList
        store={store}
        renderItem={renderItem}
      />
      <div className="modal-footer">
        <button
          className="btn btn-primary"
          onClick={() => close(filter(selection, identity))}
        >
          {translate('MULTI_SELECT_PICKER_CONFIRM')}
        </button>
      </div>
    </>
  );
};
