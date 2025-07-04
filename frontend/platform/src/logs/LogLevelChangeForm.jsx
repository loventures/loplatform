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
import PropTypes from 'prop-types';
import React from 'react';
import { Button, Col, FormGroup, Input, Label } from 'reactstrap';

import WaitDotGif from '../components/WaitDotGif';

class LogLevelChangeForm extends React.Component {
  state = {
    levelChangeSubmit: false,
  };
  onSubmitLogLevelChange = () => {
    const { loggerName, logLevel, expiresTime } = this.state;
    const url = `/api/v2/overlord/logs/logLevelChange`;
    this.setState({ levelChangeSubmit: true });
    const payload = {
      name: loggerName,
      level: logLevel || 'INFO',
      expiresIn: parseInt(expiresTime, 10) || null,
      allNodes: this.checkboxValue || false,
    };
    const { setPortalAlertStatus, T } = this.props;
    axios(url, {
      method: 'POST',
      data: JSON.stringify(payload),
      headers: {
        'Content-Type': 'application/json',
      },
    })
      .then(() => {
        setPortalAlertStatus(false, true, T.t('adminPage.logs.level.change.succeeded'));
        this.setState({ levelChangeSubmit: false });
      })
      .catch(err => {
        setPortalAlertStatus(true, false, T.t('adminPage.logs.level.change.failed'));
        this.setState({ levelChangeSubmit: false });
        console.log(err);
      });
  };
  onLoggerNameChange = e => this.setState({ loggerName: e.target.value });
  onLogLevelChange = e => this.setState({ logLevel: e.target.value });
  onLogLevelExpiryTimeChange = e => this.setState({ expiresTime: e.target.value });
  onCheckboxChange = e => this.setState({ checkboxValue: e.target.value });
  onCheckboxClick = () => {
    this.checkboxValue = !this.checkboxValue;
  };
  render() {
    const { T } = this.props;
    const { levelChangeSubmit } = this.state;
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
    const expirationMap = { never: '', fiveMinutes: 5, oneHour: 60, oneDay: 60 * 24 };
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
              onChange={this.onLoggerNameChange}
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
              onChange={this.onLogLevelChange}
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
              onChange={this.onLogLevelExpiryTimeChange}
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
                onChange={this.onCheckboxChange}
                value={this.state.checkboxValue}
                onClick={this.onCheckboxClick}
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
              onClick={this.onSubmitLogLevelChange}
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
  }
}

LogLevelChangeForm.propTypes = {
  T: PropTypes.object.isRequired,
  setPortalAlertStatus: PropTypes.func.isRequired,
};

export default LogLevelChangeForm;
