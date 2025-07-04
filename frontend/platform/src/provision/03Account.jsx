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

import _ from 'lodash';
import React, { useState } from 'react';
import { FormGroup } from 'reactstrap';

import { extendObject } from './index';
import InputText from './InputText';
import ProvisionStep from './ProvisionStep';

export default (T, updateAccount) => {
  const [emailValid, setEmailValid] = useState(true);
  const [account, setAccount] = useState({
    givenName: '',
    middleName: '',
    familyName: '',
    emailAddress: '',
  });

  const updateData = () => {
    const errors = [];

    ['givenName', 'familyName'].forEach(field => {
      const value = _.defaultTo(account[field], '');
      if (value.length === 0) {
        errors.push(
          T.t('adminForm.validation.fieldIsRequired', {
            field: T.t(`adminPage.provision.field.${field}.placeholder`),
          })
        );
      }
    });

    let userName = 'admin';
    if (emailValid && account.emailAddress.length > 0) {
      userName = account.emailAddress;
    } else if (account.emailAddress.length > 0) {
      errors.push(
        T.t('adminForm.validation.fieldMustBeValid', {
          field: T.t('adminPage.provision.field.emailAddress.placeholder'),
        })
      );
    }

    return errors.length === 0
      ? Promise.resolve().then(() => updateAccount({ ...account, userName }))
      : Promise.reject(errors);
  };

  return (
    <ProvisionStep
      step={3}
      T={T}
      formGroup={
        <FormGroup>
          {Object.keys(account)
            .slice(0, 3)
            .map(prop => (
              <InputText
                id={prop}
                key={prop}
                T={T}
                onChange={ev => setAccount(extendObject(account, prop, ev.target.value))}
              />
            ))}
          <InputText
            id="emailAddress"
            T={T}
            type="email"
            onChange={ev => {
              setEmailValid(
                (ev.target.value || '').trim().length === 0 || ev.target.checkValidity()
              );
              setAccount(extendObject(account, 'emailAddress', ev.target.value));
            }}
          />
        </FormGroup>
      }
      updateData={updateData}
    />
  );
};
