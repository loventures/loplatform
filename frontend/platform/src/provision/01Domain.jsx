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
import React, { useState } from 'react';
import { FormGroup } from 'reactstrap';

import { extendObject } from './index';
import InputText from './InputText';
import ProvisionStep from './ProvisionStep';

export default (T, updateDomainInfo) => {
  const [domainInfo, setDomainInfo] = useState({ name: '', shortName: '', hostName: '' });

  const updateData = () => {
    const errors = [];
    Object.keys(domainInfo).forEach(field => {
      const value = domainInfo[field];
      if (value.length === 0) {
        errors.push(
          T.t('adminForm.validation.fieldIsRequired', {
            field: T.t(`adminPage.provision.field.${field}.placeholder`),
          })
        );
      }
    });

    const domainId = domainInfo.hostName.replace(/\..*/, '');

    return errors.length === 0
      ? axios
          .post('/api/v2/domains/validate', domainInfo)
          .catch(({ response: { data } }) => {
            return Promise.reject(data.messages.map(m => m.message));
          })
          .then(() => updateDomainInfo({ ...domainInfo, domainId }))
      : Promise.reject(errors);
  };

  return (
    <ProvisionStep
      step={1}
      T={T}
      formGroup={
        <FormGroup>
          {Object.keys(domainInfo).map(prop => (
            <InputText
              key={prop}
              id={prop}
              T={T}
              onChange={ev => setDomainInfo(extendObject(domainInfo, prop, ev.target.value))}
            />
          ))}
        </FormGroup>
      }
      updateData={updateData}
    />
  );
};
