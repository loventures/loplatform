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

import 'rc-color-picker/assets/index.css';

import classNames from 'classnames';
import colorFn from 'color';
import PropTypes from 'prop-types';
import ColorPicker from 'rc-color-picker';
import React from 'react';
import { Button, Col, FormGroup, Input, Label } from 'reactstrap';

class AdminFormColor extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      color: this.props.value,
    };
  }

  handleChangeComplete = e => {
    const { onChange } = this.props;
    onChange && onChange(e.color);
    this.setState({ color: e.color });
  };

  render() {
    const { entity, field, inputName, invalid, required, T } = this.props;
    const id = `${entity}-${field}`;
    const { color } = this.state;
    const contrast = colorFn(color).luminosity() < 0.5 ? 'white' : 'black';
    return (
      <FormGroup
        row
        className={classNames({ 'has-danger': invalid, 'is-required': required })}
      >
        <Label
          lg={2}
          for={id}
        >
          {T.t(`adminPage.${entity}.fieldName.${field}`)}
        </Label>
        <Col lg={10}>
          <Input
            type="hidden"
            name={inputName || field}
            value={color}
          />
          <ColorPicker
            color={color}
            enableAlpha={false}
            onChange={this.handleChangeComplete}
            placement="topLeft"
          >
            <Button
              id={id}
              block
              aria-required={required}
            >
              <span style={{ color: contrast }}>{color}</span>
            </Button>
          </ColorPicker>
        </Col>
      </FormGroup>
    );
  }
}

AdminFormColor.propTypes = {
  entity: PropTypes.string,
  field: PropTypes.string,
  inputName: PropTypes.string,
  value: PropTypes.string,
  invalid: PropTypes.string,
  onChange: PropTypes.func,
  required: PropTypes.bool,
  T: PropTypes.shape({
    t: PropTypes.func,
  }),
};

export default AdminFormColor;
