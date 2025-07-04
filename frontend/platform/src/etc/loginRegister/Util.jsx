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

import classNames from 'classnames';
import React from 'react';
import { Button, FormFeedback, Input, InputGroup, Label } from 'reactstrap';

import WaitDotGif from '../../components/WaitDotGif';
import { ContentTypeURLEncoded } from '../../services';
import { lojax } from '../../services/lojax';
import {
  AccessCodeRedeemUrl,
  AccessCodeValidateUrl,
  PasswordValidateUrl,
} from '../../services/URLs';

export const Schema = 'enrollmentAccessCodeBatch'; // the only sensible option for now

export const FormInput = ({
  addOn,
  id,
  innerRef,
  invalid,
  label,
  name,
  autoComplete,
  onChange,
  autoFocus,
  readOnly,
  type,
  value,
}) => (
  <React.Fragment key={id}>
    <Label
      for={id}
      className="super-label"
    >
      {label}
    </Label>
    <InputGroup>
      <Input
        id={id}
        className={classNames({ 'is-invalid': invalid })}
        value={value}
        autoFocus={autoFocus}
        autoComplete={autoComplete}
        type={type || 'text'}
        name={name}
        onChange={onChange}
        readOnly={readOnly}
        innerRef={innerRef}
      />
      {addOn}
    </InputGroup>
    {invalid && (
      <FormFeedback
        style={{ display: 'block' }}
        id={`${id}-problem`}
      >
        {invalid}
      </FormFeedback>
    )}
  </React.Fragment>
);

export const FormSubmit = ({ block, className, id, label, submitting, disabled, color }) => (
  <Button
    id={id}
    color={color || 'primary'}
    block={block}
    className={className}
    type="submit"
    disabled={disabled || submitting}
  >
    {label}
    {submitting && (
      <WaitDotGif
        className="ms-2 waiting"
        color="light"
        size={16}
      />
    )}
  </Button>
);

export const validateAccessCode = accessCode =>
  lojax({
    method: 'post',
    url: AccessCodeValidateUrl,
    data: { accessCode, schema: Schema },
  });

export const redeemAccessCode = accessCode =>
  lojax({
    method: 'post',
    url: AccessCodeRedeemUrl,
    data: { accessCode, schema: Schema },
  });

export const validatePassword = password =>
  lojax({
    method: 'post',
    url: PasswordValidateUrl,
    data: `password=${encodeURIComponent(password)}`,
    ...ContentTypeURLEncoded,
  })
    .catch(() => Promise.reject({ reason: 'UnknownError' }))
    .then(res => (res.status === 202 ? Promise.reject(res.data) : res));
