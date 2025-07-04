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

import ErrorMessage from '../../../directives/ErrorMessage';
import LoadingSpinner from '../../../directives/loadingSpinner';
import { isErrored, isLoaded, isLoading } from '../../../types/loadable';
import { ensureNotNull } from '../../../utils/utils';
import { TranslationContext } from '../../../i18n/translationContext';
import React, { useContext } from 'react';

import LoListContext from './LoListContext';

const LoListLoader = <T,>({
  id,
  emptyMessage = 'SRS_STORE_EMPTY',
  filteredMessage = 'SRS_STORE_FILTERED',
  children,
}: {
  id: string;
  emptyMessage?: string;
  filteredMessage?: string;
} & React.ConsumerProps<T[]>) => {
  const translate = useContext(TranslationContext);
  const listState = ensureNotNull(useContext(LoListContext));

  if (isLoading(listState.listLoadable)) {
    return (
      <div
        className="card-body"
        id={id}
      >
        <LoadingSpinner />
      </div>
    );
  } else if (isLoaded(listState.listLoadable) && listState.totalCount === 0) {
    return (
      <div
        className="card-body"
        id={id}
      >
        <div className="alert alert-warning m-0">{translate(emptyMessage)}</div>
      </div>
    );
  } else if (isLoaded(listState.listLoadable) && listState.filteredCount === 0) {
    return (
      <div
        className="card-body"
        id={id}
      >
        <div
          className="alert alert-warning m-0"
          aria-live="polite"
        >
          {translate(filteredMessage)}
        </div>
      </div>
    );
  } else if (isErrored(listState.listLoadable)) {
    return (
      <div
        className="card-list-body"
        id={id}
      >
        <ErrorMessage error={listState.listLoadable.error} />
      </div>
    );
  } else {
    return (
      <div
        className="card-list-body"
        id={id}
      >
        {children(listState.listLoadable.data as T[])}
      </div>
    );
  }
};

export default LoListLoader;
