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
import { FormGroup, Input, Label } from 'reactstrap';

import { SrsCollection } from '../srs';
import { AdminFormField, AdminFormSelect } from '../components/adminForm';

interface KnownRole {
  id: number | string;
  name: string;
}

interface Column {
  dataField: string;
  isKey?: boolean;
  required?: boolean;
  [key: string]: unknown;
}

interface EditAddDomainRoleProps {
  T: Polyglot;
  editing: boolean;
  columns: Column[];
  row: Record<string, any>;
  validationErrors: Record<string, string>;
}

const EditAddDomainRole: React.FC<EditAddDomainRoleProps> = ({
  T,
  editing,
  columns,
  row,
  validationErrors,
}) => {
  const [knownRoles, setKnownRoles] = useState<KnownRole[]>([]);
  const [supportedRoles, setSupportedRoles] = useState<KnownRole[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [selectedOption, setSelectedOption] = useState(1);

  useEffect(() => {
    axios
      .all([
        axios.get<SrsCollection<KnownRole>>('/api/v2/domain/knownRoles'),
        axios.get<SrsCollection<KnownRole>>('/api/v2/domain/supportedRoles'),
      ])
      .then(
        axios.spread((knownRes, supportedRes) => {
          setKnownRoles(knownRes.data.objects);
          setSupportedRoles(supportedRes.data.objects);
          setLoaded(true);
        })
      );
     
  }, []);

  const sortByName = (a: KnownRole, b: KnownRole) => {
    return a.name.toLowerCase().localeCompare(b.name.toLowerCase());
  };

  const loadCreation = () => setSelectedOption(2);
  const loadAddition = () => setSelectedOption(1);

  const renderAddRole = () => {
    const supportedIds = supportedRoles.map(role => role.id);
    const field = 'supportedRole';
    const options = [{ id: '', name: '' } as KnownRole]
      .concat(knownRoles)
      .filter(role => !supportedIds.includes(role.id))
      .sort(sortByName)
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

  const renderCreateAddRole = () => {
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

  const renderRadioBtns = () => {
    return (
      <FormGroup tag="fieldset">
        <FormGroup check>
          <Label check>
            <Input
              type="radio"
              name="addCreate"
              onChange={loadAddition}
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
              onChange={loadCreation}
              checked={selectedOption === 2}
            />{' '}
            {T.t('adminPage.roles.create.createAndAddSupportedRole')}
          </Label>
        </FormGroup>
      </FormGroup>
    );
  };

  if (!loaded) return null;
  if (editing) return <>{renderCreateAddRole()}</>;
  return (
    <React.Fragment>
      {renderRadioBtns()}
      <input
        type="hidden"
        value="true"
        name={selectedOption === 1 ? 'addingSupported' : 'creatingAndAdding'}
      />
      <div className="my-3">{selectedOption === 1 ? renderAddRole() : renderCreateAddRole()}</div>
    </React.Fragment>
  );
};

export default EditAddDomainRole;
