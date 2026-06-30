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

import axios from 'axios';
import Polyglot from 'node-polyglot';
import React, { useRef, useState } from 'react';
import { Button, Col, FormGroup, Input, Label } from 'reactstrap';

import WaitDotGif from '../components/WaitDotGif';

interface LogLevelChangeFormProps {
  T: Polyglot;
  setPortalAlertStatus: (error: boolean, success: boolean, message: string) => void;
}

const LogLevelChangeForm: React.FC<LogLevelChangeFormProps> = ({ T, setPortalAlertStatus }) => {
  const [levelChangeSubmit, setLevelChangeSubmit] = useState(false);
  const [loggerName, setLoggerName] = useState('');
  const [logLevel, setLogLevel] = useState('');
  const [expiresTime, setExpiresTime] = useState('');
  const [checkboxValue, setCheckboxValue] = useState('');
  const allNodesRef = useRef(false);

  const onSubmitLogLevelChange = () => {
    const url = `/api/v2/overlord/logs/logLevelChange`;
    setLevelChangeSubmit(true);
    const payload = {
      name: loggerName,
      level: logLevel || 'INFO',
      expiresIn: parseInt(expiresTime, 10) || null,
      allNodes: allNodesRef.current || false,
    };
    axios(url, {
      method: 'POST',
      data: JSON.stringify(payload),
      headers: {
        'Content-Type': 'application/json',
      },
    })
      .then(() => {
        setPortalAlertStatus(false, true, T.t('adminPage.logs.level.change.succeeded'));
        setLevelChangeSubmit(false);
      })
      .catch(err => {
        setPortalAlertStatus(true, false, T.t('adminPage.logs.level.change.failed'));
        setLevelChangeSubmit(false);
        console.log(err);
      });
  };

  const onLoggerNameChange = (e: React.ChangeEvent<HTMLInputElement>) =>
    setLoggerName(e.target.value);
  const onLogLevelChange = (e: React.ChangeEvent<HTMLInputElement>) => setLogLevel(e.target.value);
  const onLogLevelExpiryTimeChange = (e: React.ChangeEvent<HTMLInputElement>) =>
    setExpiresTime(e.target.value);
  const onCheckboxChange = (e: React.ChangeEvent<HTMLInputElement>) =>
    setCheckboxValue(e.target.value);
  const onCheckboxClick = () => {
    allNodesRef.current = !allNodesRef.current;
  };

  const logLevelOptions = ['SEVERE', 'WARNING', 'INFO', 'FINE', 'FINER', 'FINEST']
    .map(key => {
      return {
        key: key,
        text: T.t(`adminPage.logs.log.level.value.${key}`),
      };
    })
    .map(({ key, text }) => (
      <option
        key={key}
        id={key}
        value={key}
      >
        {text}
      </option>
    ));
  const expirationMap: Record<string, number | string> = {
    never: '',
    fiveMinutes: 5,
    oneHour: 60,
    oneDay: 60 * 24,
  };
  const logChangeExpiryOptions = ['never', 'oneDay', 'oneHour', 'fiveMinutes']
    .map(key => {
      return {
        key: key,
        text: T.t(`adminPage.logs.level.change.expiry.option.${key}`),
      };
    })
    .map(({ key, text }) => (
      <option
        key={key}
        id={key}
        value={expirationMap[key]}
      >
        {text}
      </option>
    ));
  const id = `log-change`;
  return (
    <div>
      <h4
        className="logs-header"
        id={`${id}-header`}
      >
        {T.t(`adminPage.logs.change.log.level`)}
      </h4>
      <FormGroup row>
        <Label
          className="logs-label"
          id={`${id}-label`}
          for="level-name"
          sm={2}
        >
          {T.t('adminPage.logs.log.name.label')}
        </Label>
        <Col sm={4}>
          <Input
            id="level-name"
            type="text"
            onChange={onLoggerNameChange}
          ></Input>
        </Col>
      </FormGroup>
      <FormGroup row>
        <Label
          className="logs-label"
          id={`${id}-label2`}
          for="level-change-state"
          sm={2}
        >
          {T.t('adminPage.logs.log.level.label')}
        </Label>
        <Col sm={4}>
          <Input
            id="level-change-state"
            type="select"
            onChange={onLogLevelChange}
            defaultValue="INFO"
          >
            {logLevelOptions}
          </Input>
        </Col>
      </FormGroup>
      <FormGroup row>
        <Label
          className="logs-label"
          id={`${id}-label3`}
          for="level-change-expiry-time"
          sm={2}
        >
          {T.t('adminPage.logs.log.level.expiry')}
        </Label>
        <Col sm={4}>
          <Input
            id="level-change-expiry-time"
            type="select"
            onChange={onLogLevelExpiryTimeChange}
          >
            {logChangeExpiryOptions}
          </Input>
        </Col>
      </FormGroup>
      <FormGroup row>
        <Col sm={{ size: 4, offset: 2 }}>
          <div className="form-check">
            <input
              className="form-check-input"
              id="all-nodes-checkbox"
              type="checkbox"
              onChange={onCheckboxChange}
              value={checkboxValue}
              onClick={onCheckboxClick}
            />
            <Label
              className="form-check-label logs-label"
              id={`${id}-cluster`}
              for="all-nodes-checkbox"
            >
              {T.t('adminPage.logs.cluster.label')}
            </Label>
          </div>
        </Col>
        <Col sm={2}>
          <Button
            color="primary"
            id={`${id}-submit`}
            onClick={onSubmitLogLevelChange}
          >
            {T.t('adminPage.logs.submit.log.level.change')}
            {levelChangeSubmit && (
              <WaitDotGif
                className="ms-2 waiting"
                color="light"
                size={16}
              />
            )}
          </Button>
        </Col>
      </FormGroup>
      <hr className="logs-hr" />
    </div>
  );
};

export default LogLevelChangeForm;
