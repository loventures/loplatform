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
import serialize from 'form-serialize';
import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { Button, Col, Form, Row } from 'reactstrap';
import { bindActionCreators } from 'redux';

import {
  AdminFormColor,
  AdminFormCombobox,
  AdminFormField,
  AdminFormFile,
  AdminFormTitle,
} from '../components/adminForm';
import WaitDotGif from '../components/WaitDotGif';
import * as MainActions from '../redux/actions/MainActions';
import { DomainSettingsUrl, LocalesUrl, LoPlatformUrl, TimeZonesUrl } from '../services/URLs';
import { IoOptionsOutline } from 'react-icons/io5';

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      settings: {},
      timeZones: [],
      locales: [],
      loaded: false,
      submitting: false,
      validationErrors: {},
    };
  }

  componentDidMount() {
    const { translations: T, setPortalAlertStatus } = this.props;
    axios
      .all([DomainSettingsUrl, TimeZonesUrl, LocalesUrl].map(axios.get))
      .then(
        axios.spread((domainSettings, timeZones, locales) => {
          setPortalAlertStatus(false, false, '');
          this.setState({
            loaded: true,
            settings: domainSettings.data,
            timeZones: timeZones.data.objects.map(o => ({
              ...o,
              displayString: T.t('adminForm.format.nameId', o),
            })),
            locales: locales.data.objects.map(o => ({
              ...o,
              displayString: T.t('adminForm.format.nameId', o),
            })),
          });
        })
      )
      .catch(err => {
        console.log(err);
        setPortalAlertStatus(true, false, T.t('adminPage.domainSettings.alert.fetchError'));
      });
  }

  requiredFields = { name: true, shortName: true };

  submitSettings = e => {
    e.preventDefault();
    const { translations: T, setPortalAlertStatus } = this.props;
    const requiredFields = this.requiredFields;

    const el = e.target;
    const form = serialize(el, { hash: true, empty: true });
    const missing = Array.from(el.elements)
      .map(el => el.name)
      .find(field => requiredFields[field] && !form[field]);
    if (missing) {
      const params = { field: T.t(`adminPage.domainSettings.fieldName.${missing}`) };
      this.setState({
        validationErrors: { [missing]: T.t('adminForm.validation.fieldIsRequired', params) },
      });
      setPortalAlertStatus(true, false, T.t('adminPage.domainSettings.alert.formError'));
    } else {
      this.setState({ submitting: true, validationErrors: {} });
      this.clearPortalAlert();
      axios
        .put(DomainSettingsUrl, form)
        .then(res => {
          setPortalAlertStatus(false, true, T.t('adminPage.domainSettings.alert.success'));
          this.setState({ settings: res.data });
          axios.get(LoPlatformUrl).then(res => this.props.setLoPlatform(res.data));
        })
        .catch(err => {
          console.log(err);
          const data = err.response && err.response.data;
          if (data && data.type === 'VALIDATION_ERROR' && Array.isArray(data.messages)) {
            const validationErrors = data.messages.reduce((result, { property, message }) => {
              if (property.endsWith('Upload')) {
                const field = property.replace('Upload', '');
                const params = {
                  field: T.t(`adminPage.domainSettings.fieldName.${field}`),
                  message,
                };
                return { [field]: T.t('adminForm.validation.invalidUpload', params), ...result };
              } else {
                return { [property]: message, ...result };
              }
            }, {});
            this.setState({ validationErrors });
            setPortalAlertStatus(true, false, T.t('adminPage.domainSettings.alert.formError'));
          } else {
            setPortalAlertStatus(true, false, T.t('adminPage.domainSettings.alert.failure'));
          }
        })
        .then(() => {
          this.setState({ submitting: false });
        });
    }
  };

  clearPortalAlert = () => this.props.setPortalAlertStatus(false, false, '');

  render() {
    const { translations: T } = this.props;

    const { settings, loaded, locales, submitting, timeZones, validationErrors } = this.state;

    const entity = 'domainSettings';
    const defaultColors = {
      primaryColor: '#566B10',
      secondaryColor: '#6B8FC3',
      accentColor: '#84382E',
    };
    const requiredFields = this.requiredFields;
    const fieldUrl = field => `${DomainSettingsUrl}/${field}`;

    return (
      <div
        id="domain-settings-container"
        className="container"
      >
        {loaded && (
          <Form
            onSubmit={this.submitSettings}
            className="admin-form"
          >
            <AdminFormTitle title={T.t('adminPage.domainSettings.title.settings')} />

            <div className="mt-3">
              {[
                'name',
                'description',
                'shortName',
                'defaultLanguage',
                'defaultTimeZone',
                'googleAnalyticsAccount',
                'supportEmail',
              ].map(field => {
                if (field === 'defaultLanguage') {
                  const value = locales.find(o => o.id === settings[field]);
                  return (
                    <AdminFormCombobox
                      key={field}
                      entity={entity}
                      field={field}
                      value={value}
                      T={T}
                      options={locales}
                      required
                    />
                  );
                } else if (field === 'defaultTimeZone') {
                  const value = timeZones.find(o => o.id === settings[field]);
                  return (
                    <AdminFormCombobox
                      key={field}
                      entity={entity}
                      field={field}
                      value={value}
                      T={T}
                      options={timeZones}
                      required
                    />
                  );
                } else {
                  return (
                    <AdminFormField
                      key={field}
                      entity={entity}
                      field={field}
                      value={settings[field]}
                      required={requiredFields[field]}
                      invalid={validationErrors[field]}
                      autoFocus={field === 'name'}
                      T={T}
                      type={field === 'supportEmail' ? 'email' : 'text'}
                    />
                  );
                }
              })}
            </div>

            <AdminFormTitle title={T.t('adminPage.domainSettings.title.appearance')} />

            {/* Appending the PK to the key forces a re-render of the component when the settings are saved. */}
            <div className="mt-3">
              {['icon', 'image', 'logo', 'logo2', 'css'].map(field => {
                return (
                  <AdminFormFile
                    key={field + (settings[field] ? settings[field].id : '')}
                    entity={entity}
                    field={field}
                    fieldUrl={fieldUrl(field)}
                    value={settings[field]}
                    invalid={validationErrors[field]}
                    image={field !== 'css'}
                    T={T}
                  />
                );
              })}
            </div>

            <AdminFormTitle title={T.t('adminPage.domainSettings.title.colors')} />

            <div className="mt-3">
              {['primaryColor', 'secondaryColor', 'accentColor'].map(field => {
                return (
                  <AdminFormColor
                    key={field}
                    entity={entity}
                    field={field}
                    inputName={`styleVariables[${field}]`}
                    value={settings.styleVariables[field] || defaultColors[field]}
                    T={T}
                  />
                );
              })}
            </div>

            <AdminFormTitle title={T.t('adminPage.domainSettings.title.media')} />

            <div className="mt-3">
              {['robots', 'sitemap', 'mimeTypes'].map(field => {
                return (
                  <AdminFormFile
                    key={field + (settings[field] ? settings[field].id : '')}
                    entity={entity}
                    field={field}
                    fieldUrl={fieldUrl(field)}
                    value={settings[field]}
                    invalid={validationErrors[field]}
                    T={T}
                  />
                );
              })}
            </div>

            <Row className="my-4">
              <Col xs={{ offset: 2, size: 10 }}>
                <Button
                  id="domainSettings-submit"
                  className="px-5"
                  color="primary"
                  disabled={submitting}
                >
                  {T.t('adminPage.domainSettings.action.save')}
                  {submitting && (
                    <WaitDotGif
                      className="ms-2 waiting"
                      color="light"
                      size={16}
                    />
                  )}
                </Button>
              </Col>
            </Row>
          </Form>
        )}
      </div>
    );
  }
}

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const DomainSettings = connect(mapStateToProps, mapDispatchToProps)(App);

App.propTypes = {
  translations: PropTypes.object.isRequired,
  setPortalAlertStatus: PropTypes.func.isRequired,
  setLoPlatform: PropTypes.func.isRequired,
};

DomainSettings.pageInfo = {
  identifier: 'domainSettings',
  icon: IoOptionsOutline,
  link: '/DomainSettings',
  group: 'domain',
  right: 'loi.cp.admin.right.AdminRight',
};

export default DomainSettings;
