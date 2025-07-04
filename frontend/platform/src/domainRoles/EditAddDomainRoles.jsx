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
import React, { Component } from 'react';
import { FormGroup, Input, Label } from 'reactstrap';

import { AdminFormField, AdminFormSelect } from '../components/adminForm';

class EditAddDomainRole extends Component {
  constructor(props) {
    super(props);
    this.state = {
      knownRoles: [],
      supportedRoles: [],
      loaded: false,
      selectedOption: 1,
    };
  }

  componentDidMount() {
    axios
      .all([axios.get('/api/v2/domain/knownRoles'), axios.get('/api/v2/domain/supportedRoles')])
      .then(
        axios.spread((knownRes, supportedRes) => {
          this.setState({
            knownRoles: knownRes.data.objects,
            supportedRoles: supportedRes.data.objects,
            loaded: true,
          });
        })
      );
  }

  sortByName = (a, b) => {
    return a.name.toLowerCase().localeCompare(b.name.toLowerCase());
  };

  loadCreation = () => {
    this.setState({ selectedOption: 2 });
  };

  loadAddition = () => {
    this.setState({ selectedOption: 1 });
  };

  renderAddRole = () => {
    const { T, validationErrors } = this.props;
    const { knownRoles, supportedRoles } = this.state;
    const supportedIds = supportedRoles.map(role => role.id);
    const field = 'supportedRole';
    const options = [{ id: '', name: '' }]
      .concat(knownRoles)
      .filter(role => !supportedIds.includes(role.id))
      .sort(this.sortByName)
      .map(role => {
        return { id: role.id, text: role.name };
      });
    return (
      <AdminFormSelect
        key={field}
        required={true}
        entity="roles"
        field={field}
        inputName={field}
        value={''}
        T={T}
        options={options}
        invalid={validationErrors['supportedRoleId']}
      />
    );
  };

  renderCreateAddRole = () => {
    const { T, columns, row, validationErrors } = this.props;
    return columns
      .filter(col => !col.isKey && col.dataField !== 'rights')
      .map(col => {
        const field = col.dataField;
        return (
          <AdminFormField
            key={field}
            entity="roles"
            field={field}
            value={row[field]}
            required={col.required}
            autoFocus={field === 'roleId'}
            invalid={validationErrors[field]}
            T={T}
          />
        );
      });
  };

  renderRadioBtns = () => {
    const { selectedOption } = this.state;
    const { T } = this.props;
    return (
      <FormGroup tag="fieldset">
        <FormGroup check>
          <Label check>
            <Input
              type="radio"
              name="addCreate"
              onChange={this.loadAddition}
              checked={selectedOption === 1}
            />{' '}
            {T.t('adminPage.roles.create.addSupportedRole')}
          </Label>
        </FormGroup>
        <FormGroup check>
          <Label check>
            <Input
              type="radio"
              name="addCreate"
              onChange={this.loadCreation}
              checked={selectedOption === 2}
            />{' '}
            {T.t('adminPage.roles.create.createAndAddSupportedRole')}
          </Label>
        </FormGroup>
      </FormGroup>
    );
  };

  render() {
    const { loaded, selectedOption } = this.state;
    const { editing } = this.props;
    if (!loaded) return null;
    if (editing) return this.renderCreateAddRole();
    return (
      <React.Fragment>
        {this.renderRadioBtns()}
        <input
          type="hidden"
          value={true}
          name={selectedOption === 1 ? 'addingSupported' : 'creatingAndAdding'}
        />
        <div className="my-3">
          {selectedOption === 1 ? this.renderAddRole() : this.renderCreateAddRole()}
        </div>
      </React.Fragment>
    );
  }
}

EditAddDomainRole.propTypes = {
  T: PropTypes.object.isRequired,
  editing: PropTypes.bool.isRequired,
  columns: PropTypes.array.isRequired,
  row: PropTypes.object.isRequired,
  validationErrors: PropTypes.object.isRequired,
};

export default EditAddDomainRole;
