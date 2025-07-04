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

import PropTypes from 'prop-types';
import React from 'react';
import { FormFeedback, InputGroupText } from 'reactstrap';
import { debounce } from 'throttle-debounce';

import { trim } from '../../services';
import { FormInput, validatePassword } from './Util';

class PasswordInput extends React.Component {
  constructor(props) {
    super(props);
    this.debouncedValidatePw = debounce(300, this.validatePw);
  }

  state = {
    valid: false,
  };

  render() {
    const {
      state: { valid },
      props: { T, autoFocus, id, invalid, label, name },
    } = this;
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
          addOn={this.strength(valid, invalid)}
          onChange={this.onPasswordUpdate}
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
  }

  strength = (valid, invalid) => (
    <InputGroupText className={invalid ? 'text-danger' : valid ? 'text-success' : null}>
      <span className="material-icons md-18">security</span>
    </InputGroupText>
  );

  onPasswordUpdate = e => this.debouncedValidatePw(trim(e.target.value));

  validatePw = password => {
    const { setInvalid } = this.props;
    if (!password) {
      this.setState({ valid: false });
      setInvalid(null);
    } else {
      validatePassword(password)
        .then(() => {
          this.setState({ valid: true });
          setInvalid(null);
        })
        .catch(({ reason, messages }) => {
          if (reason === 'InvalidPassword') {
            this.setState({ valid: false });
            setInvalid(messages.map((msg, idx) => <div key={idx}>{msg}</div>));
          }
        });
    }
  };
}

PasswordInput.propTypes = {
  T: PropTypes.object.isRequired,
  id: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  setInvalid: PropTypes.func.isRequired,
  autoFocus: PropTypes.bool,
};

export default PasswordInput;
