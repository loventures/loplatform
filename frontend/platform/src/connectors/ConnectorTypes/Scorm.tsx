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

import axios from 'axios';
import React, { useEffect, useState } from 'react';

import { AdminFormSelect } from '../../components/adminForm';
import { SrsCollection } from '../../srs';
import { ConnectorComponentProps, ParsedForm, ValidateFormResult } from '../types';

interface Subtenant {
  id: number;
  name: string;
}

const Scorm: React.FC<ConnectorComponentProps> = ({ T, row, configs, renderField }) => {
  const [subtenants, setSubtenants] = useState<Subtenant[]>([]);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    axios.get<SrsCollection<Subtenant>>('/api/v2/subtenants').then(res => {
      setSubtenants(res.data.objects);
      setLoaded(true);
    });
  }, []);

  if (!loaded) return null;
  const field = 'subtenant';
  const options: { key: string | number; id: string | number; text: string }[] = (
    [{ key: 'empty', id: 'empty', text: '' }] as {
      key: string | number;
      id: string | number;
      text: string;
    }[]
  ).concat(subtenants.map(sub => ({ key: sub.id, id: sub.id, text: sub.name })));
  const configuration = row.configuration ? JSON.parse(row.configuration) : {};
  const subtenantVal = configuration.subtenant + '' || '';
  const noConfigs: Record<string, boolean> = {
    configuration: true,
    useExternalIdentifier: true,
    usernameParameter: true,
  };
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
};

const validateForm = (parsedForm: ParsedForm): ValidateFormResult => {
  const config: Record<string, any> = {};
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
