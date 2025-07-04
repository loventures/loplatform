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

import { FadeInLoader } from '../directives/loadingSpinner/FadeInLoader';
import { RestError } from '../error/RestError';
import { TranslationContext } from '../i18n/translationContext';
import { Loadable, isErrored, isLoading } from '../types/loadable';
import React, { ReactElement, useContext } from 'react';

/**
 * This Loader is disfavored because it uses ugly render props. Almost
 * everywhere else we use a selector to provide data from the store.
 * */
const Loader = <T,>({
  loadable,
  loadingMessage,
  children,
}: {
  loadable: Loadable<T>;
  loadingMessage?: string;
} & React.ConsumerProps<T>): ReactElement<any> | null => {
  const translate = useContext(TranslationContext);
  const message = loadingMessage ? loadingMessage : translate('LOADING_MESSAGE');
  if (isLoading(loadable)) {
    return <FadeInLoader message={message} />;
  } else if (isErrored(loadable)) {
    return <RestError error={loadable.error} />;
  } else {
    return <>{children(loadable.data)}</>;
  }
};

export default Loader;
