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

import classNames from 'classnames';
import * as React from 'react';
import { useState } from 'react';
import { IoSearchOutline } from 'react-icons/io5';
import { Button, Input, InputGroup } from 'reactstrap';

import { usePolyglot } from '../../hooks';

const SearchBarAutohide: React.FC<{
  value: string;
  size?: 'lg' | 'sm';
  className?: string;
  setValue: (s: string) => void;
  onSearch: () => void;
  disabled?: boolean;
  placeholder: string;
}> = ({ value, size, className, setValue, onSearch, disabled, placeholder }) => {
  const polyglot = usePolyglot();
  const [hidden, setHidden] = useState(true);
  return (
    <div
      className="w-100"
      onClick={() => setHidden(false)}
    >
      <InputGroup className={classNames('search-bar', className, hidden && 'auto-hidden')}>
        {hidden ? (
          <Button
            key="magnifier"
            size={size}
            color="transparent"
            className="border-0 search-button d-flex align-content-center br-50 ms-auto"
            style={{ padding: '.4rem' }}
            title="Search"
            onClick={() => setHidden(false)}
          >
            <IoSearchOutline
              aria-hidden
              size="1rem"
            />
          </Button>
        ) : (
          <>
            <Input
              type="search"
              className="ps-3"
              value={value}
              bsSize={size}
              onChange={e => setValue(e.target.value)}
              placeholder={placeholder}
              autoFocus
              onKeyDown={e => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  onSearch();
                }
              }}
              onBlur={() => {
                if (!value) setHidden(true);
              }}
            />
            <Button
              key="magnifier"
              size={size}
              color="primary"
              disabled={disabled}
              className={`search-icon flex-grow-0 d-flex align-items-center justify-content-center p-0 pe-1 form-control form-control-${size}`}
              onClick={onSearch}
              title={polyglot.t('SEARCH_GO')}
            >
              <IoSearchOutline aria-hidden />
            </Button>
          </>
        )}
      </InputGroup>
    </div>
  );
};

export default SearchBarAutohide;
