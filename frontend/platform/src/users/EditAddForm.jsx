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
import { Button, Col, FormFeedback, FormGroup, Input, Label, Row } from 'reactstrap';

import { AdminFormField, AdminFormSection, AdminFormSelect } from '../components/adminForm';
import LoPropTypes from '../react/loPropTypes';

class EditAddForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loaded: true,
      pass: false,
      email: false,
      integrations: [],
    };
  }

  componentDidMount() {
    const editing = Object.keys(this.props.row).length > 0;
    if (Object.keys(this.props.row).length > 0) {
      axios.get('/api/v2/users/' + this.props.row.id + '/integrations').then(res => {
        const integrations = res.data.objects;
        if (!editing) {
          integrations.push({ connector_id: '', uniqueId: '' });
        }
        this.setState({ loaded: true, email: !editing, integrations: integrations });
      });
    } else {
      this.setState({
        loaded: true,
        email: !editing,
        integrations: [{ connector_id: '', uniqueId: '' }],
      });
    }
  }

  handleCheck = type => {
    switch (type) {
      case 'email':
        return this.setState({ email: !this.state.email, pass: false });
      case 'pass':
        return this.setState({ pass: !this.state.pass, email: false });
      default:
        return;
    }
  };

  renderPersonalDetails = () => {
    const { columns, validationErrors, row, translations: T } = this.props;
    const fields = ['userName', 'givenName', 'middleName', 'familyName', 'emailAddress'];
    return fields.map(field => {
      const col = columns.find(col => col.dataField === field);
      const isEmail = field === 'emailAddress';
      const isRequired = col.required || (isEmail && this.state.email);
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

  renderPasswordSettings = () => {
    const { allowPasswordReset, validationErrors, row, translations: T } = this.props;
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
                  checked={this.state.email}
                  onChange={() => this.handleCheck('email')}
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
                checked={this.state.pass}
                onChange={() => this.handleCheck('pass')}
              />
              {T.t('adminPage.users.modal.setPassword')}
            </Label>
          </Col>
        </FormGroup>
        {this.state.pass && (
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

  renderDomainRoles = () => {
    const row = this.props.row;
    const editing = Object.keys(row).length > 0;
    return (
      <React.Fragment>
        {this.props.domainRoles.map(role => (
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

  renderIntegrationSettings = () => {
    const { validationErrors, row, subtenants, translations: T } = this.props;
    const subtenant = row.subtenant_id ? row.subtenant_id.toString() : '';
    const invalid = validationErrors.uniqueIds;
    return (
      <React.Fragment>
        {!this.props.lo_platform.user.subtenant_id && !!subtenants.length && (
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

  render() {
    if (!this.state.loaded) return null;
    const row = this.props.row;
    const T = this.props.translations;
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
          {this.renderPersonalDetails()}
        </AdminFormSection>
        <div className="mt-3">
          <AdminFormSection
            {...baseSectionProps}
            section="passwordSettings"
          >
            {this.renderPasswordSettings()}
          </AdminFormSection>
        </div>
        <div className="mt-3">
          <AdminFormSection
            {...baseSectionProps}
            section="domainRoles"
          >
            {this.renderDomainRoles()}
          </AdminFormSection>
        </div>
        <div className="mt-3">
          <AdminFormSection
            {...baseSectionProps}
            section="integrationSettings"
          >
            {this.renderIntegrationSettings()}
          </AdminFormSection>
        </div>
      </React.Fragment>
    );
  }
}

EditAddForm.propTypes = {
  allowPasswordReset: PropTypes.bool,
  row: PropTypes.object,
  columns: PropTypes.array.isRequired,
  validationErrors: PropTypes.object,
  translations: LoPropTypes.translations,
  domainRoles: PropTypes.array.isRequired,
  externalSystems: PropTypes.array.isRequired,
  subtenants: PropTypes.array.isRequired,
  lo_platform: LoPropTypes.lo_platform,
};

EditAddForm.defaultProps = {
  allowPasswordReset: true,
};

export default EditAddForm;
