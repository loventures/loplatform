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

import axios from 'axios';
import { get } from 'lodash';
import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Col, Modal, ModalBody, ModalHeader } from 'reactstrap';

import Octo from '../components/Octo';

export default (T, perform, payload, onError) => {
  const [evSrc, setEvSrc] = useState(null);
  const [status, setStatus] = useState({
    create: false,
    init: false,
    profile: false,
    account: false,
    appearance: false,
    dns: false,
    done: false,
  });
  const [result, setResult] = useState(null);

  const onMessage =
    statusAcc =>
    ({ data }) => {
      const msg = JSON.parse(data);
      if (msg.status === 'warning') {
        const key = msg.body.message;
        if (!statusAcc[key]) {
          statusAcc[key] = true;
          setStatus(statusAcc);
        }
      } else if (msg.status === 'ok') {
        setResult(msg.body);
      } else if (msg.status === 'error') {
        onError();
      }
    };

  const className = prop => {
    const activeClass = () =>
      Object.keys(status).filter(p => !status[p])[0] === prop ? 'active' : 'inactive';
    return status[prop] ? 'success' : status.done ? 'failure' : activeClass();
  };

  useEffect(() => {
    if (perform && !evSrc) {
      axios
        .post('/api/v2/domains/provision', payload)
        .then(({ data: { channel } }) => {
          const statusAcc = { ...status }; // accumulator for async event listener
          const es = new EventSource(`event${channel}`);
          es.addEventListener(channel, onMessage(statusAcc));
          setEvSrc(es);
        })
        .catch(onError);
    }
  });

  const dnsClass = className('dns');
  return (
    <Col className="provision-form">
      <div>
        <ol>
          {Object.keys(status)
            .slice(0, 6)
            .map(prop => (
              <li
                key={prop}
                className={className(prop)}
                id={`step-${prop}`}
              >
                {T.t(`adminPage.provision.step4.${prop}`)}
              </li>
            ))}
        </ol>
      </div>
      <Modal
        className="provision-modal success modal-dialog-centered"
        isOpen={status.done && result}
      >
        <ModalHeader>{T.t('adminPage.provision.success.created')}</ModalHeader>
        <ModalBody>
          <div>
            <span className="key">{T.t('adminSection.domain.name')}:</span>
            <span
              id="span-did"
              className="value"
            >
              {get(result, 'domain.domainId', '')}&nbsp; ({get(result, 'domain.shortName', '')})
            </span>
          </div>
          <div className="sub">
            <span className="key">{T.t('adminPage.provision.success.url')}:</span>
            <span
              id="span-url"
              className="value"
            >
              https://{get(result, 'domain.hostName', '')}
            </span>
          </div>
          <div>
            <span className="key">{T.t('adminPage.provision.success.administrator')}:</span>
            <span
              id="span-name"
              className="value"
            >
              {get(result, 'account.givenName', '')}&nbsp;
              {get(result, 'account.middleName', '')}&nbsp;
              {get(result, 'account.familyName', '')}
            </span>
          </div>
          <div className="sub">
            <span className="key">{T.t('adminPage.users.fieldName.userName')}:</span>
            <span
              id="span-email"
              className="value"
            >
              {get(result, 'account.userName', '')}
            </span>
          </div>
          <div className="sub">
            <span className="key">{T.t('adminPage.users.fieldName.password')}:</span>
            <span
              id="span-pwd"
              className="value"
            >
              {get(result, 'password', '')}
            </span>
          </div>
          <div>
            <span className="key">{T.t('adminPage.provision.success.dns')}:</span>
            <span
              id="span-dns"
              className={`value ${dnsClass}`}
            >
              {T.t(`adminPage.provision.success.${dnsClass}`)}
            </span>
          </div>
        </ModalBody>
        <Link
          to="/"
          className="btn btn-warning"
        >
          {T.t('adminPage.provision.success.awesome')}
        </Link>
        <Octo />
      </Modal>
    </Col>
  );
};
