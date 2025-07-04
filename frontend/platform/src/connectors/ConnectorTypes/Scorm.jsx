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
import PropTypes from 'prop-types';
import React, { Component } from 'react';

import { AdminFormSelect } from '../../components/adminForm';

class Scorm extends Component {
  constructor(props) {
    super(props);
    this.state = {
      subtenants: [],
      loaded: false,
    };
  }

  componentDidMount() {
    axios.get('/api/v2/subtenants').then(res => {
      this.setState({ subtenants: res.data.objects, loaded: true });
    });
  }

  render() {
    const { T, row, configs, renderField } = this.props;
    const { subtenants, loaded } = this.state;
    if (!loaded) return null;
    const field = 'subtenant';
    const options = [{ key: 'empty', id: 'empty', text: '' }].concat(
      subtenants.map(sub => ({ key: sub.id, id: sub.id, text: sub.name }))
    );
    const configuration = row.configuration ? JSON.parse(row.configuration) : {};
    const subtenantVal = configuration.subtenant + '' || '';
    const noConfigs = { configuration: true, useExternalIdentifier: true, usernameParameter: true };
    return (
      <React.Fragment>
        {configs &&
          configs.filter(config => !noConfigs[config.id]).map(config => renderField(config))}
        <AdminFormSelect
          key={field}
          entity="connectors"
          field={field}
          inputName={field}
          value={subtenantVal}
          T={T}
          options={options}
        />
      </React.Fragment>
    );
  }
}

Scorm.propTypes = {
  T: PropTypes.object.isRequired,
  row: PropTypes.object.isRequired,
  configs: PropTypes.array.isRequired,
  renderField: PropTypes.func.isRequired,
};

const validateForm = parsedForm => {
  const config = {};
  config.preferences = {
    lOFooter: false,
    instructorControlsV2: true,
  };
  config.autoCreateSubtenant = false;
  config.subtenant = parsedForm.subtenant ? parseInt(parsedForm.subtenant, 10) : null;
  const data = {
    configuration: JSON.stringify(config),
  };
  delete parsedForm.subtenant;
  return {
    dto: { data },
    parsedForm: parsedForm,
  };
};

export default {
  componentId: 'loi.cp.scorm.impl.ScormSystemImpl',
  component: Scorm,
  validateForm: validateForm,
};
