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

import React, { useEffect, useRef, useState } from 'react';

interface GraderSelectProps<T> {
  options: T[];
  selected?: T;
  getKey: (option: T) => React.Key;
  renderSelected: (selected?: T) => React.ReactNode;
  renderOption: (option: T) => React.ReactNode;
  onSelect: (option: T) => void;
}

/**
 * React port of the `loSelect` directive as used by the grader's submission selectors — the attempt /
 * question dropdowns. DOM preserved verbatim from loSelect.html so the Selenide `LoSelect` page object
 * holds: `.lo-select.dropdown` root, `.dropdown-toggle` button (the selected display), and a
 * `.dropdown-menu > ul.list-group > li.btn` option list. The menu stays mounted and is shown/hidden via
 * an explicit `display` toggle (deterministic for Selenide, vs. relying on the bootstrap `.open` CSS).
 *
 * Replaces two `compile=`-based `lo-select` instances — one of the remaining `$compile` holdouts.
 */
export function GraderSelect<T>({
  options,
  selected,
  getKey,
  renderSelected,
  renderOption,
  onSelect,
}: GraderSelectProps<T>) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);

  // Close on outside click (uib-dropdown did this for free).
  useEffect(() => {
    if (!open) return;
    const onDocClick = (e: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onDocClick);
    return () => document.removeEventListener('mousedown', onDocClick);
  }, [open]);

  return (
    <div
      ref={rootRef}
      className={`lo-select w-100 dropdown${open ? ' show open' : ''}`}
    >
      <button
        type="button"
        className="btn btn-outline-primary w-100 dropdown-toggle d-flex align-items-center"
        style={{justifyContent: 'space-between'}}
        aria-expanded={open}
        onClick={() => setOpen(o => !o)}
      >
        {renderSelected(selected)}
      </button>

      <div
        className={`w-100 p-0 dropdown-menu${open ? ' show' : ''}`}
        role="menu"
        style={{ display: open ? 'block' : 'none' }}
      >
        <ul className="list-group list-unstyled btn-group-vertical w-100">
          {options.map(option => (
            <li
              key={getKey(option)}
              className={`btn btn-outline-primary w-100${option === selected ? ' selected' : ''}`}
              onClick={() => {
                onSelect(option);
                setOpen(false);
              }}
            >
              {renderOption(option)}
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

export default GraderSelect;
