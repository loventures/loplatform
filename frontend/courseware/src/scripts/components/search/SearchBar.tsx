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

import { useTranslation } from '../../i18n/translationContext';
import React from 'react';
import { IoSearchOutline } from 'react-icons/io5';
import { Button, Input, InputGroup } from 'reactstrap';

export const SearchBar: React.FC<{
  value: string;
  setValue: (s: string) => void;
  onSearch: () => void;
  disabled?: boolean;
  autoFocus?: boolean;
}> = ({ value, setValue, onSearch, disabled, autoFocus }) => {
  const translate = useTranslation();
  return (
    <InputGroup className="search-bar">
      <Input
        type="search"
        className="ps-3"
        value={value}
        onChange={e => setValue(e.target.value)}
        autoFocus={autoFocus}
        placeholder={translate('SEARCH_BAR_PLACEHOLDER')}
        aria-label={translate('SEARCH_BAR_DESCRIPTION')}
        onKeyDown={e => {
          if (e.key === 'Enter') {
            e.preventDefault();
            onSearch();
          }
        }}
      />
      <Button
        color="primary"
        disabled={disabled}
        className="form-control flex-grow-0 d-flex align-items-center justify-content-center p-0 pe-1"
        onClick={onSearch}
        title={translate('SEARCH_GO')}
        aria-label={translate('SEARCH_GO')}
      >
        <IoSearchOutline aria-hidden />
      </Button>
    </InputGroup>
  );
};
