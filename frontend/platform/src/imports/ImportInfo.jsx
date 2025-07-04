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
import moment from 'moment-timezone';
import PropTypes from 'prop-types';
import React from 'react';
import { Badge, Button, Col, Modal, ModalBody, ModalFooter, ModalHeader, Row } from 'reactstrap';

import { inCurrTimeZone } from '../services/moment.js';

class ImportInfo extends React.Component {
  state = {
    loaded: false,
    importInfo: {},
  };

  componentDidMount() {
    const { importId, T, setPortalAlertStatus } = this.props;
    axios
      .get(`/api/v2/imports/${importId}`)
      .then(res => {
        const importInfo = res.data;
        importInfo.startedBy = importInfo.startedBy
          ? importInfo.startedBy.fullName || importInfo.startedBy.name
          : T.t('user.unknown');
        this.setState({ importInfo: importInfo, loaded: true });
      })
      .catch(err => {
        console.log(err);
        setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
  }

  formatFile = file => {
    const { importId } = this.props;
    return (
      <a
        href={`/api/v2/imports/${importId}/file/view?download=true`}
        download
      >
        {file.fileName}
      </a>
    );
  };

  formatTime = t => {
    const { T } = this.props;
    const dateTimeFormat = T.t('format.dateTime.compact');
    const formatted = inCurrTimeZone(moment(t)).format(dateTimeFormat);
    const fromNow = moment(t).fromNow();
    const params = { formatted: formatted, fromNow: fromNow };
    return T.t('adminPage.imports.importInfo.times', params);
  };

  formatCounts = (count, field) => {
    const { importId, T } = this.props;
    const error = field === 'failureCount' && count > 0;
    return (
      <React.Fragment>
        <Badge
          color={error ? 'danger' : 'secondary'}
          className="me-1"
        >
          {count}
        </Badge>
        {error && (
          <a
            href={`/api/v2/imports/${importId}/errors/download`}
            download
          >
            {T.t('adminPage.imports.importInfo.errorCsv.name')}
          </a>
        )}
      </React.Fragment>
    );
  };

  renderField = (field, format) => {
    const { T } = this.props;
    const { importInfo } = this.state;
    const id = `imports-details-modal-${field}`;
    return (
      <Row key={id}>
        <Col sm={4}>
          <label>
            <strong>{T.t(`adminPage.imports.importInfo.${field}.label`)}</strong>:
          </label>
        </Col>
        <Col
          sm={8}
          id={id}
        >
          {format ? format(importInfo[field], field) : importInfo[field]}
        </Col>
      </Row>
    );
  };

  render() {
    const { T, close } = this.props;
    const { importInfo, loaded } = this.state;
    if (!loaded) return null;
    const id = 'imports-details-modal';
    return (
      <Modal
        id={id}
        isOpen={true}
        backdrop="static"
        size="lg"
      >
        <ModalHeader
          id={`${id}-header`}
          toggle={this.toggle}
          tag="h2"
        >
          {T.t('adminPage.imports.importInfo.header', importInfo)}
        </ModalHeader>
        <ModalBody>
          {['identifier', 'status'].map(field => this.renderField(field, null))}
          {['startTime', 'endTime'].map(field => this.renderField(field, this.formatTime))}
          {this.renderField('startedBy')}
          {this.renderField('importFile', this.formatFile)}
          {['total', 'successCount', 'failureCount'].map(field => {
            return this.renderField(field, this.formatCounts);
          })}
        </ModalBody>
        <ModalFooter>
          <Button
            id={`${id}-close`}
            color="secondary"
            onClick={close}
          >
            {T.t('adminPage.imports.importInfo.close')}
          </Button>
        </ModalFooter>
      </Modal>
    );
  }
}

ImportInfo.propTypes = {
  importId: PropTypes.number.isRequired,
  T: PropTypes.object.isRequired,
  close: PropTypes.func.isRequired,
  setPortalAlertStatus: PropTypes.func.isRequired,
};

export default ImportInfo;
