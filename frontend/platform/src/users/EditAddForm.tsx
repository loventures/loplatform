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
import React, { useEffect, useState } from 'react';
import { Button, Col, FormFeedback, FormGroup, Input, Label, Row } from 'reactstrap';

import { AdminFormField, AdminFormSection, AdminFormSelect } from '../components/adminForm';
import { LoPlatform } from '../types/loPlatform';

interface EditAddFormProps {
  allowPasswordReset?: boolean;
  row: any;
  columns: any[];
  validationErrors: any;
  translations: Polyglot;
  domainRoles: any[];
  externalSystems: any[];
  subtenants: any[];
  lo_platform: LoPlatform;
}

const EditAddForm = (props: EditAddFormProps) => {
  const {
    allowPasswordReset = true,
    row,
    columns,
    validationErrors,
    translations: T,
    domainRoles,
    externalSystems,
    subtenants,
    lo_platform,
  } = props;

  const [loaded, setLoaded] = useState(true);
  const [pass, setPass] = useState(false);
  const [email, setEmail] = useState(false);
  const [integrations, setIntegrations] = useState<any[]>([]);

  useEffect(() => {
    const editing = Object.keys(row).length > 0;
    if (Object.keys(row).length > 0) {
      axios.get('/api/v2/users/' + row.id + '/integrations').then(res => {
        const integrations = res.data.objects;
        if (!editing) {
          integrations.push({ connector_id: '', uniqueId: '' });
        }
        setLoaded(true);
        setEmail(!editing);
        setIntegrations(integrations);
      });
    } else {
      setLoaded(true);
      setEmail(!editing);
      setIntegrations([{ connector_id: '', uniqueId: '' }]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleCheck = (type: string) => {
    switch (type) {
      case 'email':
        setEmail(!email);
        setPass(false);
        return;
      case 'pass':
        setPass(!pass);
        setEmail(false);
        return;
      default:
        return;
    }
  };

  const renderPersonalDetails = () => {
    const fields = ['userName', 'givenName', 'middleName', 'familyName', 'emailAddress'];
    return fields.map(field => {
      const col = columns.find(col => col.dataField === field);
      const isEmail = field === 'emailAddress';
      const isRequired = col.required || (isEmail && email);
      return (
        <AdminFormField
          key={field}
          entity="users"
          field={field}
          value={row[field]}
          invalid={validationErrors[field]}
          type={isEmail ? 'email' : 'text'}
          required={isRequired}
          autoFocus={field === 'userName'}
          T={T}
        />
      );
    });
  };

  const renderPasswordSettings = () => {
    const editing = Object.keys(row).length > 0;
    return (
      <React.Fragment>
        {allowPasswordReset && (
          <FormGroup
            check
            row
          >
            <Col lg={{ size: 10, offset: 2 }}>
              <Label check>
                <input
                  id="email"
                  className="form-check-input me-2"
                  type="radio"
                  name="email"
                  checked={email}
                  onChange={() => handleCheck('email')}
                />
                {editing
                  ? T.t('adminPage.users.editModal.sendEmail')
                  : T.t('adminPage.users.createModal.sendEmail')}
              </Label>
            </Col>
          </FormGroup>
        )}
        <FormGroup
          check
          row
        >
          <Col lg={{ size: 10, offset: 2 }}>
            <Label check>
              <input
                id="pass"
                className="form-check-input me-2"
                type="radio"
                name="pass"
                checked={pass}
                onChange={() => handleCheck('pass')}
              />
              {T.t('adminPage.users.modal.setPassword')}
            </Label>
          </Col>
        </FormGroup>
        {pass && (
          <AdminFormField
            entity="users"
            field="password"
            type="password"
            invalid={validationErrors.password}
            required={true}
            autoFocus={true}
            T={T}
          />
        )}
      </React.Fragment>
    );
  };

  const renderDomainRoles = () => {
    const editing = Object.keys(row).length > 0;
    return (
      <React.Fragment>
        {domainRoles.map(role => (
          <FormGroup
            check
            key={role.id}
            row
          >
            <Col lg={{ size: 10, offset: 2 }}>
              <Label
                check
                className={role.superior ? '' : 'text-muted'}
              >
                <input
                  id={'role-' + role.name}
                  className="form-check-input me-2"
                  type="checkbox"
                  name="roles"
                  value={role.id}
                  defaultChecked={editing && row.roles.split(', ').includes(role.name)}
                  disabled={!role.superior}
                />
                {role.name}
              </Label>
            </Col>
          </FormGroup>
        ))}
      </React.Fragment>
    );
  };

  const renderIntegrationRow = (integration: any, index: number) => {
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
          {integration.id
            ? renderSystemOption(integration.connector_id)
            : renderSystemOption(null)}
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

  const handleSystemChange = (event: React.ChangeEvent<HTMLInputElement>, idx: number) => {
    integrations[idx].connector_id = event.target.value;
    setIntegrations(integrations);
  };

  const renderSystemOption = (_connectorId?: any) => {
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

  const handleUniqueIdChange = (event: React.ChangeEvent<HTMLInputElement>, index: number) => {
    const newIntegrations = [...integrations];
    newIntegrations[index].uniqueId = event.target.value;
    setIntegrations(newIntegrations);
  };

  const addUniqueId = () => {
    setIntegrations(prevIntegrations => {
      const integrations = prevIntegrations;
      integrations.push({ uniqueId: '', connector_id: '' });
      return integrations;
    });
  };

  const removeUniqueId = (idx: number) => {
    setIntegrations(prevIntegrations => {
      const integrations = prevIntegrations;
      integrations.splice(idx, 1);
      return integrations;
    });
  };

  const renderIntegrationSettings = () => {
    const subtenant = row.subtenant_id ? row.subtenant_id.toString() : '';
    const invalid = validationErrors.uniqueIds;
    return (
      <React.Fragment>
        {!lo_platform.user.subtenant_id && !!subtenants.length && (
          <AdminFormSelect
            entity="users"
            field="subtenant"
            inputName="subtenantId"
            value={subtenant}
            options={[{ id: '', name: '' }, ...subtenants]}
            T={T}
          />
        )}
        <AdminFormField
          entity="users"
          field="externalId"
          value={row.externalId}
          invalid={validationErrors.externalId}
          T={T}
        />
        <FormGroup row>
          <Label lg={2}>{T.t('adminPage.users.fieldName.uniqueId')}</Label>
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
                id={'users-uniqueIds-problem'}
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
    page: 'users',
    translations: T,
  };
  return (
    <React.Fragment>
      {row.id && <div className="entity-id">{row.id}</div>}
      <AdminFormSection
        {...baseSectionProps}
        section="personalDetails"
      >
        {renderPersonalDetails()}
      </AdminFormSection>
      <div className="mt-3">
        <AdminFormSection
          {...baseSectionProps}
          section="passwordSettings"
        >
          {renderPasswordSettings()}
        </AdminFormSection>
      </div>
      <div className="mt-3">
        <AdminFormSection
          {...baseSectionProps}
          section="domainRoles"
        >
          {renderDomainRoles()}
        </AdminFormSection>
      </div>
      <div className="mt-3">
        <AdminFormSection
          {...baseSectionProps}
          section="integrationSettings"
        >
          {renderIntegrationSettings()}
        </AdminFormSection>
      </div>
    </React.Fragment>
  );
};

export default EditAddForm;
