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

import { debounce } from 'lodash';
import { TranslationContext } from '../i18n/translationContext';
import React, { HTMLAttributes, useContext, useEffect, useRef, useState } from 'react';

//from lodash, can't import directly due to issues with babel-plugin-lodash
type Cancelable = {
  cancel(): void;
  flush(): void;
};

type DebouncedSearch = ((s: string) => void) & Cancelable;

const SearchBox: React.FC<
  {
    searchString: string;
    placeholder: string;
    searchAction: (s: string) => void;
    ariaControls: string;
    append?: React.ReactElement<any>;
    prepend?: React.ReactElement<any>;
  } & HTMLAttributes<unknown>
> = ({ searchString, placeholder, searchAction, ariaControls, append, prepend, ...otherProps }) => {
  const translate = useContext(TranslationContext);
  const inputRef = useRef<HTMLInputElement>(null);
  const [localString, setLocalString] = useState(searchString);
  const [debouncing, setDebouncing] = useState(false);
  const debounceRef = useRef<DebouncedSearch | null>(null);
  useEffect(() => {
    const debounceSearch = debounce(s => {
      searchAction(s);
      setDebouncing(false);
    }, 500);
    debounceRef.current = debounceSearch;
    return () => {
      setDebouncing(false);
      debounceSearch.cancel();
    };
  }, [searchAction]);

  useEffect(() => {
    if (!debouncing) {
      setLocalString(searchString);
    }
  }, [searchString]);

  return (
    <div {...otherProps}>
      <div className="input-group align-items-stretch">
        <span className="input-group-text icon icon-search" />

        <input
          className="form-control hang-next hang-icon-btn"
          type="text"
          aria-label={placeholder}
          ref={inputRef}
          aria-controls={ariaControls}
          placeholder={placeholder}
          value={localString}
          onChange={event => {
            setDebouncing(true);
            setLocalString(event.target.value);
            if (debounceRef.current) {
              debounceRef.current(event.target.value);
            }
          }}
        />

        <div className="input-hang-end">
          {searchString && (
            <button
              className="icon-btn icon-btn-danger"
              title={translate('SRS_SEARCH_CLEAR')}
              onClick={() => {
                setLocalString('');
                searchAction('');
                if (inputRef.current) {
                  inputRef.current.focus();
                }
              }}
            >
              <span className="icon icon-cancel-circle" />
            </button>
          )}
        </div>

        {append && <span className="input-group-append">{append}</span>}
      </div>
    </div>
  );
};

export default SearchBox;
