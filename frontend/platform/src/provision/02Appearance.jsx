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
import { FormGroup, FormText } from 'reactstrap';

import Color from './Color';
import FileUpload from './FileUpload';
import { extendObject } from './index';
import ProvisionStep from './ProvisionStep';

const ValidColor = /^#[0-9A-F]{3,6}$/i;

export default (T, setErrors, updateAppearance) => {
  const [appearance, setAppearance] = useState({
    favicon: '',
    logo: '',
    primaryColor: '',
    secondaryColor: '',
    accentColor: '',
  });

  const updateData = () => {
    let errors = [];

    const newAppearance = { ...appearance };
    ['primary', 'secondary', 'accent'].forEach(c => {
      const field = `${c}Color`;
      const val = _.defaultTo(appearance[field], '').substring(0, 7).toUpperCase();
      newAppearance[field] = val;
      if (val.length > 0 && !ValidColor.test(val)) {
        errors.push(
          T.t('adminForm.validation.fieldMustBeValid', {
            field: T.t(`adminPage.provision.field.${field}.placeholder`),
          })
        );
      }
    });

    return errors.length === 0
      ? Promise.resolve().then(() => updateAppearance(newAppearance))
      : Promise.reject(errors);
  };

  return (
    <ProvisionStep
      step={2}
      T={T}
      formGroup={
        <FormGroup>
          {[
            ['favicon', '/favicon.ico'],
            ['logo', '/Domain/Media/biohazard.png'],
          ].map(([prop, img]) => (
            <FileUpload
              key={prop}
              T={T}
              id={prop}
              defImgSrc={img}
              updateFormData={guid => setAppearance(extendObject(appearance, prop, guid))}
              setErrors={setErrors}
            />
          ))}
          <div className="mt-4">
            {[
              ['primary', '#22839C'],
              ['secondary', '#09414E'],
              ['accent', '#CE3B19'],
            ].map(([c, def]) => {
              const prop = `${c}Color`;
              return (
                <Color
                  T={T}
                  id={`${c}-color`}
                  key={prop}
                  defaultColor={def}
                  updateFormData={imgSrc => setAppearance(extendObject(appearance, prop, imgSrc))}
                  setErrors={setErrors}
                />
              );
            })}
            <FormText> {T.t('adminPage.provision.step2.exampleColor')} </FormText>
          </div>
        </FormGroup>
      }
      updateData={updateData}
    />
  );
};
