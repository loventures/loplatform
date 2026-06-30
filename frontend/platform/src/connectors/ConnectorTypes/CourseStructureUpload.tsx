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

import { defaultTo, keyBy, map } from 'lodash';
import React, { useState } from 'react';

import {
  AdminFormCheck,
  AdminFormField,
  AdminFormSecret,
  AdminFormSelect,
} from '../../components/adminForm';
import { ConnectorComponentProps, ParsedForm, ValidateFormResult } from '../types';

const CourseStructureUpload: React.FC<ConnectorComponentProps> = ({
  T,
  row,
  configs,
  renderField,
}) => {
  const [useInstanceProfile, setUseInstanceProfile] = useState<boolean>(
    defaultTo(row.useInstanceProfile, true)
  );
  const [useAssumeRole, setUseAssumeRole] = useState<boolean>(defaultTo(row.useAssumeRole, true));

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
  const awsRegionOptions = map(awsRegions, (r: string) => ({ id: r, text: r, name: r }));

  const getValue = (id: string, defaultValue?: string | boolean): any =>
    (row && row[id]) || defaultValue || '';

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
        value={getValue(c.useInstanceProfile.id, useInstanceProfile)}
        onChange={e => setUseInstanceProfile(e.target.checked)}
        entity="connectors"
        T={T}
      />
      <AdminFormCheck
        key={c.useAssumeRole.id}
        field={c.useAssumeRole.id}
        value={getValue(c.useAssumeRole.id, useAssumeRole)}
        onChange={e => setUseAssumeRole(e.target.checked)}
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
};

const validateForm = (parsedForm: ParsedForm): ValidateFormResult => {
  const config: Record<string, any> = {};
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
