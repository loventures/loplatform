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

import { QueryClientProvider, queryClient } from '../resources/queryClient';
import React from 'react';
import { react2angular } from 'react2angular';

export const withQueryProvider = (Component: React.ComponentClass) => {
  const WithQueryProvider = (props: any) => (
    <QueryClientProvider client={queryClient}>
      <Component {...props} />
    </QueryClientProvider>
  );

  WithQueryProvider.displayName = `WithQueryProvider(${Component.displayName || Component.name})`;

  return WithQueryProvider;
};

export const react2angularWithQueryProvider = (
  Component: React.ComponentClass,
  bindings: any,
  injections: any
) => {
  return react2angular(withQueryProvider(Component), bindings, injections);
};
