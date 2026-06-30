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
import React, { useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import {
  ButtonDropdown,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  Modal,
  ModalBody,
  ModalHeader,
} from 'reactstrap';

import { PageInfo } from '../adminPortal/types';
import ReactTable from '../components/reactTable/ReactTable';
import WaitDotGif from '../components/WaitDotGif';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useLoPlatform, useTranslations } from '../redux/state';
import { asjax } from '../services';
import { OverlordRight, SupportRight, hasRight } from '../services/Rights';
import { DomainProfilesUrl, RedshiftSchemaNamesUrl } from '../services/URLs';
import EditAddForm from './EditAddForm';

const MaxMessages = 64;

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

interface DomainRow {
  id: number;
  state?: string;
  primaryHostName?: string;
  [key: string]: any;
}

interface DomainsPageInfo extends Omit<PageInfo, 'icon'> {
  iconName: string;
}

const Domains: React.FC & { pageInfo: DomainsPageInfo } = () => {
  const T = useTranslations();
  const lo_platform = useLoPlatform();
  const dispatch = useDispatch();

  const [dnsSupported, setDnsSupported] = useState(false);
  const [profiles, setProfiles] = useState<Profile[]>([]);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [stateOpen, setStateOpen] = useState(false);
  const [provisioning, setProvisioning] = useState(false);
  const [skipped, setSkipped] = useState(0);
  const [messages, setMessages] = useState<string[]>([]);
  const [redshiftSchemaNames, setRedshiftSchemaNames] = useState<string[]>([]);

  const refreshTable = useRef<() => void>(() => null);
  const msgsRef = useRef<HTMLDivElement | null>(null);

  const refresh = () => refreshTable.current();

  useEffect(() => {
    axios.get(DomainProfilesUrl).then(({ data: { dnsSupported, profiles } }) => {
      setDnsSupported(dnsSupported);
      setProfiles(profiles);
    });
    axios.get(RedshiftSchemaNamesUrl).then(({ data: names }) => {
      setRedshiftSchemaNames(names.objects);
    });
    const onMessage = (e: MessageEvent) => {
      // well hello there. a hacked broadcast message to refresh the table...
      if (e.source === window && e.data === 'maintenance') refresh();
    };
    window.addEventListener('message', onMessage, false); // what monsters we've become
    return () => {
      window.removeEventListener('message', onMessage, false); // what monsters we've become
    };
     
  }, []);

  useEffect(() => {
    if (msgsRef.current) {
      const el = msgsRef.current;
      setTimeout(() => {
        el.scrollTop = el.scrollHeight;
      }, 1);
    }
  });

  const formatHostNames = (hostNames: string[], { primaryHostName }: DomainRow) =>
    [...new Set(hostNames.concat(primaryHostName as string))].sort().join(', ');

  const generateColumns = () => {
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
        dataFormat: formatHostNames,
      },
    ];
    return columns;
  };

  const renderForm = (row: DomainRow, validationErrors: Record<string, string>) => {
    return (
      <EditAddForm
        translations={T}
        row={row}
        validationErrors={validationErrors}
        profile={profile}
        dnsSupported={dnsSupported}
        columns={generateColumns()}
        lo_platform={lo_platform}
        redshiftSchemaNames={redshiftSchemaNames}
      />
    );
  };

  const validateForm = (form: Record<string, any>, row: DomainRow, el: HTMLFormElement) => {
    const additional = (form.additionalHostNames || '').split(/\s*,\s*/).filter((s: string) => !!s);
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
      profile: profile,
      requestDns: form.requestDns === 'on',
      launchAdmin: form.launchAdmin === 'on',
      form,
    };
    const isRequired = (field: string) =>
      ['domainId', 'name', 'shortName', 'primaryHostName'].indexOf(field) >= 0;
    const missing = Array.from(el.elements)
      .map(el => (el as HTMLInputElement).name)
      .find(field => isRequired(field) && !form[field]); // in order of elements for uxiness
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

  const showProgress = (s: string) => {
    setProvisioning(true);
    setMessages(prev => {
      const add = [s]; // s.split("\n");
      const skip = Math.max(0, add.length + prev.length - MaxMessages);
      setSkipped(prevSkipped => prevSkipped + skip);
      return prev.slice(skip).concat(add);
    });
  };

  const initDomain = (domain: DomainRow) => {
    setProvisioning(true);
    setSkipped(0);
    setMessages([]);
    showProgress('Initializing domain...');
    return asjax(`/api/v2/domains/${domain.id}/init`, {}, showProgress);
  };

  const applyProfile = (domain: DomainRow, prof: Profile, form: Record<string, any>) => {
    showProgress('Applying profile...');
    const extract = (cf: ProfileConfig) => {
      const name = `cf-${cf.id}`,
        value = form[name];
      return cf.type === 'Boolean' || cf.type === 'Choice' ? !!value : value;
    };
    const config = prof.configs.reduce<Record<string, unknown>>(
      (o, cf) => ({ ...o, [cf.id]: extract(cf) }),
      {}
    );
    return asjax(`/api/v2/domains/${domain.id}/bootstrap/${prof.identifier}`, config, showProgress);
  };

  const dnsCheck = (domain: DomainRow) => {
    showProgress('Requesting DNS...');
    return asjax(`/api/v2/domains/${domain.id}/requestDns`, {}, showProgress).then((data: any) => {
      if (data.left) {
        showProgress(data.a);
        return Promise.reject({ response: { data: { type: 'ModalError', message: data.a } } });
      } else {
        showProgress(data.b);
      }
    });
  };

  const sudo = (selectedRow: DomainRow) => {
    return axios.post('/api/v2/domains/' + selectedRow.id + '/manage').then(() => {
      window.history.replaceState(
        {},
        'Exit',
        '/sys/eunt/domus' + window.location.pathname + '?user=' + lo_platform.user.id
      );
      window.top!.location.href = '/Administration';
      return false;
    });
  };

  const adminDomain = (domain: DomainRow) => {
    showProgress('Launching domain...');
    sudo(domain);
    return false;
  };

  const done = (response: any) => {
    setProvisioning(false);
    return response;
  };

  const postCreate = (
    response: any,
    { create, profile, form, requestDns, launchAdmin }: any
  ) => {
    const domain = response.data;
    if (create) {
      return initDomain(domain)
        .then(() => applyProfile(domain, profile, form))
        .then(() => requestDns && dnsCheck(domain))
        .then(() => (launchAdmin ? adminDomain(domain) : done(response)));
    } else if (requestDns) {
      dnsCheck(domain).then(() => response);
    } else {
      return response;
    }
  };

  const transition = (selectedRow: DomainRow, state: string) => {
    return axios
      .post('/api/v2/domains/' + selectedRow.id + '/state', {
        state: state,
        message: null,
      })
      .then(refresh);
  };

  const isOverlord = () => hasRight(lo_platform.user, OverlordRight);

  // passing in togglePopover is verging awful
  const getButtonInfo = (
    selectedRow: DomainRow | null,
    togglePopover: (name: string, open: boolean) => void
  ) => {
    const toggleState = () => {
      setStateOpen(prev => !prev);
      togglePopover('download', false);
    };
    const ol = isOverlord();
    const stater = (
      <ButtonDropdown
        key="state"
        isOpen={!!selectedRow && stateOpen}
        toggle={toggleState}
      >
        <DropdownToggle
          caret
          className="glyphButton"
          disabled={!selectedRow}
          id="react-table-state-button"
          onMouseOver={() => togglePopover('state', !stateOpen)}
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
              onClick={() => transition(selectedRow!, f)}
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
      onClick: sudo,
      className: !ol && 'lastButton',
    };
    return isOverlord() ? [stater, sudoer] : [sudoer];
  };

  const generateDropdownItems = () => {
    return profiles.map(profile => {
      return {
        name: profile.name,
        key: profile.identifier,
        onClick: () => setProfile(profile),
      };
    });
  };

  const trClassFormat = ({ state }: DomainRow) =>
    state === 'Suspended' ? 'row-disabled' : state === 'Maintenance' ? 'row-maintenance' : '';

  const ol = isOverlord();
  return (
    <React.Fragment>
      <ReactTable
        entity="domains"
        autoComplete="off"
        refreshRef={r => (refreshTable.current = r)}
        columns={generateColumns()}
        defaultSortField="name"
        defaultSearchField="meta"
        renderForm={renderForm}
        validateForm={validateForm}
        translations={T}
        setPortalAlertStatus={(error: any, success: boolean, message: string) =>
          dispatch(setPortalAlertStatus(error, success, message))
        }
        getButtons={getButtonInfo}
        trClassFormat={trClassFormat}
        openRow={sudo}
        createButton={false}
        createDropdown={ol}
        deleteButton={ol}
        updateButton={ol}
        dropdownItems={generateDropdownItems()}
        afterCreateOrUpdate={postCreate}
      />
      ,
      <Modal
        size="xl"
        backdrop="static"
        isOpen={provisioning}
        className="domain-provision"
      >
        <ModalHeader tag="h2">
          {T.t('adminPage.domains.provisioning')}
          <WaitDotGif
            style={{ position: 'absolute', right: '1.5rem' }}
            color="dark"
            size={16}
          />
        </ModalHeader>
        <ModalBody>
          <div
            className="domain-messages"
            ref={msgsRef}
          >
            {messages.map((msg, idx) => (
              <div key={skipped + idx}>{msg}</div>
            ))}
          </div>
        </ModalBody>
      </Modal>
    </React.Fragment>
  );
};

Domains.pageInfo = {
  identifier: 'domains',
  iconName: 'video_label',
  link: '/Domains',
  group: 'Overlord',
  right: SupportRight,
};

export default Domains;
