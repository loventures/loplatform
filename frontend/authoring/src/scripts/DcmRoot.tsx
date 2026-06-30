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
import { Route, Routes, unstable_HistoryRouter as HistoryRouter } from 'react-router-dom';

import DcmReady from './DcmReady';
import { basename, dcmStore, history } from './dcmStore';
import ErrorDcm from './ErrorDcm';

import $ from 'jquery';

(window as any).$ = (window as any).jQuery = $;

type LoPlatform = Window['lo_platform'];

const DcmRoot = ({ loPlatform }: { loPlatform: LoPlatform }) => {
  return (
    <Provider store={dcmStore}>
      <HistoryRouter
        history={history as any}
        basename={basename}
      >
        <Routes>
          <Route
            path="/error"
            element={<ErrorDcm />}
          />
          <Route
            path="*"
            element={<DcmReady loPlatform={loPlatform} />}
          />
        </Routes>
      </HistoryRouter>
    </Provider>
  );
};

export default DcmRoot;
