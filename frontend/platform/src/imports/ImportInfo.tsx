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
import moment from 'moment-timezone';
import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';
import { Badge, Button, Col, Modal, ModalBody, ModalFooter, ModalHeader, Row } from 'reactstrap';

import { inCurrTimeZone } from '../services/moment';

interface ImportInfoProps {
  importId: number;
  T: Polyglot;
  close: () => void;
  setPortalAlertStatus: (error: boolean, success: boolean, message: string) => void;
}

const ImportInfo: React.FC<ImportInfoProps> = ({
  importId,
  T,
  close,
  setPortalAlertStatus,
}) => {
  const [loaded, setLoaded] = useState(false);
  const [importInfo, setImportInfo] = useState<Record<string, any>>({});

  useEffect(() => {
    axios
      .get(`/api/v2/imports/${importId}`)
      .then(res => {
        const info = res.data;
        info.startedBy = info.startedBy
          ? info.startedBy.fullName || info.startedBy.name
          : T.t('user.unknown');
        setImportInfo(info);
        setLoaded(true);
      })
      .catch(err => {
        console.log(err);
        setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const formatFile = (file: { fileName: string }) => {
    return (
      <a
        href={`/api/v2/imports/${importId}/file/view?download=true`}
        download
      >
        {file.fileName}
      </a>
    );
  };

  const formatTime = (t: string) => {
    const dateTimeFormat = T.t('format.dateTime.compact');
    const formatted = inCurrTimeZone(moment(t)).format(dateTimeFormat);
    const fromNow = moment(t).fromNow();
    const params = { formatted: formatted, fromNow: fromNow };
    return T.t('adminPage.imports.importInfo.times', params);
  };

  const formatCounts = (count: number, field: string) => {
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

  const renderField = (field: string, format?: (value: any, field: string) => React.ReactNode) => {
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
        tag="h2"
      >
        {T.t('adminPage.imports.importInfo.header', importInfo)}
      </ModalHeader>
      <ModalBody>
        {['identifier', 'status'].map(field => renderField(field, undefined))}
        {['startTime', 'endTime'].map(field => renderField(field, formatTime))}
        {renderField('startedBy')}
        {renderField('importFile', formatFile)}
        {['total', 'successCount', 'failureCount'].map(field => {
          return renderField(field, formatCounts);
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
};

export default ImportInfo;
