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
import { connect } from 'react-redux';
import {
  ButtonDropdown,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  Modal,
  ModalBody,
  ModalHeader,
} from 'reactstrap';
import { bindActionCreators } from 'redux';

import ReactTable from '../components/reactTable/ReactTable';
import WaitDotGif from '../components/WaitDotGif';
import * as MainActions from '../redux/actions/MainActions';
import { asjax } from '../services';
import { OverlordRight, SupportRight, hasRight } from '../services/Rights';
import { DomainProfilesUrl, RedshiftSchemaNamesUrl } from '../services/URLs.js';
import EditAddForm from './EditAddForm';

const MaxMessages = 64;

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      dnsSupported: false,
      profiles: [],
      profile: null,
      stateOpen: false,
      provisioning: false,
      skipped: 0,
      messages: [],
      redshiftSchemaNames: [],
    };
  }

  componentDidMount() {
    axios.get(DomainProfilesUrl).then(({ data: { dnsSupported, profiles } }) => {
      this.setState({ dnsSupported, profiles });
    });
    axios.get(RedshiftSchemaNamesUrl).then(({ data: names }) => {
      this.setState({ redshiftSchemaNames: names.objects });
    });
    window.addEventListener('message', this.onMessage, false); // what monsters we've become
  }

  componentWillUnmount() {
    window.removeEventListener('message', this.onMessage, false); // what monsters we've become
  }

  onMessage = e => {
    // well hello there. a hacked broadcast message to refresh the table...
    if (e.source === window && e.data === 'maintenance') this.refresh();
  };

  formatHostNames = (hostNames, { primaryHostName }) =>
    [...new Set(hostNames.concat(primaryHostName))].sort().join(', ');

  generateColumns = () => {
    const columns = [
      { dataField: 'id', isKey: true },
      { dataField: 'domainId', sortable: true, required: true, searchOperator: 'ts', width: '10%' },
      { dataField: 'shortName', sortable: true, required: true, width: '10%' },
      { dataField: 'name', sortable: true, required: true },
      { dataField: 'meta', searchable: true, hidden: true, searchOperator: 'ts' },
      {
        dataField: 'hostNames',
        sortable: true,
        searchable: true,
        required: true,
        dataFormat: this.formatHostNames,
      },
    ];
    return columns;
  };

  renderForm = (row, validationErrors) => {
    return (
      <EditAddForm
        translations={this.props.translations}
        row={row}
        validationErrors={validationErrors}
        profile={this.state.profile}
        dnsSupported={this.state.dnsSupported}
        columns={this.generateColumns()}
        lo_platform={this.props.lo_platform}
        redshiftSchemaNames={this.state.redshiftSchemaNames}
      />
    );
  };

  validateForm = (form, row, el) => {
    const additional = (form.additionalHostNames || '').split(/\s*,\s*/).filter(s => !!s);
    const data = {
      domainId: form.domainId,
      name: form.name,
      shortName: form.shortName,
      primaryHostName: form.primaryHostName,
      hostNames: [...new Set([...additional, form.primaryHostName])],
      locale: form.locale,
      timeZone: form.timeZone,
      securityLevel: form.securityLevel,
    };
    const extras = {
      create: !row.id,
      profile: this.state.profile,
      requestDns: form.requestDns === 'on',
      launchAdmin: form.launchAdmin === 'on',
      form,
    };
    const isRequired = field =>
      ['domainId', 'name', 'shortName', 'primaryHostName'].indexOf(field) >= 0;
    const missing = Array.from(el.elements)
      .map(el => el.name)
      .find(field => isRequired(field) && !form[field]); // in order of elements for uxiness
    const T = this.props.translations;
    if (missing) {
      const params = { field: T.t(`adminPage.domains.fieldName.${missing}`) };
      return {
        validationErrors: { [missing]: T.t('adminForm.validation.fieldIsRequired', params) },
      };
    } else if (!/^[a-z\d][a-z\d-_.]*$/i.test(data.domainId)) {
      const params = { field: T.t('adminPage.domains.fieldName.domainId') };
      return {
        validationErrors: { domainId: T.t('adminForm.validation.fieldMustBeValid', params) },
      };
    } else if (
      !/^[a-z\d]([a-z\d-]{0,61}[a-z\d])?(\.[a-z\d]([a-z\d-]{0,61}[a-z\d])?)*$/i.test(
        data.primaryHostName
      )
    ) {
      const params = { field: T.t('adminPage.domains.fieldName.primaryHostName') };
      return {
        validationErrors: { primaryHostName: T.t('adminForm.validation.fieldMustBeValid', params) },
      };
    } else {
      return { data, extras };
    }
  };

  componentDidUpdate() {
    if (this.msgs) {
      setTimeout(() => {
        this.msgs.scrollTop = this.msgs.scrollHeight;
      }, 1);
    }
  }

  showProgress = s => {
    this.setState(state => {
      const add = [s]; // s.split("\n");
      const skip = Math.max(0, add.length + state.messages.length - MaxMessages);
      return {
        provisioning: true,
        skipped: state.skipped + skip,
        messages: state.messages.slice(skip).concat(add),
      };
    });
  };

  postCreate = (response, { create, profile, form, requestDns, launchAdmin }) => {
    const domain = response.data;
    if (create) {
      return this.initDomain(domain)
        .then(() => this.applyProfile(domain, profile, form))
        .then(() => requestDns && this.dnsCheck(domain))
        .then(() => (launchAdmin ? this.adminDomain(domain) : this.done(response)));
    } else if (requestDns) {
      this.dnsCheck(domain).then(() => response);
    } else {
      return response;
    }
  };

  initDomain = domain => {
    this.setState({ provisioning: true, skipped: 0, messages: [] });
    this.showProgress('Initializing domain...');
    return asjax(`/api/v2/domains/${domain.id}/init`, {}, this.showProgress);
  };

  applyProfile = (domain, profile, form) => {
    this.showProgress('Applying profile...');
    const extract = cf => {
      const name = `cf-${cf.id}`,
        value = form[name];
      return cf.type === 'Boolean' || cf.type === 'Choice' ? !!value : value;
    };
    const config = profile.configs.reduce((o, cf) => ({ ...o, [cf.id]: extract(cf) }), {});
    return asjax(
      `/api/v2/domains/${domain.id}/bootstrap/${profile.identifier}`,
      config,
      this.showProgress
    );
  };

  dnsCheck = domain => {
    this.showProgress('Requesting DNS...');
    return asjax(`/api/v2/domains/${domain.id}/requestDns`, {}, this.showProgress).then(data => {
      if (data.left) {
        this.showProgress(data.a);
        return Promise.reject({ response: { data: { type: 'ModalError', message: data.a } } });
      } else {
        this.showProgress(data.b);
      }
    });
  };

  adminDomain = domain => {
    this.showProgress('Launching domain...');
    this.sudo(domain);
    return false;
  };

  done = response => {
    this.setState({ provisioning: false });
    return response;
  };

  transition = (selectedRow, state) => {
    return axios
      .post('/api/v2/domains/' + selectedRow.id + '/state', {
        state: state,
        message: null,
      })
      .then(this.refresh);
  };

  refresh = () => this.refreshTable();

  sudo = selectedRow => {
    return axios.post('/api/v2/domains/' + selectedRow.id + '/manage').then(() => {
      window.history.replaceState(
        {},
        'Exit',
        '/sys/eunt/domus' + window.location.pathname + '?user=' + this.props.lo_platform.user.id
      );
      window.top.location.href = '/Administration';
      return false;
    });
  };

  // passing in togglePopover is verging awful
  getButtonInfo = (selectedRow, togglePopover) => {
    const T = this.props.translations;
    const toggleState = () => {
      this.setState({ stateOpen: !this.state.stateOpen });
      togglePopover('download', false);
    };
    const ol = this.isOverlord();
    const stater = (
      <ButtonDropdown
        key="state"
        isOpen={!!selectedRow && this.state.stateOpen}
        toggle={toggleState}
      >
        <DropdownToggle
          caret
          className="glyphButton"
          disabled={!selectedRow}
          id="react-table-state-button"
          onMouseOver={() => togglePopover('state', !this.state.stateOpen)}
          onMouseOut={() => togglePopover('state', false)}
        >
          <i
            className="material-icons md-18"
            aria-hidden="true"
          >
            build
          </i>
        </DropdownToggle>
        <DropdownMenu>
          {['Normal', 'Maintenance', 'Suspended'].map(f => (
            <DropdownItem
              key={f}
              disabled={!!selectedRow && f === selectedRow.state}
              onClick={() => this.transition(selectedRow, f)}
            >
              {T.t(`adminPage.domains.state.${f}`)}
            </DropdownItem>
          ))}
        </DropdownMenu>
      </ButtonDropdown>
    );
    const sudoer = {
      name: 'sudo',
      iconName: 'directions_run',
      onClick: this.sudo,
      className: !ol && 'lastButton',
    };
    return this.isOverlord() ? [stater, sudoer] : [sudoer];
  };

  generateDropdownItems = () => {
    const { profiles } = this.state;
    return profiles.map(profile => {
      return {
        name: profile.name,
        key: profile.identifier,
        onClick: () => this.setState({ profile: profile }),
      };
    });
  };

  trClassFormat = ({ state }) =>
    state === 'Suspended' ? 'row-disabled' : state === 'Maintenance' ? 'row-maintenance' : '';

  isOverlord = () => hasRight(this.props.lo_platform.user, OverlordRight);

  render() {
    const T = this.props.translations;
    const { messages, provisioning, skipped } = this.state;
    const ol = this.isOverlord();
    return (
      <React.Fragment>
        <ReactTable
          entity="domains"
          autoComplete="off"
          refreshRef={r => (this.refreshTable = r)}
          columns={this.generateColumns()}
          defaultSortField="name"
          defaultSearchField="meta"
          renderForm={this.renderForm}
          validateForm={this.validateForm}
          translations={this.props.translations}
          setPortalAlertStatus={this.props.setPortalAlertStatus}
          getButtons={this.getButtonInfo}
          trClassFormat={this.trClassFormat}
          openRow={this.sudo}
          createButton={false}
          createDropdown={ol}
          deleteButton={ol}
          updateButton={ol}
          dropdownItems={this.generateDropdownItems()}
          afterCreateOrUpdate={this.postCreate}
        />
        ,
        <Modal
          size="xl"
          backdrop="static"
          isOpen={provisioning}
          className="domain-provision"
        >
          <ModalHeader tag="h2">
            {T.t`adminPage.domains.provisioning`}
            <WaitDotGif
              style={{ position: 'absolute', right: '1.5rem' }}
              color="dark"
              size={16}
            />
          </ModalHeader>
          <ModalBody>
            <div
              className="domain-messages"
              ref={e => (this.msgs = e)}
            >
              {messages.map((msg, idx) => (
                <div key={skipped + idx}>{msg}</div>
              ))}
            </div>
          </ModalBody>
        </Modal>
      </React.Fragment>
    );
  }
}

App.propTypes = {
  translations: PropTypes.object.isRequired,
  lo_platform: PropTypes.object.isRequired,
  setPortalAlertStatus: PropTypes.func.isRequired,
};

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
    lo_platform: state.main.lo_platform,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const Domains = connect(mapStateToProps, mapDispatchToProps)(App);

Domains.pageInfo = {
  identifier: 'domains',
  iconName: 'video_label',
  link: '/Domains',
  group: 'Overlord',
  right: SupportRight,
};

export default Domains;
