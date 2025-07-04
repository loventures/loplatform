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

import { defaultTo, keyBy, map } from 'lodash';
import PropTypes from 'prop-types';
import React, { Component } from 'react';

import {
  AdminFormCheck,
  AdminFormField,
  AdminFormSecret,
  AdminFormSelect,
} from '../../components/adminForm';

class CourseStructureUpload extends Component {
  constructor(props) {
    super(props);
    this.state = {
      useInstanceProfile: defaultTo(props.row.useInstanceProfile, true),
      useAssumeRole: defaultTo(props.row.useAssumeRole, true),
    };
  }

  toggleInstanceProfile = e => {
    this.setState({
      useInstanceProfile: e.target.checked,
    });
  };

  toggleAssumeRole = e => {
    this.setState({
      useAssumeRole: e.target.checked,
    });
  };

  render() {
    const { T, row, configs, renderField } = this.props;
    const { useInstanceProfile, useAssumeRole } = this.state;

    const c = keyBy(configs, 'id');

    const awsRegions = [
      'us-east-1',
      'us-east-2',
      'us-west-1',
      'us-west-2',
      'af-south-1',
      'ap-east-1',
      'ap-south-1',
      'ap-northeast-3',
      'ap-northeast-2',
      'ap-southeast-1',
      'ap-southeast-2',
      'ap-northeast-1',
      'ca-central-1',
      'cn-north-1',
      'cn-northwest-1',
      'eu-central-1',
      'eu-west-1',
      'eu-west-2',
      'eu-south-1',
      'eu-west-3',
      'eu-north-1',
      'me-south-1',
      'sa-east-1',
    ];
    const awsRegionOptions = map(awsRegions, r => ({ id: r, text: r, name: r }));

    const getValue = (id, defaultValue) => (row && row[id]) || defaultValue || '';

    return (
      <React.Fragment>
        <AdminFormSelect
          key={c.region.id}
          field={c.region.id}
          inputName={c.region.id}
          value={getValue(c.region.id, 'us-east-1')}
          options={awsRegionOptions}
          entity="connectors"
          T={T}
        />
        {renderField(c.bucket)}
        {renderField(c.prefix)}
        <AdminFormCheck
          key={c.useInstanceProfile.id}
          field={c.useInstanceProfile.id}
          value={getValue(c.useInstanceProfile.id, this.state.useInstanceProfile)}
          onChange={this.toggleInstanceProfile}
          entity="connectors"
          T={T}
        />
        <AdminFormCheck
          key={c.useAssumeRole.id}
          field={c.useAssumeRole.id}
          value={getValue(c.useAssumeRole.id, this.state.useAssumeRole)}
          onChange={this.toggleAssumeRole}
          entity="connectors"
          T={T}
        />
        <AdminFormField
          key={c.assumeRoleArn.id}
          field={c.assumeRoleArn.id}
          value={getValue(c.assumeRoleArn.id)}
          disabled={!useAssumeRole}
          entity="connectors"
          type="text"
          T={T}
        />
        <AdminFormField
          key={c.accessKeyId.id}
          field={c.accessKeyId.id}
          value={getValue(c.accessKeyId.id)}
          disabled={useInstanceProfile}
          entity="connectors"
          type="text"
          T={T}
        />
        <AdminFormSecret
          key={c.secretAccessKey.id}
          field={c.secretAccessKey.id}
          disabled={useInstanceProfile}
          value={getValue(c.secretAccessKey.id)}
          entity="connectors"
          type="text"
          T={T}
        />
      </React.Fragment>
    );
  }
}

CourseStructureUpload.propTypes = {
  T: PropTypes.object.isRequired,
  row: PropTypes.object.isRequired,
  configs: PropTypes.array.isRequired,
  renderField: PropTypes.func.isRequired,
};

const validateForm = parsedForm => {
  const config = {};
  config.region = parsedForm.region;
  config.bucket = parsedForm.bucket;
  config.prefix = parsedForm.prefix;
  config.useInstanceProfile = parsedForm.useInstanceProfile === 'on';
  config.useAssumeRole = parsedForm.useAssumeRole === 'on';
  config.assumeRoleArn = parsedForm.assumeRoleArn;
  config.accessKeyId = parsedForm.accessKeyId;
  config.secretAccessKey = parsedForm.secretAccessKey;
  parsedForm.useInstanceProfile = !!parsedForm.useInstanceProfile; //always have a value, it is better this way
  parsedForm.useAssumeRole = !!parsedForm.useAssumeRole;
  return {
    dto: {},
    parsedForm: parsedForm,
  };
};

export default {
  componentId: 'loi.cp.structure.CourseStructureUploadSystemImpl',
  component: CourseStructureUpload,
  validateForm: validateForm,
};
