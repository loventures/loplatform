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
import React, { useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Button, Col, FormGroup, Input, InputGroup, Row } from 'reactstrap';

import { AdminPage } from '../adminPortal/types';
import {
  AdminFormCheck,
  AdminFormField,
  AdminFormSecret,
  AdminFormSelect,
} from '../components/adminForm';
import ReactTable from '../components/reactTable/ReactTable';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import { trim } from '../services';
import { IoGlobeOutline } from 'react-icons/io5';

const LTI_1_1 = 'LTI-1p0';
const LTI_1_3 = 'LTI-1p3';

const columns: any[] = [
  { dataField: 'id', isKey: true },
  { dataField: 'name', sortable: true, searchable: true, required: true },
];

const LtiTools: React.FC & AdminPage = () => {
  const T = useTranslations();
  const dispatch = useDispatch();

  const [ltiVersion, setLtiVersion] = useState(LTI_1_1);
  const [customParams, setCustomParams] = useState<string[]>([]);
  const inputEl = useRef<HTMLInputElement>(null);

  const renderForm = (row: any, validationErrors: any, tweakParentState: () => void) => {
    const config = row.ltiConfiguration ? row.ltiConfiguration.defaultConfiguration : {};
    const customs = config.customParameters || {};
    const editables = row.ltiConfiguration ? row.ltiConfiguration.instructorEditable : {};
    const editableCustoms = editables.editableCustomParameters || [];
    const editable = (field: string) => {
      const checked = field.startsWith('custom_')
        ? editableCustoms.indexOf(field.substring(7)) >= 0
        : editables[field];
      return (
        <FormGroup
          switch
          inline
          className="ms-3 me-0 align-self-center"
        >
          <Input
            id={`ltiTools-${field}-editable`}
            defaultChecked={checked}
            className="editable"
            type="switch"
            name={`${field}Editable`}
          />
        </FormGroup>
      );
    };
    const includes = [
      'includeUsername',
      'isGraded',
      'includeRoles',
      'includeEmailAddress',
      'includeContextTitle',
    ];
    const ltiVersions = [
      { id: LTI_1_1, name: T.t('adminPage.ltiTools.ltiVersion.lti_1_1') },
      { id: LTI_1_3, name: T.t('adminPage.ltiTools.ltiVersion.lti_1_3') },
    ];
    const launchStyles = [
      { id: 'FRAMED', name: T.t('adminPage.ltiTools.launchStyle.framed') },
      { id: 'NEW_WINDOW', name: T.t('adminPage.ltiTools.launchStyle.newWindow') },
    ];
    const updateVersion = (version: string) => {
      setLtiVersion(version);
      tweakParentState(); // hack to trigger a redraw
    };
    const addCustom = () => {
      const value = trim(inputEl.current!.value).replace('custom_', '');
      if (value && customParams.indexOf(value) < 0) {
        setCustomParams(customParams => [...customParams, value]);
        tweakParentState(); // hack to trigger a redraw
      }
      inputEl.current!.value = '';
    };
    const deleteCustom = (param: string) => {
      setCustomParams(customParams => customParams.filter(a => a !== param));
      tweakParentState(); // hack to trigger a redraw
    };
    return (
      <>
        <Input
          type="hidden"
          name="disabled"
          value={row.disabled ? 'on' : ''}
        />
        <AdminFormField
          entity="ltiTools"
          field="name"
          invalid={validationErrors.name}
          value={row.name}
          required={true}
          autoFocus={true}
          T={T}
        />
        <AdminFormSelect
          entity="ltiTools"
          field="ltiVersion"
          value={config.ltiVersion}
          options={ltiVersions}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => updateVersion(e.target.value)}
          T={T}
        />
        {ltiVersion !== LTI_1_3 && (
          <>
            <Row>
              <Col className="ltiToolsEditableField">{T.t('adminPage.ltiTools.editable')}</Col>
            </Row>
            <AdminFormField
              entity="ltiTools"
              field="url"
              invalid={validationErrors.url}
              value={config.url}
              T={T}
              addOn={editable('url')}
            />
            <AdminFormField
              entity="ltiTools"
              field="key"
              invalid={validationErrors.key}
              value={config.key}
              T={T}
              addOn={editable('key')}
            />
            <AdminFormSecret
              entity="ltiTools"
              field="secret"
              invalid={validationErrors.secret}
              value={config.secret}
              T={T}
              addOn={editable('secret')}
            />
          </>
        )}
        {ltiVersion === LTI_1_3 && (
          <>
            <AdminFormSecret
              entity="ltiTools"
              field="clientId"
              invalid={validationErrors.clientId}
              value={config.clientId}
              noBlur={true}
              T={T}
            />
            <AdminFormSecret
              entity="ltiTools"
              field="deploymentId"
              invalid={validationErrors.deploymentId}
              value={config.deploymentId}
              noBlur={true}
              T={T}
            />
            <AdminFormField
              entity="ltiTools"
              field="keysetUrl"
              invalid={validationErrors.keysetUrl}
              value={config.keysetUrl}
              T={T}
            />
            <AdminFormField
              entity="ltiTools"
              field="loginUrl"
              invalid={validationErrors.loginUrl}
              value={config.loginUrl}
              T={T}
            />
            <AdminFormField
              entity="ltiTools"
              field="redirectionUrls"
              type="textarea"
              invalid={validationErrors.redirectionUrls}
              value={config.redirectionUrls}
              T={T}
            />
            <AdminFormField
              entity="ltiTools"
              field="deepLinkUrl"
              invalid={validationErrors.deepLinkUrl}
              value={config.deepLinkUrl}
              T={T}
            />
            <Row>
              <Col className="ltiToolsEditableField">{T.t('adminPage.ltiTools.editable')}</Col>
            </Row>
            <AdminFormField
              entity="ltiTools"
              field="url"
              invalid={validationErrors.url}
              value={config.url}
              T={T}
              addOn={editable('url')}
            />
          </>
        )}
        <AdminFormSelect
          entity="ltiTools"
          field="launchStyle"
          value={config.launchStyle}
          options={launchStyles}
          T={T}
          addOn={editable('launchStyle')}
        />
        {customParams.map(param => {
          const addOn = (
            <>
              <Button
                id={`ltiTools-custom_${param}-delete`}
                onClick={() => deleteCustom(param)}
                aria-label={T.t('adminPage.ltiTools.delete')}
                className="ms-1"
              >
                <i
                  className="material-icons md-18"
                  aria-hidden="true"
                >
                  delete
                </i>
              </Button>
              {editable(`custom_${param}`)}
            </>
          );
          return (
            <AdminFormField
              key={`custom_${param}`}
              entity="ltiTools"
              field={`custom_${param}`}
              label={param}
              value={customs[param]}
              T={T}
              addOn={addOn}
              title={`custom_${param}`}
              labelClassName="label-truncate label-custom"
            />
          );
        })}
        <Row className="form-group form-inline mb-3">
          <Col
            lg={{ size: 10, offset: 2 }}
            className="d-flex flex-shrink-1"
          >
            <InputGroup>
              <Input
                id="add-custom-text"
                innerRef={inputEl}
                type="text"
              />
              <Button
                id="add-custom-button"
                onClick={addCustom}
              >
                Add Custom Parameter
              </Button>
            </InputGroup>
            {editable('customParameters')}
          </Col>
        </Row>
        {includes.map(field => (
          <AdminFormCheck
            key={field}
            entity="ltiTools"
            field={field}
            value={config[field]}
            T={T}
            addOn={editable(field)}
          />
        ))}
        <AdminFormCheck
          entity="ltiTools"
          field="useExternalId"
          value={config.useExternalId}
          T={T}
        />
        <AdminFormCheck
          entity="ltiTools"
          field="copyBranchSection"
          value={row.copyBranchSection}
          T={T}
        />
      </>
    );
  };

  const validateForm = (form: any) => {
    const parameters = [
      'ltiVersion',
      'url',
      'key',
      'secret',
      'clientId',
      'deploymentId',
      'keysetUrl',
      'loginUrl',
      'redirectionUrls',
      'deepLinkUrl',
      'launchStyle',
      'includeUsername',
      'includeRoles',
      'includeContextTitle',
      'includeEmailAddress',
      'isGraded',
      'customParameters',
    ];
    const getProp = (prop: string) =>
      prop.match('^(include|is|use)') ? form[prop] === 'on' : trim(form[prop]);
    const defaultConfiguration = parameters.reduce(
      (o, prop) => ({ ...o, [prop]: getProp(prop) }),
      {} as Record<string, any>
    );
    const instructorEditable = parameters.reduce(
      (o, prop) => ({ ...o, [prop]: form[`${prop}Editable`] === 'on' }),
      {} as Record<string, any>
    );
    const customParameters = customParams.reduce(
      (o, param) => ({ ...o, [param]: form[`custom_${param}`] || '' }),
      {} as Record<string, any>
    );
    const editableCustomParameters = customParams.filter(
      param => form[`custom_${param}Editable`] === 'on'
    );
    const data: Record<string, any> = {
      name: trim(form.name),
      disabled: form.disabled === 'on',
      copyBranchSection: form.copyBranchSection === 'on',
      ltiConfiguration: {
        defaultConfiguration: {
          ...defaultConfiguration,
          useExternalId: form.useExternalId === 'on',
          ltiMessageType: null,
          customParameters,
        },
        instructorEditable: {
          ...instructorEditable,
          includeExternalId: false,
          editableCustomParameters,
        },
      },
    };
    const missing = columns.find(col => col.required && data[col.dataField] === '');
    const params = missing && { field: T.t(`adminPage.ltiTools.fieldName.${missing.dataField}`) };
    return missing
      ? {
          validationErrors: {
            [missing.dataField]: T.t('adminForm.validation.fieldIsRequired', params),
          },
        }
      : { data };
  };

  const onBeforeModalShowCallbackFnCreatorProducer = (row: any) => {
    const ltiVersion = row.ltiConfiguration?.defaultConfiguration.ltiVersion;
    const customParams = row.ltiConfiguration
      ? [...Object.keys(row.ltiConfiguration.defaultConfiguration.customParameters)]
      : [];
    setLtiVersion(ltiVersion);
    setCustomParams(customParams);
  };

  const transition = (selectedRow: any) => {
    const { _type, id, toolId, disabled, ...rest } = selectedRow; // eslint-disable-line
    return axios
      .put(`/api/v2/ltiTools/${selectedRow.id}`, { ...rest, disabled: !disabled })
      .catch(e => {
        console.log(e);
        dispatch(setPortalAlertStatus(true, false, T.t('error.unexpectedError')));
      });
  };

  const getButtonInfo = (selectedRow: any) => {
    return [
      {
        name: !selectedRow || !selectedRow.disabled ? 'suspend' : 'reinstate',
        iconName: !selectedRow || !selectedRow.disabled ? 'not_interested' : 'check',
        onClick: transition,
      },
    ];
  };

  const trClassFormat = ({ disabled }: any) => (disabled ? 'row-disabled' : '');

  return (
    <ReactTable
      entity="ltiTools"
      beforeCreateOrUpdate={onBeforeModalShowCallbackFnCreatorProducer}
      columns={columns}
      defaultSortField="name"
      defaultSearchField="name"
      renderForm={renderForm}
      validateForm={validateForm}
      trClassFormat={trClassFormat}
      getButtons={getButtonInfo}
      translations={T}
    />
  );
};

LtiTools.pageInfo = {
  identifier: 'ltiTools',
  icon: IoGlobeOutline,
  link: '/LtiTools',
  group: 'integrations',
  right: 'loi.cp.ltitool.ManageLtiToolsAdminRight',
  entity: 'ltiTools',
};

export default LtiTools;
