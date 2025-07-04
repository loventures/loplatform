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

import React from 'react';
import { Provider } from 'react-redux';
import { react2angular } from 'react2angular';
import { lojector } from '../loject.js';

export const withNgReduxProvider = Component => {
  const WithNgProvider = props => (
    <Provider store={lojector.get('$ngRedux')}>
      <Component {...props} />
    </Provider>
  );

  WithNgProvider.displayName = `WithNgProvider(${Component.displayName || Component.name})`;

  return WithNgProvider;
};

export const react2angularWithNgProvider = (Component, bindings, injections) => {
  return react2angular(withNgReduxProvider(Component), bindings, injections);
};
