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
import csv from 'csvtojson';
import Polyglot from 'node-polyglot';
import React, { useState } from 'react';
import {
  Button,
  ButtonGroup,
  Col,
  FormGroup,
  Label,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from 'reactstrap';

import { AdminFormFile } from '../components/adminForm';
import AdminFormCombobox from '../components/adminForm/AdminFormCombobox';
import ModalBar from '../components/reactTable/ModalBar';
import WaitDotGif from '../components/WaitDotGif';
import ScormConnector from '../connectors/ConnectorTypes/Scorm';
import { asjax } from '../services';
import { LoPlatform } from '../types/loPlatform';

interface ScormRow {
  id: number;
  [key: string]: unknown;
}

interface Connector {
  id: number;
  [key: string]: unknown;
}

interface ScormPackageProps {
  multi: boolean;
  row?: ScormRow;
  T: Polyglot;
  close: () => void;
  setPortalAlertStatus: (error: boolean, success: boolean, message: string) => void;
  lo_platform: LoPlatform;
}

const ScormPackage: React.FC<ScormPackageProps> = ({ row, T, close, multi, setPortalAlertStatus }) => {
  const [scormFormat, setScormFormat] = useState('CourseEntry');
  const [connector, setConnector] = useState<Connector | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [invalid, setInvalid] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [errorCount, setErrorCount] = useState(0);
  const [productCodes, setProductCodes] = useState<string[]>([]);

  const download = (guid: string) => {
    const a = document.createElement('a');
    a.target = '_blank';
    a.innerHTML = 'dl';
    a.href = `/api/v2/scorm/package/${multi ? 'batch/' : ''}${guid}`;
    a.onclick = event => document.body.removeChild(event.target as Node);
    a.style.display = 'none';
    document.body.appendChild(a);
    a.click();
  };

  const singleSubmit = () => {
    const request = {
      offeringId: row!.id,
      systemId: connector!.id,
      scormFormat,
    };
    return axios.post('/api/v2/scorm/package', request).then(({ data: guid }) => {
      download(guid);
      setPortalAlertStatus(
        false,
        true,
        T.t('adminPage.courseOfferings.scormPackageModal.downloadPending')
      );
      close();
    });
  };

  const multiSubmit = () => {
    const request = {
      productCodes,
      systemId: connector!.id,
      scormFormat,
    };
    return asjax('/api/v2/scorm/package/batch', request, () => {}).then(guid => {
      download(guid as string);
      setPortalAlertStatus(
        false,
        true,
        T.t('adminPage.courseOfferings.scormPackageModal.downloadPending')
      );
      close();
    });
  };

  const onSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!connector) {
      setError(T.t('adminForm.alert.formError'));
      setInvalid(true);
      setSubmitting(false);
      setErrorCount(c => 1 + c);
    } else {
      setError(null);
      setInvalid(false);
      setSubmitting(true);
      (multi ? multiSubmit : singleSubmit)().catch(err => {
        console.log(err);
        setError(T.t('error.unexpectedError'));
        setSubmitting(false);
        setErrorCount(c => 1 + c);
      });
    }
  };

  const onScormCsv = (file: File) => {
    const setErr = (e: string | null) => {
      setError(e);
      setErrorCount(c => 1 + c);
    };
    const fr = new FileReader();
    fr.onload = () => {
      Promise.resolve(
        csv({
          noheader: true,
          output: 'csv',
        }).fromString(fr.result as string)
      )
        .then((rows: string[][]) => {
          if (rows.length < 2) return setErr(`Only found ${rows.length} rows`);
          const header = rows[0];
          const index = header.indexOf('Product Code');
          if (index < 0) return setErr(`No "Product Code" header in: ${header.join(', ')}`);
          const codes = rows
            .slice(1)
            .map(r => r[index])
            .filter(pc => typeof pc === 'string');
          setProductCodes(codes);
          setErr(null);
        })
        .catch((e: unknown) => {
          console.log(e);
          setErr('An unknown error occurred');
        });
    };
    fr.onerror = () => setErr('Error reading file');
    fr.readAsText(file);
  };

  const baseName = 'adminPage.courseOfferings.scormPackageModal';
  const prefilter = [
    { property: 'disabled', operator: 'eq', value: false },
    { property: 'implementation', operator: 'eq', value: ScormConnector.componentId },
  ];
  // `/api/v2/lwc/courseOfferings/${row.id}/scormPackage`,
  return (
    <Modal
      isOpen={true}
      backdrop="static"
      size="lg"
      toggle={close}
      className="crudTable-modal scormPackageModal"
    >
      <form
        id="reactTable-modalForm"
        className="admin-form"
        onSubmit={onSubmit}
      >
        <ModalHeader tag="h2">
          {multi ? T.t(`${baseName}.multiTitle`) : T.t(`${baseName}.title`, row)}
        </ModalHeader>
        <ModalBody>
          {error && (
            <ModalBar
              key={'modal-' + errorCount}
              value={error}
              type="error"
            />
          )}
          <AdminFormCombobox
            entity="courseOfferings.scormPackageModal"
            field="connector"
            required={true}
            T={T}
            labelWidth={3}
            targetEntity="connectors"
            matrixPrefilter={prefilter}
            onChange={(c: Connector | null) => setConnector(c)}
            invalid={
              invalid
                ? T.t('adminForm.validation.fieldIsRequired', {
                    field: T.t(`${baseName}.fieldName.connector`),
                  })
                : undefined
            }
            matrixFilter={(value: string) => ({ property: 'name', operator: 'co', value })}
          />
          <FormGroup
            row
            className="is-required"
          >
            <Label
              lg={3}
              for={`packageFormat-${scormFormat}`}
            >
              {T.t(`${baseName}.label.packageFormat`)}
            </Label>
            <Col lg={9}>
              <ButtonGroup
                vertical
                style={{ width: '100%' }}
              >
                {['CourseEntry', 'CourseWithNavigation'].map(nav => (
                  <Button
                    key={nav}
                    id={`packageFormat-${nav}`}
                    block
                    color={scormFormat === nav ? 'primary' : 'light'}
                    onClick={() => setScormFormat(nav)}
                  >
                    {T.t(`${baseName}.packageFormat.${nav}`)}
                  </Button>
                ))}
              </ButtonGroup>
            </Col>
          </FormGroup>
          {multi ? (
            <AdminFormFile
              entity="courseOfferings.scormPackageModal"
              field="csv"
              required={true}
              T={T}
              labelWidth={3}
              accept={['.csv']}
              onChange={onScormCsv}
              noUpload
              help="A comma-separated CSV file with a Product Code column."
            />
          ) : null}
        </ModalBody>
        <ModalFooter>
          <Button
            id="react-table-close-modal-btn"
            onClick={close}
          >
            {T.t('crudTable.modal.closeButton')}
          </Button>
          <Button
            id="react-table-submit-modal-btn"
            type="submit"
            color="primary"
            disabled={submitting || !connector || (multi && !productCodes.length)}
          >
            {T.t(`${baseName}.submitButton`)}
            {submitting && (
              <WaitDotGif
                className="ms-2 waiting"
                color="light"
                size={16}
              />
            )}
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
};

export default ScormPackage;
