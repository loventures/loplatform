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
import React, { useState } from 'react';
import { FormGroup, Input, Label, ListGroup, ListGroupItem } from 'reactstrap';

import { AdminFormField, AdminFormFile, AdminFormSelect } from '../../components/adminForm';

import type { AxiosResponse } from 'axios';

export type ValidationErrors = Record<string, string | undefined>;

export interface ModalError {
  field: string;
  message: string;
}

/** The flat form object that ReactTable collects and passes to validation. */
export type AccessCodeForm = Record<string, any>;

/** Result of a batch type's `validateForm`: either errors or the data payload. */
export type ValidateFormResult =
  | { validationErrors: ValidationErrors }
  | { data: Record<string, unknown> };

/** A batch-type descriptor as consumed by the AccessCodeTypes registry. */
export interface AccessCodeBatchType {
  component: React.ComponentType<any>;
  validateForm: (form: AccessCodeForm, T: Polyglot, modalError?: ModalError | null) => ValidateFormResult;
  afterCreateOrUpdate: (res: AxiosResponse, form: AccessCodeForm) => Promise<AxiosResponse>;
  id: string;
}

export interface AccessCodeBatchProps {
  T: Polyglot;
  validationErrors: ValidationErrors;
  componentIdentifier: string;
  onModalErrorChange: (err: ModalError | null) => void;
  extraFormFields?: (validationErrors: ValidationErrors) => React.ReactNode;
  hasDuration?: boolean;
  canGenerate?: boolean;
  // `type` is forwarded by the per-type wrappers but unused here.
  type?: string;
}

interface UploadValue {
  guid?: string;
  [key: string]: unknown;
}

interface CsvChangeData {
  error?: string;
  guid?: string;
  value?: UploadValue;
}

const AccessCodeBatch: React.FC<AccessCodeBatchProps> = props => {
  const { T, validationErrors, componentIdentifier, onModalErrorChange, canGenerate, hasDuration } =
    props;

  const baseFormState = {
    uploadInfo: {} as UploadValue,
    csvRows: [] as string[],
    numRows: 0,
    csvError: null as string | null,
    skipFirst: true,
  };

  const [generating, setGenerating] = useState<boolean>(!!canGenerate);
  const [uploadInfo, setUploadInfo] = useState<UploadValue>(baseFormState.uploadInfo);
  const [csvRows, setCsvRows] = useState<string[]>(baseFormState.csvRows);
  const [numRows, setNumRows] = useState(baseFormState.numRows);
  const [csvError, setCsvError] = useState<string | null>(baseFormState.csvError);
  const [skipFirst, setSkipFirst] = useState(baseFormState.skipFirst);

  const resetBaseFormState = () => {
    setUploadInfo(baseFormState.uploadInfo);
    setCsvRows(baseFormState.csvRows);
    setNumRows(baseFormState.numRows);
    setCsvError(baseFormState.csvError);
    setSkipFirst(baseFormState.skipFirst);
  };

  const renderName = () => {
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

  const renderDuration = () => {
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

  const generateOrImportChange = () => {
    setGenerating(!generating);
    resetBaseFormState();
  };

  const renderGenerateOrImport = () => {
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
                onChange={generateOrImportChange}
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
                onChange={generateOrImportChange}
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

  const renderPrefixAndQuantity = () => {
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

  const onCsvChange = (data: CsvChangeData) => {
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
            setCsvError(res.data.error);
            console.log(res.data.error);
          } else {
            onModalErrorChange(null);
            setUploadInfo(data.value ?? {});
            setCsvRows(res.data.data);
            setNumRows(res.data.rows);
            setCsvError(null);
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

  const renderCsv = () => {
    const diff = numRows - csvRows.length;
    const moreRows = diff > 0;
    return (
      <React.Fragment>
        <input
          type="hidden"
          value={uploadInfo.guid ?? ''}
          name={'guid'}
        />
        <AdminFormFile
          key="csv"
          required={true}
          entity="accessCodes"
          field="csv"
          onChange={onCsvChange}
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
                onChange={e => setSkipFirst(e.target.checked)}
              />{' '}
              Skip first row
            </Label>
          </FormGroup>
        )}
      </React.Fragment>
    );
  };

  return (
    <React.Fragment>
      {renderName()}
      {props.extraFormFields && props.extraFormFields(validationErrors)}
      {hasDuration && renderDuration()}
      {renderGenerateOrImport()}
      {generating ? renderPrefixAndQuantity() : renderCsv()}
    </React.Fragment>
  );
};

export default AccessCodeBatch;
