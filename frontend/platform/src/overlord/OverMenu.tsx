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
import moment, { Moment } from 'moment-timezone';
import React, { useState } from 'react';
import { useDispatch } from 'react-redux';
import {
  Button,
  ButtonDropdown,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from 'reactstrap';

import { AdminFormDateTime } from '../components/adminForm';
import { setPortalAlertStatus } from '../redux/actions/MainActions';
import { useTranslations } from '../redux/state';
import { Hyperlink, ponyImageName, ponyName } from '../services';

const ponies = import.meta.glob<{ default: string }>('../imgs/ponies/*.svg');

const OverMenu: React.FC = () => {
  const T = useTranslations();
  const dispatch = useDispatch();
  const [open, setOpen] = useState(false);
  const [enteringMaintenance, setEnteringMaintenance] = useState(false);
  const [endDate, setEndDate] = useState<Moment | null>(null);
  const [description, setDescription] = useState<any>(null);
  const [image, setImage] = useState('');

  const about = () => {
    axios.get('/sys/describe').then(res => {
      setDescription(res.data);
      const path = '../imgs/ponies/' + ponyImageName(res.data.platform.version);
      ponies[path]?.().then(() => {
        const imgURL = new URL(path, import.meta.url);
        setImage(imgURL.href);
      });
    });
  };

  const enterMaintenance = () => {
    setEnteringMaintenance(true);
    setEndDate(moment());
  };

  const submitMaintenance = () => {
    setEnteringMaintenance(false);
    axios
      .put('/api/v2/domains/maintenance', { enabled: true, end: endDate?.toISOString() })
      .then(() => {
        dispatch(setPortalAlertStatus(false, true, T.t('overlord.alert.maintenanceEntered')));
        window.postMessage('maintenance', '*'); // what monsters we've become
      })
      .catch(error => {
        console.log(error);
        dispatch(setPortalAlertStatus(true, false, T.t('error.unexpectedError')));
      });
  };

  const exitMaintenance = () => {
    axios
      .put('/api/v2/domains/maintenance', { enabled: false })
      .then(() => {
        dispatch(setPortalAlertStatus(false, true, T.t('overlord.alert.maintenanceExited')));
        window.postMessage('maintenance', '*'); // what monsters we've become
      })
      .catch(error => {
        console.log(error);
        dispatch(setPortalAlertStatus(true, false, T.t('error.unexpectedError')));
      });
  };

  const invalidateCdn = () => {
    axios
      .post('/sys/cdn/antivenin')
      .then(() => {
        dispatch(setPortalAlertStatus(false, true, T.t('overlord.alert.cdnRefreshed')));
      })
      .catch(error => {
        console.log(error);
        dispatch(setPortalAlertStatus(true, false, T.t('error.unexpectedError')));
      });
  };

  const shutdownCluster = () => {
    window.top!.location.href = '/control/shutdown';
  };

  const toggleState = () => setOpen(o => !o);
  const hideModal = () => {
    setEnteringMaintenance(false);
    setDescription(null);
  };

  return (
    <div>
      <ButtonDropdown
        key="state"
        isOpen={open}
        toggle={toggleState}
      >
        <DropdownToggle
          color="transparent"
          className="glyphButton dropdown-toggle"
          id="overlord-menu-dropdown"
        >
          <i
            className="material-icons md-24"
            aria-hidden="true"
          >
            menu
          </i>
        </DropdownToggle>
        <DropdownMenu id="overlord-dropdown-menu">
          <DropdownItem
            id="overlord-about-button"
            onClick={about}
          >
            {T.t('about.page.name')}
          </DropdownItem>
          <DropdownItem
            id="overlord-enter-maintenance"
            onClick={enterMaintenance}
          >
            {T.t('overlord.menu.enterMaintenance')}
          </DropdownItem>
          <DropdownItem
            id="overlord-exit-maintenance"
            onClick={exitMaintenance}
          >
            {T.t('overlord.menu.exitMaintenance')}
          </DropdownItem>
          <DropdownItem
            id="overlord-invalidate-cdn"
            onClick={invalidateCdn}
          >
            {T.t('overlord.menu.invalidateCdn')}
          </DropdownItem>
          <DropdownItem
            id="overlord-shutdown-cluster"
            onClick={shutdownCluster}
          >
            {T.t('overlord.menu.shutdownCluster')}
          </DropdownItem>
        </DropdownMenu>
      </ButtonDropdown>
      {enteringMaintenance && (
        <Modal
          id="maintenance-modal"
          size="lg"
          isOpen={true}
          backdrop="static"
          toggle={hideModal}
        >
          <ModalHeader tag="h2">{T.t('overlord.maintenance.title')}</ModalHeader>
          <ModalBody className="admin-form">
            <AdminFormDateTime
              entity="maintenance"
              field="end"
              value={endDate?.toISOString()}
              label={T.t('overlord.maintenance.endTime')}
              onChange={date => setEndDate(date ? moment(date) : null)}
              T={T}
              required={true}
            />
          </ModalBody>
          <ModalFooter>
            <Button
              id="maintenance-close"
              onClick={hideModal}
            >
              {T.t('overlord.maintenance.close')}
            </Button>
            <Button
              id="maintenance-submit"
              className="ms-2"
              onClick={submitMaintenance}
              type="submit"
              color="primary"
            >
              {T.t('overlord.maintenance.submit')}
            </Button>
          </ModalFooter>
        </Modal>
      )}
      {description && (
        <Modal
          id="about-modal"
          size="md"
          isOpen={true}
          backdrop="static"
          toggle={hideModal}
        >
          <ModalHeader>{ponyName(description.platform.version)}</ModalHeader>
          <ModalBody>
            <div
              style={{
                height: '12rem',
                background: `center / contain no-repeat url(${image})`,
              }}
            >
              <div
                style={{
                  color: '#777',
                  fontWeight: '100',
                  fontSize: '12px',
                  position: 'absolute',
                  right: '1rem',
                  bottom: '.5rem',
                  textAlign: 'right',
                }}
              >
                <div>{T.t('about.buildDate', description.platform)}</div>
                <div>
                  <Hyperlink
                    label={T.t('about.commit', description.platform)}
                    style={{ color: '#777' }}
                    target="_blank"
                    href={description.platform.stashDetails}
                  />
                </div>
              </div>
            </div>
          </ModalBody>
          <ModalFooter>
            <Button onClick={hideModal}>{T.t('overlord.maintenance.close')}</Button>
          </ModalFooter>
        </Modal>
      )}
    </div>
  );
};

export default OverMenu;
