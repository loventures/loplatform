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

import classNames from 'classnames';
import React from 'react';
import { AxiosResponse } from 'axios';
import { Button, FormFeedback, Input, InputGroup, Label } from 'reactstrap';
import { InputType } from 'reactstrap/types/lib/Input';

import WaitDotGif from '../../components/WaitDotGif';
import { ContentTypeURLEncoded } from '../../services';
import { lojax } from '../../services/lojax';
import {
  AccessCodeRedeemUrl,
  AccessCodeValidateUrl,
  PasswordValidateUrl,
} from '../../services/URLs';

export const Schema = 'enrollmentAccessCodeBatch'; // the only sensible option for now

interface FormInputProps {
  addOn?: React.ReactNode;
  id: string;
  innerRef?: React.Ref<HTMLInputElement>;
  invalid?: React.ReactNode;
  label: React.ReactNode;
  name: string;
  autoComplete?: string;
  onChange?: React.ChangeEventHandler<HTMLInputElement>;
  autoFocus?: boolean;
  readOnly?: boolean;
  type?: InputType;
  value?: string;
}

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
}: FormInputProps) => (
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

interface FormSubmitProps {
  block?: boolean;
  className?: string;
  id: string;
  label: React.ReactNode;
  submitting?: boolean | null;
  disabled?: boolean;
  color?: string;
}

export const FormSubmit = ({
  block,
  className,
  id,
  label,
  submitting,
  disabled,
  color,
}: FormSubmitProps) => (
  <Button
    id={id}
    color={color || 'primary'}
    block={block}
    className={className}
    type="submit"
    disabled={disabled || !!submitting}
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

export const validateAccessCode = (accessCode: string): Promise<AxiosResponse> =>
  lojax({
    method: 'post',
    url: AccessCodeValidateUrl,
    data: { accessCode, schema: Schema },
  });

export const redeemAccessCode = (accessCode: string): Promise<AxiosResponse> =>
  lojax({
    method: 'post',
    url: AccessCodeRedeemUrl,
    data: { accessCode, schema: Schema },
  });

export const validatePassword = (password: string): Promise<AxiosResponse> =>
  lojax({
    method: 'post',
    url: PasswordValidateUrl,
    data: `password=${encodeURIComponent(password)}`,
    ...ContentTypeURLEncoded,
  })
    .catch(() => Promise.reject({ reason: 'UnknownError' }))
    .then(res => (res.status === 202 ? Promise.reject(res.data) : res));
