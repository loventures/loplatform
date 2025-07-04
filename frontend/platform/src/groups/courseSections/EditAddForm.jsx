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
import classNames from 'classnames';
import moment from 'moment-timezone';
import PropTypes from 'prop-types';
import React from 'react';
import { Button, Col, FormFeedback, FormGroup, Input, Label, Row } from 'reactstrap';

import {
  AdminFormCheck,
  AdminFormDateTime,
  AdminFormField,
  AdminFormSection,
  AdminFormSelect,
} from '../../components/adminForm';
import LoPropTypes from '../../react/loPropTypes';
import { inCurrTimeZone } from '../../services/moment.js';
import EditAddLwSection from '../EditAddLwSection';

class EditAddForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loaded: false,
      integrations: [],
      project: null,
      course: null,
      endDate: props.row.endDate,
      selfStudy: props.row.selfStudy,
      reviewPeriodOffset: 0,
      betaSelfStudy: false,
    };
  }

  componentDidMount() {
    const { row } = this.props;
    if (row.id) {
      const fetchen = [
        axios.get(`/api/v2/courseSections/${row.id}`),
        axios.get(`/api/v2/courseSections/configuration`),
      ];
      Promise.all(fetchen)
        .then(([crsRes, cfRes]) => {
          this.setState({
            loaded: true,
            integrations: crsRes.data.integrations,
            reviewPeriodOffset: cfRes.data.reviewPeriodOffset,
            betaSelfStudy: cfRes.data.betaSelfStudy,
          });
        })
        .catch(this.genericError);
    } else {
      axios
        .get(`/api/v2/courseSections/configuration`)
        .then(({ data }) => {
          this.setState({
            loaded: true,
            integrations: [{ connector_id: '', uniqueId: '' }],
            reviewPeriodOffset: data.reviewPeriodOffset,
            betaSelfStudy: data.betaSelfStudy,
          });
        })
        .catch(this.genericError);
    }
  }

  genericError = e => {
    console.log(e);
    const T = this.props.translations;
    this.props.setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
  };

  renderSectionDetails = () => {
    const { columns, validationErrors, row, translations: T } = this.props;
    const fields = ['groupId', 'name'];
    return fields.map(field => {
      const col = columns.find(col => col.dataField === field);
      return (
        <AdminFormField
          key={field}
          entity="courseSections"
          field={field}
          value={row[field]}
          invalid={validationErrors[field]}
          required={col.required}
          T={T}
          autoFocus={row && row.id && field === 'groupId'}
        />
      );
    });
  };

  updateDefaultCourseName = (name, originName) => {
    // if the course name is blank or matches the previous origin course then update it
    const nameEl = document.getElementById('courseSections-name');
    if (nameEl && (!nameEl.value || (originName && nameEl.value === originName))) {
      nameEl.value = name;
    }
  };

  renderSectionDates = () => {
    const { columns, fjœr, validationErrors, row, translations: T } = this.props;
    const { endDate, reviewPeriodOffset, betaSelfStudy, selfStudy } = this.state;
    const fields = selfStudy
      ? []
      : ['startDate', 'endDate'].concat(endDate ? ['shutdownDate'] : []);
    return (
      <React.Fragment>
        {fields.map(field => {
          const col = columns.find(col => col.dataField === field) || {};
          const help =
            field === 'shutdownDate' ? T.t(`adminPage.courseSections.fieldHelp.${field}`) : null;
          const value = field === 'shutdownDate' ? row.configuredShutdownDate : row[field];
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
                if (field === 'endDate') this.setState({ endDate: d });
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
            onChange={e => this.setState({ selfStudy: e.target.checked })}
          />
        ) : null}
      </React.Fragment>
    );
  };

  renderIntegrationSettings = () => {
    const { validationErrors, row, subtenants, translations: T } = this.props;
    const subtenant = row.subtenant_id ? row.subtenant_id.toString() : '';
    const invalid = validationErrors.uniqueIds;
    return (
      <React.Fragment>
        {!this.props.lo_platform.user.subtenant_id && !!subtenants.length && (
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
            {this.state.integrations.map((integration, idx) => (
              <Row
                key={idx}
                className="mb-2"
              >
                {this.renderIntegrationRow(integration, idx)}
              </Row>
            ))}
            <Row>
              <Col xs={{ size: 2, offset: 10 }}>
                <Button
                  onClick={this.addUniqueId}
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

  renderIntegrationRow = (integration, index) => {
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
          onChange={e => this.handleSystemChange(e, index)}
          defaultValue={integration.connector_id || ''}
        >
          <option value=""></option>
          {integration.id
            ? this.renderSystemOption(integration.connector_id)
            : this.renderSystemOption(null)}
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
          onChange={e => this.handleUniqueIdChange(e, index)}
        />
      </Col>,
      <Col
        xs={2}
        key="deleter"
      >
        <Button
          onClick={() => this.removeUniqueId(index)}
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

  addUniqueId = () => {
    this.setState(prevState => {
      const integrations = prevState.integrations;
      integrations.push({ uniqueId: '', connector_id: '' });
      return { integrations: integrations };
    });
  };

  removeUniqueId = idx => {
    this.setState(prevState => {
      const integrations = prevState.integrations;
      integrations.splice(idx, 1);
      return { integrations: integrations };
    });
  };

  handleSystemChange = (event, idx) => {
    const integrations = this.state.integrations;
    integrations[idx].connector_id = event.target.value;
    this.setState({ integrations: integrations });
  };

  renderSystemOption = () => {
    return this.props.externalSystems.map(system => {
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

  handleUniqueIdChange = (event, index) => {
    const integrations = [...this.state.integrations];
    integrations[index].uniqueId = event.target.value;
    this.setState({ integrations: integrations });
  };

  render() {
    if (!this.state.loaded) return null;
    const { row, translations: T, fjœr, validationErrors, setPortalAlertStatus } = this.props;
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
            updateDefaultSectionName={this.updateDefaultCourseName}
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
          {this.renderSectionDetails()}
          {this.renderSectionDates()}
        </AdminFormSection>
        <AdminFormSection
          {...baseSectionProps}
          section="integrationSettings"
        >
          {this.renderIntegrationSettings()}
        </AdminFormSection>
      </React.Fragment>
    );
  }
}

EditAddForm.validateForm = (form, row, el, T) => {
  const fjœr = !!form.fjœr;
  const parse = s => parseInt(s, 10) || null;
  const data = {
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
  for (let i = 0; el[`integrationId-${i}`]; ++i) {
    integrations.push({
      integrationId: parse(el[`integrationId-${i}`].value),
      systemId: parse(el[`systemId-${i}`].value),
      uniqueId: el[`uniqueId-${i}`].value,
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

EditAddForm.propTypes = {
  row: PropTypes.object,
  columns: PropTypes.array.isRequired,
  validationErrors: PropTypes.object,
  translations: LoPropTypes.translations,
  setPortalAlertStatus: PropTypes.func.isRequired,
  externalSystems: PropTypes.array.isRequired,
  subtenants: PropTypes.array.isRequired,
  lo_platform: LoPropTypes.lo_platform,
  fjœr: PropTypes.bool,
};

export default EditAddForm;
