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

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useParams } from 'react-router-dom';
import { Alert, Col, Row } from 'reactstrap';
import _ from 'underscore';

import { useTranslations } from '../redux/state';
import Loading from '../etc/EtcLoading';
import MessageDisplay from './MessageDisplay';
import { loadLocalmail, loadLocalmails, LocalmailState } from './reducks';
import Selector from './Selector';

interface LocalmailRootState {
  localmail: LocalmailState;
}

const Localmail: React.FC = () => {
  const T = useTranslations();
  const { currentAccount, currentMessage, error, loaded, messagess } = useSelector(
    (state: LocalmailRootState) => state.localmail
  );

  if (error) {
    return (
      <div
        id="localmail-container"
        className="container-fluid"
      >
        <Alert
          color="danger"
          id="localmail-error"
        >
          {T.t(error as string)}
        </Alert>
      </div>
    );
  } else if (!loaded) {
    return <Loading />;
  } else if (_.isEmpty(messagess)) {
    return (
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
    );
  } else {
    return (
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
                message={messagess[currentAccount].find(a => a.id === currentMessage)!}
              />
            )}
          </Col>
        </Row>
      </div>
    );
  }
};

const App: React.FC = () => {
  const dispatch = useDispatch();
  const { account } = useParams<{ account: string }>();

  useEffect(() => {
    if (account) {
      dispatch(loadLocalmail(account) as any);
    } else {
      dispatch(loadLocalmails() as any);
    }
  }, [account, dispatch]);

  return <Localmail />;
};

export default App;
