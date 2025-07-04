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
import csvjson from 'csvjson';
import PropTypes from 'prop-types';
import React from 'react';
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

class ScormPackage extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      scormFormat: 'CourseEntry',
      connector: null,
      submitting: false,
      invalid: false,
      error: null,
      errorCount: 0,
      productCodes: [],
    };
  }

  singleSubmit = () => {
    const {
      props: { close, row, setPortalAlertStatus, T },
      state: { connector, scormFormat },
    } = this;
    const request = {
      offeringId: row.id,
      systemId: connector.id,
      scormFormat,
    };
    return axios.post('/api/v2/scorm/package', request).then(({ data: guid }) => {
      this.download(guid);
      setPortalAlertStatus(
        false,
        true,
        T.t('adminPage.courseOfferings.scormPackageModal.downloadPending')
      );
      close();
    });
  };

  multiSubmit = () => {
    const {
      props: { close, setPortalAlertStatus, T },
      state: { connector, scormFormat, productCodes },
    } = this;
    const request = {
      productCodes,
      systemId: connector.id,
      scormFormat,
    };
    return asjax('/api/v2/scorm/package/batch', request, () => {}).then(guid => {
      this.download(guid);
      setPortalAlertStatus(
        false,
        true,
        T.t('adminPage.courseOfferings.scormPackageModal.downloadPending')
      );
      close();
    });
  };

  onSubmit = e => {
    e.preventDefault();
    const {
      props: { multi, T },
      state: { connector },
    } = this;
    if (!connector) {
      this.setState(({ errorCount }) => ({
        error: T.t('adminForm.alert.formError'),
        invalid: true,
        submitting: false,
        errorCount: 1 + errorCount,
      }));
    } else {
      this.setState({ error: null, invalid: false, submitting: true });
      (multi ? this.multiSubmit : this.singleSubmit)().catch(err => {
        console.log(err);
        this.setState(({ errorCount }) => ({
          error: T.t('error.unexpectedError'),
          submitting: false,
          errorCount: 1 + errorCount,
        }));
      });
    }
  };

  download = guid => {
    const { multi } = this.props;
    const a = document.createElement('a');
    a.target = '_blank';
    a.innerHTML = 'dl';
    a.href = `/api/v2/scorm/package/${multi ? 'batch/' : ''}${guid}`;
    a.onclick = event => document.body.removeChild(event.target);
    a.style.display = 'none';
    document.body.appendChild(a);
    a.click();
  };

  onScormCsv = file => {
    const setError = error =>
      this.setState(({ errorCount }) => ({ error, errorCount: 1 + errorCount }));
    const fr = new FileReader();
    fr.onload = () => {
      try {
        const rows = csvjson.toArray(fr.result);
        if (rows.length < 2) return setError(`Only found ${rows.length} rows`);
        const header = rows[0];
        const index = header.indexOf('Product Code');
        if (index < 0) return setError(`No "Product Code" header in: ${header.join(', ')}`);
        const productCodes = rows
          .slice(1)
          .map(row => row[index])
          .filter(pc => typeof pc === 'string');
        this.setState({ productCodes });
        setError(null);
      } catch (e) {
        console.log(e);
        setError('An unknown error occurred');
      }
    };
    fr.onerror = () => setError('Error reading file');
    fr.readAsText(file);
  };

  render() {
    const { row, T, close, multi } = this.props;
    const { connector, scormFormat, submitting, invalid, error, errorCount, productCodes } =
      this.state;
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
          onSubmit={this.onSubmit}
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
              onChange={connector => this.setState({ connector })}
              invalid={
                invalid
                  ? T.t('adminForm.validation.fieldIsRequired', {
                      field: T.t(`${baseName}.fieldName.connector`),
                    })
                  : null
              }
              matrixFilter={value => ({ property: 'name', operator: 'co', value })}
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
                      onClick={() => this.setState({ scormFormat: nav })}
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
                onChange={this.onScormCsv}
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
  }
}

ScormPackage.propTypes = {
  multi: PropTypes.bool.isRequired,
  row: PropTypes.object,
  T: PropTypes.object.isRequired,
  close: PropTypes.func.isRequired,
  setPortalAlertStatus: PropTypes.func.isRequired,
  lo_platform: PropTypes.object.isRequired,
};

export default ScormPackage;
