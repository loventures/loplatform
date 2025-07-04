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
import { connect } from 'react-redux';
import { Alert, Col, Row } from 'reactstrap';
import _ from 'underscore';

import Loading from '../etc/EtcLoading';
import MessageDisplay from './MessageDisplay';
import { loadLocalmail, loadLocalmails } from './reducks';
import Selector from './Selector';

const Localmail = connect(state => ({ ...state.localmail, T: state.main.translations }))(
  ({ currentAccount, currentMessage, error, loaded, messagess, T }) =>
    error ? (
      <div
        id="localmail-container"
        className="container-fluid"
      >
        <Alert
          color="danger"
          id="localmail-error"
        >
          {T.t(error)}
        </Alert>
      </div>
    ) : !loaded ? (
      <Loading />
    ) : _.isEmpty(messagess) ? (
      <div
        id="localmail-container"
        className="container-fluid"
      >
        <Alert
          color="success"
          id="localmail-error"
        >
          No messages.
        </Alert>
      </div>
    ) : (
      <div
        id="localmail-container"
        className="container-fluid"
      >
        <Row>
          <Col
            xs={4}
            lg={3}
          >
            <Selector />
          </Col>
          <Col
            xs={8}
            lg={9}
          >
            {!!currentAccount && !!currentMessage && (
              <MessageDisplay
                account={currentAccount}
                message={messagess[currentAccount].find(a => a.id === currentMessage)}
              />
            )}
          </Col>
        </Row>
      </div>
    )
);

const App = connect(null, { loadLocalmail, loadLocalmails })(({
  match: {
    params: { account },
  },
  loadLocalmail,
  loadLocalmails,
}) => {
  if (account) {
    loadLocalmail(account);
  } else {
    loadLocalmails();
  }
  return <Localmail />;
});

export default App;
