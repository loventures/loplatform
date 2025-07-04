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
import React from 'react';
import { FormText } from 'reactstrap';

import {
  AdminFormCheck,
  AdminFormCombobox,
  AdminFormField,
  AdminFormSection,
  AdminFormSelect,
} from '../components/adminForm';
import LoPropTypes from '../react/loPropTypes';
import { LocalesUrl, TimeZonesUrl } from '../services/URLs.js';

// TODO: ComboBox craply configured to require both name (for initial selection display)
// and displayString (for the dropdown).
const mogrify = a => a.map(o => ({ ...o, displayString: o.name }));

class EditAddForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = { locales: [], timeZones: [], loaded: false };
  }

  componentDidMount() {
    const { translations: T } = this.props;
    axios
      .all([axios.get(TimeZonesUrl), axios.get(LocalesUrl)])
      .then(
        axios.spread((timeZones, locales) => {
          this.setState({
            loaded: true,
            timeZones: mogrify(
              timeZones.data.objects.map(o => ({ ...o, name: T.t('adminForm.format.nameId', o) }))
            ),
            locales: mogrify(
              locales.data.objects.map(o => ({ ...o, name: T.t('adminForm.format.nameId', o) }))
            ),
          });
        })
      )
      .catch(err => {
        console.log(err);
      });
  }

  renderDomainDetails = () => {
    const { columns, validationErrors, row, translations: T } = this.props;
    const fields = ['domainId', 'name', 'shortName', 'primaryHostName', 'additionalHostNames'];
    return fields.map(field => {
      const col = columns.find(col => col.dataField === field);
      const isRequired = (col && col.required) || field === 'primaryHostName';
      const value =
        field !== 'additionalHostNames'
          ? row[field]
          : (row.hostNames || []).filter(h => h !== row.primaryHostName).join(', ');
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

  renderOptions = () => {
    const { row, translations: T } = this.props;
    const securityLevels = mogrify(
      ['NoSecurity', 'SecureAlways'].map(level => ({
        id: level,
        name: T.t(`adminPage.domains.securityLevel.${level}`),
      }))
    );
    const securityLevel = securityLevels.find(
      level => level.id === (row.securityLevel || 'SecureAlways')
    );
    const locale = this.state.locales.find(locale => locale.id === (row.locale || 'en-US'));
    const timeZone = this.state.timeZones.find(
      locale => locale.id === (row.timeZone || 'US/Eastern')
    );
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
        options={this.state.locales}
        required={true}
        T={T}
      />,
      <AdminFormCombobox
        key="timeZone"
        entity="domains"
        field="timeZone"
        value={timeZone}
        options={this.state.timeZones}
        required={true}
        T={T}
      />,
    ];
  };

  renderChecks = () => {
    const { dnsSupported, row, translations: T } = this.props;
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

  renderProfileConfig = cf => {
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
      const { translations: T, redshiftSchemaNames } = this.props;
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
      return <AdminFormField {...props} />;
    } else if (cf.type === 'Boolean') {
      return <AdminFormCheck {...baseProps} />;
    } else if (cf.type === 'Choice') {
      const props = {
        ...baseProps,
        value: baseProps.value ? 'on' : '',
        options: [
          { id: '', name: cf.noName },
          { id: 'on', name: cf.yesName },
        ],
      };
      return <AdminFormSelect {...props} />;
    } else if (cf.type === 'String') {
      return <AdminFormField {...baseProps} />;
    } else {
      // Select
      return null;
    }
  };

  render() {
    if (!this.state.loaded) return null;
    const { profile, row, translations: T } = this.props;
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
          {this.renderDomainDetails()}
          {this.renderOptions()}
        </AdminFormSection>
        {profile && !!profile.configs.length && (
          <AdminFormSection
            {...baseSectionProps}
            section="profileSettings"
          >
            {profile.configs.map(this.renderProfileConfig)}
          </AdminFormSection>
        )}
        <AdminFormSection
          {...baseSectionProps}
          section="createOptions"
        >
          {this.renderChecks()}
        </AdminFormSection>
      </React.Fragment>
    );
  }
}

EditAddForm.propTypes = {
  row: PropTypes.object,
  columns: PropTypes.array.isRequired,
  profile: PropTypes.object,
  validationErrors: PropTypes.object,
  dnsSupported: PropTypes.bool,
  translations: LoPropTypes.translations,
  lo_platform: LoPropTypes.lo_platform,
  redshiftSchemaNames: PropTypes.array,
};

export default EditAddForm;
