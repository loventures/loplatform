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
import { FormGroup, Input, Label, ListGroup, ListGroupItem } from 'reactstrap';

import { AdminFormField, AdminFormFile, AdminFormSelect } from '../../components/adminForm';

class AccessCodeBatch extends React.Component {
  constructor(props) {
    super(props);
    this.baseFormState = {
      uploadInfo: {},
      csvRows: [],
      numRows: 0,
      csvError: null,
      skipFirst: true,
    };
    this.state = {
      generating: props.canGenerate,
      ...this.baseFormState,
    };
  }

  renderName = () => {
    const { validationErrors, T } = this.props;
    return (
      <AdminFormField
        key={'name'}
        entity="accessCodes"
        field={'name'}
        required={true}
        autoFocus={true}
        invalid={validationErrors['name']}
        T={T}
      />
    );
  };

  renderDuration = () => {
    const { T } = this.props;
    const durations = ['unlimited', '1 day', '1 month', '3 months', '6 months', '12 months'];
    const options = durations.map(dur => ({
      key: dur,
      id: dur,
      text: T.t(`adminPage.accessCodes.duration.${dur.replace(/\s/g, '')}`),
    }));
    return (
      <AdminFormSelect
        required={true}
        entity="accessCodes"
        field="duration"
        options={options}
        T={T}
      />
    );
  };

  generateOrImportChange = () => {
    this.setState({
      generating: !this.state.generating,
      ...this.baseFormState,
    });
  };

  renderGenerateOrImport = () => {
    const generating = this.state.generating;
    const { T, canGenerate } = this.props;
    return (
      <React.Fragment>
        <input
          type="hidden"
          value="true"
          name={generating ? 'generating' : 'importing'}
        />
        {canGenerate && (
          <FormGroup tag="fieldset">
            <FormGroup check>
              <Input
                type="radio"
                id="accessCodes-generate"
                name="generateImport"
                onChange={this.generateOrImportChange}
                defaultChecked={canGenerate}
              />
              <Label
                check
                id="accessCodes-generate-label"
                for="accessCodes-generate"
              >
                {T.t('adminPage.accessCodes.generateAccessCodes')}
              </Label>
            </FormGroup>
            <FormGroup check>
              <Input
                id="accessCodes-import"
                type="radio"
                name="generateImport"
                onChange={this.generateOrImportChange}
                defaultChecked={!canGenerate}
              />
              <Label
                check
                id="accessCodes-import-label"
                for="accessCodes-import"
              >
                {T.t('adminPage.accessCodes.importAccessCodes')}
              </Label>
            </FormGroup>
          </FormGroup>
        )}
      </React.Fragment>
    );
  };

  renderPrefixAndQuantity = () => {
    const { T, validationErrors } = this.props;
    return (
      <React.Fragment>
        <AdminFormField
          key={'prefix'}
          entity="accessCodes"
          field={'prefix'}
          invalid={validationErrors['prefix']}
          value="DE"
          required={true}
          T={T}
        />
        <AdminFormField
          key={'quantity'}
          value={'1'}
          entity="accessCodes"
          field={'quantity'}
          required={true}
          invalid={validationErrors['quantity']}
          T={T}
        />
      </React.Fragment>
    );
  };

  onCsvChange = data => {
    const { onModalErrorChange, T, componentIdentifier } = this.props;
    console.log(data);
    if (data.error) {
      onModalErrorChange({
        field: 'csv',
        message: data.error,
      });
      console.log(data.error);
    } else if (data.guid) {
      const guid = data.guid;
      const url = `/api/v2/accessCodes/batchComponents/${componentIdentifier}/instance/validateUpload?upload=${guid}`;
      axios
        .get(url)
        .then(res => {
          if (res.data.error) {
            onModalErrorChange({
              field: 'csv',
              message: T.t(res.data.error),
            });
            this.setState({
              csvError: res.data.error,
            });
            console.log(res.data.error);
          } else {
            onModalErrorChange(null);
            this.setState({
              uploadInfo: data.value,
              csvRows: res.data.data,
              numRows: res.data.rows,
              csvError: null,
            });
          }
        })
        .catch(err => {
          onModalErrorChange({
            field: 'csv',
            message: T.t('adminPage.accessCodes.uploadCsv.validationError.unexpected'),
          });
          console.log(err);
        });
    }
  };

  renderCsv = () => {
    const { T, validationErrors } = this.props;
    const { csvRows, numRows, csvError, skipFirst } = this.state;
    const diff = numRows - csvRows.length;
    const moreRows = diff > 0;
    return (
      <React.Fragment>
        <input
          type="hidden"
          value={this.state.uploadInfo.guid ?? ''}
          name={'guid'}
        />
        <AdminFormFile
          key="csv"
          required={true}
          entity="accessCodes"
          field="csv"
          onChange={this.onCsvChange}
          invalid={csvError || validationErrors['csv']}
          accept={['.csv']}
          T={T}
        />
        <ListGroup>
          {csvRows.map((row, idx) => (
            <ListGroupItem
              key={idx}
              className={skipFirst && !idx ? 'font-weight-bold line-through' : undefined}
            >
              {row}
            </ListGroupItem>
          ))}
          {moreRows && <ListGroupItem>{`... ${diff} more rows`}</ListGroupItem>}
        </ListGroup>
        {csvRows.length > 0 && (
          <FormGroup
            check
            style={{ marginTop: 15 }}
          >
            <Label check>
              <Input
                type="checkbox"
                id="accessCodes-skipFirstRow"
                name="skipFirstRow"
                checked={skipFirst}
                onChange={e => this.setState({ skipFirst: e.target.checked })}
              />{' '}
              Skip first row
            </Label>
          </FormGroup>
        )}
      </React.Fragment>
    );
  };

  render() {
    const { generating } = this.state;
    return (
      <React.Fragment>
        {this.renderName()}
        {this.props.extraFormFields && this.props.extraFormFields(this.props.validationErrors)}
        {this.props.hasDuration && this.renderDuration()}
        {this.renderGenerateOrImport()}
        {generating ? this.renderPrefixAndQuantity() : this.renderCsv()}
      </React.Fragment>
    );
  }
}

AccessCodeBatch.propTypes = {
  T: PropTypes.object.isRequired,
  validationErrors: PropTypes.object.isRequired,
  componentIdentifier: PropTypes.string.isRequired,
  onModalErrorChange: PropTypes.func.isRequired,
  extraFormFields: PropTypes.func,
  hasDuration: PropTypes.bool,
  canGenerate: PropTypes.bool,
};

export default AccessCodeBatch;
