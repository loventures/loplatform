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
import classNames from 'classnames';
import moment from 'moment-timezone';
import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';
import { Button, Col, FormFeedback, FormGroup, Input, Label, Row } from 'reactstrap';

import {
  AdminFormCheck,
  AdminFormDateTime,
  AdminFormField,
  AdminFormSection,
  AdminFormSelect,
} from '../../components/adminForm';
import { LoPlatform } from '../../types/loPlatform';
import { inCurrTimeZone } from '../../services/moment';
import EditAddLwSection from '../EditAddLwSection';

interface Integration {
  id?: number;
  connector_id?: string;
  uniqueId: string;
}

interface NamedEntity {
  id: number;
  name: string;
}

interface CourseSectionRow {
  id?: number;
  endDate?: string;
  selfStudy?: boolean;
  subtenant_id?: number;
  externalId?: string;
  configuredShutdownDate?: string;
  fjœr?: boolean;
  [key: string]: unknown;
}

interface ColumnConfig {
  dataField: string;
  required?: boolean;
  [key: string]: unknown;
}

interface EditAddFormProps {
  row: CourseSectionRow;
  columns: ColumnConfig[];
  validationErrors: Record<string, string>;
  translations: Polyglot;
  setPortalAlertStatus: (error: boolean, success: boolean, message: string) => void;
  externalSystems: NamedEntity[];
  subtenants: NamedEntity[];
  lo_platform: LoPlatform;
  fjœr?: boolean;
}

interface EditAddFormComponent extends React.FC<EditAddFormProps> {
  validateForm: (
    form: Record<string, any>,
    row: CourseSectionRow,
    el: HTMLFormElement,
    T: Polyglot
  ) => any;
}

const EditAddForm: EditAddFormComponent = props => {
  const {
    columns,
    validationErrors,
    row,
    translations: T,
    fjœr,
    setPortalAlertStatus,
    subtenants,
    externalSystems,
    lo_platform,
  } = props;
  const [loaded, setLoaded] = useState(false);
  const [integrations, setIntegrations] = useState<Integration[]>([]);
  const [endDate, setEndDate] = useState<string | undefined>(row.endDate);
  const [selfStudy, setSelfStudy] = useState<boolean | undefined>(row.selfStudy);
  const [reviewPeriodOffset, setReviewPeriodOffset] = useState(0);
  const [betaSelfStudy, setBetaSelfStudy] = useState(false);

  const genericError = (e: unknown) => {
    console.log(e);
    setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
  };

  useEffect(() => {
    if (row.id) {
      const fetchen = [
        axios.get(`/api/v2/courseSections/${row.id}`),
        axios.get(`/api/v2/courseSections/configuration`),
      ];
      Promise.all(fetchen)
        .then(([crsRes, cfRes]) => {
          setLoaded(true);
          setIntegrations(crsRes.data.integrations);
          setReviewPeriodOffset(cfRes.data.reviewPeriodOffset);
          setBetaSelfStudy(cfRes.data.betaSelfStudy);
        })
        .catch(genericError);
    } else {
      axios
        .get(`/api/v2/courseSections/configuration`)
        .then(({ data }) => {
          setLoaded(true);
          setIntegrations([{ connector_id: '', uniqueId: '' }]);
          setReviewPeriodOffset(data.reviewPeriodOffset);
          setBetaSelfStudy(data.betaSelfStudy);
        })
        .catch(genericError);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const renderSectionDetails = () => {
    const fields = ['groupId', 'name'];
    return fields.map(field => {
      const col = columns.find(c => c.dataField === field);
      return (
        <AdminFormField
          key={field}
          entity="courseSections"
          field={field}
          value={row[field] as string}
          invalid={validationErrors[field]}
          required={col && col.required}
          T={T}
          autoFocus={!!(row && row.id && field === 'groupId')}
        />
      );
    });
  };

  const updateDefaultCourseName = (name?: string, originName?: string) => {
    // if the course name is blank or matches the previous origin course then update it
    const nameEl = document.getElementById('courseSections-name') as HTMLInputElement | null;
    if (nameEl && (!nameEl.value || (originName && nameEl.value === originName))) {
      nameEl.value = name || '';
    }
  };

  const renderSectionDates = () => {
    const fields = selfStudy
      ? []
      : ['startDate', 'endDate'].concat(endDate ? ['shutdownDate'] : []);
    return (
      <React.Fragment>
        {fields.map(field => {
          const col = columns.find(c => c.dataField === field) || ({} as ColumnConfig);
          const help =
            field === 'shutdownDate'
              ? T.t(`adminPage.courseSections.fieldHelp.${field}`)
              : undefined;
          const value =
            field === 'shutdownDate'
              ? (row.configuredShutdownDate as string)
              : (row[field] as string);
          const placeholder =
            field === 'shutdownDate'
              ? inCurrTimeZone(moment(endDate).add(reviewPeriodOffset, 'h')).format(
                  T.t('format.dateTime.full')
                )
              : '';
          return (
            <AdminFormDateTime
              key={field}
              help={help}
              entity="courseSections"
              field={field}
              value={value}
              invalid={validationErrors[field]}
              placeholder={placeholder}
              onChange={d => {
                if (field === 'endDate') setEndDate(d as string);
              }}
              T={T}
              required={col.required}
            />
          );
        })}
        {betaSelfStudy && fjœr ? (
          <AdminFormCheck
            entity="courseSections"
            field="selfStudy"
            value={row.selfStudy}
            T={T}
            onChange={e => setSelfStudy(e.target.checked)}
          />
        ) : null}
      </React.Fragment>
    );
  };

  const renderSystemOption = () => {
    return externalSystems.map(system => {
      return (
        <option
          key={system.id}
          value={system.id}
        >
          {system.name}
        </option>
      );
    });
  };

  const handleSystemChange = (event: React.ChangeEvent<HTMLInputElement>, idx: number) => {
    const next = [...integrations];
    next[idx].connector_id = event.target.value;
    setIntegrations(next);
  };

  const handleUniqueIdChange = (event: React.ChangeEvent<HTMLInputElement>, index: number) => {
    const next = [...integrations];
    next[index].uniqueId = event.target.value;
    setIntegrations(next);
  };

  const addUniqueId = () => {
    setIntegrations(prev => [...prev, { uniqueId: '', connector_id: '' }]);
  };

  const removeUniqueId = (idx: number) => {
    setIntegrations(prev => {
      const next = [...prev];
      next.splice(idx, 1);
      return next;
    });
  };

  const renderIntegrationRow = (integration: Integration, index: number) => {
    return [
      <Col
        xs={4}
        key="systemId"
      >
        <input
          type="hidden"
          name={`integrationId-${index}`}
          value={integration.id || ''}
        />
        <Input
          id={'system-' + index}
          type="select"
          name={`systemId-${index}`}
          onChange={e => handleSystemChange(e, index)}
          defaultValue={integration.connector_id || ''}
        >
          <option value=""></option>
          {renderSystemOption()}
        </Input>
      </Col>,
      <Col
        xs={6}
        key="uniqueId"
      >
        <Input
          id={'uniqueId-' + index}
          type="text"
          name={`uniqueId-${index}`}
          value={integration.uniqueId}
          onChange={e => handleUniqueIdChange(e, index)}
        />
      </Col>,
      <Col
        xs={2}
        key="deleter"
      >
        <Button
          onClick={() => removeUniqueId(index)}
          className="border-0"
        >
          <i
            className="material-icons md-18"
            aria-hidden="true"
          >
            delete
          </i>
        </Button>
      </Col>,
    ];
  };

  const renderIntegrationSettings = () => {
    const subtenant = row.subtenant_id ? row.subtenant_id.toString() : '';
    const invalid = validationErrors.uniqueIds;
    return (
      <React.Fragment>
        {!lo_platform.user.subtenant_id && !!subtenants.length && (
          <AdminFormSelect
            entity="courseSections"
            field="subtenant"
            inputName="subtenantId"
            value={subtenant}
            options={[{ id: '', name: '' }, ...subtenants]}
            T={T}
          />
        )}
        <AdminFormField
          entity="courseSections"
          field="externalId"
          value={row.externalId}
          invalid={validationErrors.externalId}
          T={T}
        />
        <FormGroup
          row
          className={classNames({ 'has-danger': invalid })}
        >
          <Label lg={2}>{T.t('adminPage.courseSections.fieldName.uniqueId')}</Label>
          <Col lg={10}>
            {integrations.map((integration, idx) => (
              <Row
                key={idx}
                className="mb-2"
              >
                {renderIntegrationRow(integration, idx)}
              </Row>
            ))}
            <Row>
              <Col xs={{ size: 2, offset: 10 }}>
                <Button
                  onClick={addUniqueId}
                  className="border-0"
                >
                  <i
                    className="material-icons md-18"
                    aria-hidden="true"
                  >
                    add
                  </i>
                </Button>
              </Col>
            </Row>
            {invalid && (
              <FormFeedback
                style={{ display: 'block' }}
                id={'courseSections-uniqueIds-problem'}
              >
                {invalid}
              </FormFeedback>
            )}
          </Col>
        </FormGroup>
      </React.Fragment>
    );
  };

  if (!loaded) return null;
  const baseSectionProps = {
    page: 'courseSections',
    translations: T,
  };
  return (
    <React.Fragment>
      {row.id && <div className="entity-id">{row.id}</div>}
      <AdminFormSection
        {...baseSectionProps}
        section="courseAssociation"
      >
        <EditAddLwSection
          entity="courseSections"
          row={row}
          translations={T}
          validationErrors={validationErrors}
          setPortalAlertStatus={setPortalAlertStatus}
          offeredOnly={true}
          updateDefaultSectionName={updateDefaultCourseName}
        />
        <Input
          type="hidden"
          name="fjœr"
          value={fjœr ? 'on' : ''}
        />
      </AdminFormSection>
      <AdminFormSection
        {...baseSectionProps}
        section="sectionInformation"
      >
        {renderSectionDetails()}
        {renderSectionDates()}
      </AdminFormSection>
      <AdminFormSection
        {...baseSectionProps}
        section="integrationSettings"
      >
        {renderIntegrationSettings()}
      </AdminFormSection>
    </React.Fragment>
  );
};

EditAddForm.validateForm = (form, row, el, T) => {
  const fjœr = !!form.fjœr;
  const parse = (s: string) => parseInt(s, 10) || null;
  const data: Record<string, any> = {
    fjœr,
    groupId: form.groupId,
    name: form.name,
    startDate: form.startDate || null,
    endDate: form.endDate || null,
    shutdownDate: form.shutdownDate || null,
    selfStudy: form.selfStudy === 'on',
    externalId: !form.externalId ? null : form.externalId,
    subtenant_id: parse(form.subtenantId),
    project_id: parse(form.project),
    version_id: parse(form.version),
    course_id: parse(form.course),
    useOffering: fjœr,
  };
  // serialize behaviour is malfeasant when fields are empty, even with empty: true
  const integrations = [];
  for (let i = 0; el[`integrationId-${i}` as any]; ++i) {
    integrations.push({
      integrationId: parse((el[`integrationId-${i}` as any] as HTMLInputElement).value),
      systemId: parse((el[`systemId-${i}` as any] as HTMLInputElement).value),
      uniqueId: (el[`uniqueId-${i}` as any] as HTMLInputElement).value,
    });
  }
  data.uniqueIds = integrations.filter(i => i.systemId && i.uniqueId);
  const missing =
    !row.id && fjœr && !data.project_id
      ? 'project'
      : !row.id && fjœr && !data.version_id
        ? 'version'
        : !row.id && fjœr && !data.course_id
          ? 'course'
          : !data.groupId
            ? 'groupId'
            : !data.name
              ? 'name'
              : null;
  if (missing) {
    const params = { field: T.t(`adminPage.courseSections.fieldName.${missing}`) };
    return { validationErrors: { [missing]: T.t('adminForm.validation.fieldIsRequired', params) } };
  } else if (data.startDate && data.endDate && !moment(data.startDate).isBefore(data.endDate)) {
    const params = { field: T.t(`adminPage.courseSections.fieldName.endDate`) };
    return { validationErrors: { endDate: T.t('adminForm.validation.fieldMustBeValid', params) } };
  } else if (data.shutdownDate && moment(data.endDate).isAfter(data.shutdownDate)) {
    const params = { field: T.t(`adminPage.courseSections.fieldName.shutdownDate`) };
    return {
      validationErrors: { shutdownDate: T.t('adminForm.validation.fieldMustBeValid', params) },
    };
  } else {
    return { data, extras: { roster: form.roster === 'on' } };
  }
};

export default EditAddForm;
