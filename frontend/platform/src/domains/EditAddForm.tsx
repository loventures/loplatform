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
import Polyglot from 'node-polyglot';
import React, { useEffect, useState } from 'react';
import { FormText } from 'reactstrap';

import {
  AdminFormCheck,
  AdminFormCombobox,
  AdminFormField,
  AdminFormSection,
  AdminFormSelect,
} from '../components/adminForm';
import { LoPlatform } from '../types/loPlatform';
import { LocalesUrl, TimeZonesUrl } from '../services/URLs';

interface Option {
  id: string;
  name: string;
  displayString?: string;
  [key: string]: unknown;
}

interface Column {
  dataField: string;
  required?: boolean;
  [key: string]: unknown;
}

interface ProfileConfig {
  id: string;
  name: string;
  type: string;
  default?: unknown;
  noName?: string;
  yesName?: string;
}

interface Profile {
  identifier: string;
  name: string;
  configs: ProfileConfig[];
}

interface EditAddFormProps {
  row: Record<string, any>;
  columns: Column[];
  profile: Profile | null;
  validationErrors: Record<string, string>;
  dnsSupported: boolean;
  translations: Polyglot;
  lo_platform: LoPlatform;
  redshiftSchemaNames: string[];
}

// TODO: ComboBox craply configured to require both name (for initial selection display)
// and displayString (for the dropdown).
const mogrify = (a: Option[]): Option[] => a.map(o => ({ ...o, displayString: o.name }));

const EditAddForm: React.FC<EditAddFormProps> = ({
  row,
  columns,
  profile,
  validationErrors,
  dnsSupported,
  translations: T,
  redshiftSchemaNames,
}) => {
  const [locales, setLocales] = useState<Option[]>([]);
  const [timeZones, setTimeZones] = useState<Option[]>([]);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    axios
      .all([axios.get(TimeZonesUrl), axios.get(LocalesUrl)])
      .then(
        axios.spread((timeZonesRes, localesRes) => {
          setLoaded(true);
          setTimeZones(
            mogrify(
              timeZonesRes.data.objects.map((o: Option) => ({
                ...o,
                name: T.t('adminForm.format.nameId', o),
              }))
            )
          );
          setLocales(
            mogrify(
              localesRes.data.objects.map((o: Option) => ({
                ...o,
                name: T.t('adminForm.format.nameId', o),
              }))
            )
          );
        })
      )
      .catch(err => {
        console.log(err);
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const renderDomainDetails = () => {
    const fields = ['domainId', 'name', 'shortName', 'primaryHostName', 'additionalHostNames'];
    return fields.map(field => {
      const col = columns.find(col => col.dataField === field);
      const isRequired = (col && col.required) || field === 'primaryHostName';
      const value =
        field !== 'additionalHostNames'
          ? row[field]
          : (row.hostNames || [])
              .filter((h: string) => h !== row.primaryHostName)
              .join(', ');
      return (
        <AdminFormField
          key={field}
          entity="domains"
          field={field}
          value={value}
          invalid={validationErrors[field]}
          required={isRequired}
          autoFocus={field === 'userName'}
          T={T}
        />
      );
    });
  };

  const renderOptions = () => {
    const securityLevels = mogrify(
      ['NoSecurity', 'SecureAlways'].map(level => ({
        id: level,
        name: T.t(`adminPage.domains.securityLevel.${level}`),
      }))
    );
    const securityLevel = securityLevels.find(
      level => level.id === (row.securityLevel || 'SecureAlways')
    );
    const locale = locales.find(locale => locale.id === (row.locale || 'en-US'));
    const timeZone = timeZones.find(locale => locale.id === (row.timeZone || 'US/Eastern'));
    return [
      <AdminFormCombobox
        key="securityLevel"
        entity="domains"
        field="securityLevel"
        value={securityLevel}
        options={securityLevels}
        required={true}
        T={T}
      />,
      <AdminFormCombobox
        key="locale"
        entity="domains"
        field="locale"
        value={locale}
        options={locales}
        required={true}
        T={T}
      />,
      <AdminFormCombobox
        key="timeZone"
        entity="domains"
        field="timeZone"
        value={timeZone}
        options={timeZones}
        required={true}
        T={T}
      />,
    ];
  };

  const renderChecks = () => {
    return [
      <AdminFormCheck
        disabled={!dnsSupported}
        key="requestDns"
        entity="domains"
        field="requestDns"
        value={dnsSupported}
        T={T}
      />,
    ].concat(
      row.id ? (
        []
      ) : (
        <AdminFormCheck
          key="launchAdmin"
          entity="domains"
          field="launchAdmin"
          value={true}
          T={T}
        />
      )
    );
  };

  const renderProfileConfig = (cf: ProfileConfig) => {
    // this ignores hidden...
    const baseProps = {
      key: cf.id,
      entity: 'profile',
      field: cf.id,
      label: cf.name,
      inputName: `cf-${cf.id}`,
      value: cf['default'],
    };
    if (cf.id === 'redshiftSchemaName') {
      const names = redshiftSchemaNames.join(', ');
      const schemaHelp = (
        <React.Fragment>
          <FormText id="existingSchemas">
            {T.t('adminPage.domains.help.existingSchemas', { names })}
          </FormText>
        </React.Fragment>
      );

      const props = {
        ...baseProps,
        help: schemaHelp,
      };
      return <AdminFormField {...(props as any)} />;
    } else if (cf.type === 'Boolean') {
      return <AdminFormCheck {...(baseProps as any)} />;
    } else if (cf.type === 'Choice') {
      const props = {
        ...baseProps,
        value: baseProps.value ? 'on' : '',
        options: [
          { id: '', name: cf.noName },
          { id: 'on', name: cf.yesName },
        ],
      };
      return <AdminFormSelect {...(props as any)} />;
    } else if (cf.type === 'String') {
      return <AdminFormField {...(baseProps as any)} />;
    } else {
      // Select
      return null;
    }
  };

  if (!loaded) return null;
  const baseSectionProps = {
    page: 'domains',
    translations: T,
  };
  return (
    <React.Fragment>
      {row.id && <div className="entity-id">{row.id}</div>}
      <AdminFormSection
        {...baseSectionProps}
        section="domainSettings"
      >
        {renderDomainDetails()}
        {renderOptions()}
      </AdminFormSection>
      {profile && !!profile.configs.length && (
        <AdminFormSection
          {...baseSectionProps}
          section="profileSettings"
        >
          {profile.configs.map(renderProfileConfig)}
        </AdminFormSection>
      )}
      <AdminFormSection
        {...baseSectionProps}
        section="createOptions"
      >
        {renderChecks()}
      </AdminFormSection>
    </React.Fragment>
  );
};

export default EditAddForm;
