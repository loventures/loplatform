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
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import {
  Col,
  FormFeedback,
  FormGroup,
  FormText,
  Input,
  InputGroup,
  InputGroupText,
  Label,
} from 'reactstrap';

class AdminFormSecret extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      password: props.value || '',
    };
  }

  generateKey = () => {
    axios.get('/api/v2/connectors/key').then(res => this.setState({ password: res.data }));
  };

  render() {
    const { password } = this.state;
    const {
      addOn,
      type,
      entity,
      inputName,
      field,
      autoFocus,
      help,
      invalid,
      required,
      label,
      T,
      disabled,
      noBlur,
    } = this.props;
    const id = `${entity}-${field}`;
    const validProp = invalid ? { invalid: true } : {};
    return (
      <FormGroup
        row
        className={classNames({ 'has-danger': invalid, 'is-required': required })}
      >
        <Label
          lg={2}
          for={id}
        >
          {label || T.t(`adminPage.${entity}.fieldName.${field}`)}
        </Label>
        <Col lg={10}>
          <InputGroup>
            <Input
              {...validProp}
              disabled={disabled}
              type={type || 'text'}
              id={id}
              name={inputName || field}
              className={noBlur ? '' : 'secret-blur'}
              value={password}
              autoFocus={autoFocus}
              required={required}
              onChange={e => this.setState({ password: e.target.value })}
            />
            <InputGroupText
              className="clickable"
              onClick={this.generateKey}
            >
              <span className="material-icons md-18">settings</span>
            </InputGroupText>
            {addOn}
          </InputGroup>
          {invalid && <FormFeedback>{invalid}</FormFeedback>}
          {help && <FormText>{help}</FormText>}
        </Col>
      </FormGroup>
    );
  }
}

AdminFormSecret.propTypes = {
  entity: PropTypes.string,
  field: PropTypes.string,
  inputName: PropTypes.string,
  type: PropTypes.string,
  value: PropTypes.string,
  invalid: PropTypes.string,
  help: PropTypes.string,
  required: PropTypes.bool,
  autoFocus: PropTypes.bool,
  label: PropTypes.string,
  addOn: PropTypes.element,
  noBlur: PropTypes.bool,
  T: PropTypes.shape({
    t: PropTypes.func,
  }),
};

export default AdminFormSecret;
