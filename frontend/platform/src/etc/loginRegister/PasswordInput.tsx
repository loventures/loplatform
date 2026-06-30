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

import Polyglot from 'node-polyglot';
import React, { useMemo, useState } from 'react';
import { FormFeedback, InputGroupText } from 'reactstrap';
import { debounce } from 'throttle-debounce';

import { trim } from '../../services';
import { FormInput, validatePassword } from './Util';

interface PasswordInputProps {
  T: Polyglot;
  id: string;
  name: string;
  label: React.ReactNode;
  setInvalid: (invalid: React.ReactNode) => void;
  invalid?: React.ReactNode;
  autoFocus?: boolean;
}

const strength = (valid: boolean, invalid?: React.ReactNode) => (
  <InputGroupText className={invalid ? 'text-danger' : valid ? 'text-success' : undefined}>
    <span className="material-icons md-18">security</span>
  </InputGroupText>
);

const PasswordInput: React.FC<PasswordInputProps> = ({
  T,
  id,
  name,
  label,
  setInvalid,
  invalid,
  autoFocus,
}) => {
  const [valid, setValid] = useState(false);

  const validatePw = (password: string) => {
    if (!password) {
      setValid(false);
      setInvalid(null);
    } else {
      validatePassword(password)
        .then(() => {
          setValid(true);
          setInvalid(null);
        })
        .catch(({ reason, messages }: { reason: string; messages: string[] }) => {
          if (reason === 'InvalidPassword') {
            setValid(false);
            setInvalid(messages.map((msg, idx) => <div key={idx}>{msg}</div>));
          }
        });
    }
  };

  const debouncedValidatePw = useMemo(() => debounce(300, validatePw), []);

  const onPasswordUpdate = (e: React.ChangeEvent<HTMLInputElement>) =>
    debouncedValidatePw(trim(e.target.value));

  return (
    <React.Fragment>
      <FormInput
        autoComplete="new-password"
        autoFocus={autoFocus}
        id={id}
        invalid={invalid}
        name={name}
        label={label}
        type="password"
        addOn={strength(valid, invalid)}
        onChange={onPasswordUpdate}
      />
      {valid && (
        <FormFeedback
          valid
          style={{ display: 'block' }}
          id={`${id}-acceptable`}
        >
          {T.t('password.acceptable')}
        </FormFeedback>
      )}
    </React.Fragment>
  );
};

export default PasswordInput;
