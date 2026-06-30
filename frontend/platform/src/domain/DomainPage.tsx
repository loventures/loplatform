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

import React from 'react';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';

import { loadProgressBar } from '../components/progress';
import { initCsrf } from '../services';
import { store } from './domainStore';
import Routes from './Routes';

initCsrf();

loadProgressBar({ showSpinner: false });

const App: React.FC = () => (
  <Provider store={store}>
    <BrowserRouter basename={window.lo_base_url || '/'}>
      <Routes />
    </BrowserRouter>
  </Provider>
);

export default App;
