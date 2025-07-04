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

import { keys, values, first } from 'lodash';
import React, { useContext } from 'react';
import SearchBox from '../../components/SearchBox.js';
import { TranslationContext } from '../../i18n/translationContext.js';

const ListSearch = ({
  className,
  activeSearchString,
  searchAction,
  searchByProps,
  ariaControls,
  append = undefined,
  prepend = undefined,
}) => {
  const translate = useContext(TranslationContext);
  const placeholder = translate(first(keys(searchByProps)));
  const searchConfig = first(values(searchByProps));
  return (
    <SearchBox
      className={className}
      searchString={activeSearchString || ''}
      placeholder={placeholder}
      searchAction={s => searchAction(s, searchConfig)}
      ariaControls={ariaControls}
      append={append}
      prepend={prepend}
    />
  );
};

export default ListSearch;
