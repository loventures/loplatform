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

import { AdminFormCheck, AdminFormField, AdminFormSelect } from '../../components/adminForm';

class LtiProvider extends Component {
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
    const configurationConfig = configs.find(config => config.id === 'configuration');
    const configuration = row.configuration ? JSON.parse(row.configuration) : {};
    const subtenantVal = configuration.subtenant + '' || '';
    const autoCreateSubtenant = configuration.autoCreateSubtenant;
    const sectionPerOffering = configuration.sectionPerOffering;
    const hideFooterLinks =
      !row.id || (configuration.preferences && configuration.preferences.lOFooter) === false;
    delete configuration.subtenant;
    const configStr = JSON.stringify(configuration);
    return (
      <React.Fragment>
        {configs &&
          configs
            .filter(config => config.id !== 'configuration')
            .map(config => renderField(config))}
        <AdminFormCheck
          entity="connectors"
          field="hideFooterLinks"
          value={hideFooterLinks}
          T={T}
        />
        <AdminFormSelect
          key={field}
          entity="connectors"
          field={field}
          inputName={field}
          value={subtenantVal}
          T={T}
          options={options}
        />
        <AdminFormCheck
          entity="connectors"
          field="autoCreateSubtenant"
          value={autoCreateSubtenant}
          T={T}
        />
        <AdminFormCheck
          entity="connectors"
          field="sectionPerOffering"
          value={sectionPerOffering}
          T={T}
        />
        <AdminFormField
          key={configurationConfig.id}
          label={configurationConfig.name}
          entity={'connectors'}
          type="textarea"
          field={configurationConfig.id}
          value={configStr}
          T={T}
        />
      </React.Fragment>
    );
  }
}

LtiProvider.propTypes = {
  T: PropTypes.object.isRequired,
  row: PropTypes.object.isRequired,
  configs: PropTypes.array.isRequired,
  renderField: PropTypes.func.isRequired,
};

const validateForm = parsedForm => {
  const config = parsedForm.configuration ? JSON.parse(parsedForm.configuration) : {};
  config.autoCreateSubtenant = parsedForm.autoCreateSubtenant === 'on';
  config.sectionPerOffering = parsedForm.sectionPerOffering === 'on';
  config.subtenant = parsedForm.subtenant ? parseInt(parsedForm.subtenant, 10) : null;
  if (parsedForm.hideFooterLinks) {
    config.preferences = { ...(config.preferences || {}), lOFooter: false };
  } else if (config.preferences) {
    delete config.preferences.lOFooter;
  }
  const data = {
    configuration: JSON.stringify(config),
  };
  delete parsedForm.subtenant;
  delete parsedForm.autoCreateSubtenant;
  delete parsedForm.sectionPerOffering;
  delete parsedForm.hideFooterLinks;
  return {
    dto: { data },
    parsedForm: parsedForm,
  };
};

export default {
  componentId: 'loi.cp.lti.BasicLTIProducerSystem',
  component: LtiProvider,
  validateForm: validateForm,
};
