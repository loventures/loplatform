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

import { ConnectedRouter } from 'connected-react-router';
import React from 'react';
import { Provider } from 'react-redux';
import { Link, Redirect, Route, Switch } from 'react-router-dom';

import DcmReady from './DcmReady';
import { dcmStore, history } from './dcmStore';
import ErrorDcm from './ErrorDcm';
import { dcmPaths } from './router/routes';

import $ from 'jquery';

window.$ = window.jQuery = $;

const DcmRoot = ({ loPlatform }) => {
  return (
    <Provider store={dcmStore}>
      <ConnectedRouter history={history}>
        <Switch>
          <Route path={dcmPaths}>
            <DcmReady loPlatform={loPlatform} />
          </Route>
          <Route
            path="/error"
            component={ErrorDcm}
          />
        </Switch>
      </ConnectedRouter>
    </Provider>
  );
};

export default DcmRoot;
