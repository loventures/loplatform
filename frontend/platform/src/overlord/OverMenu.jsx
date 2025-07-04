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
import moment from 'moment-timezone';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
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
import { bindActionCreators } from 'redux';

import { AdminFormDateTime } from '../components/adminForm';
import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import { Hyperlink, ponyImageName, ponyName, staticUrl } from '../services';

const ponies = import.meta.glob('../imgs/ponies/*.svg');

class OverMenu extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      open: false,
      enteringMaintenance: false,
      endDate: null,
      description: null,
      image: '',
    };
  }

  about = () => {
    axios.get('/sys/describe').then(res => {
      this.setState({ description: res.data });
      const path = '../imgs/ponies/' + ponyImageName(res.data.platform.version);
      ponies[path]?.().then(res => {
        const imgURL = new URL(path, import.meta.url);
        this.setState({ image: imgURL.href });
      });
    });
  };

  enterMaintenance = () => {
    this.setState({ enteringMaintenance: true, endDate: moment() });
  };

  submitMaintenance = () => {
    const { T, setPortalAlertStatus } = this.props;
    const { endDate } = this.state;
    this.setState({ enteringMaintenance: false });
    axios
      .put('/api/v2/domains/maintenance', { enabled: true, end: endDate.toISOString() })
      .then(() => {
        setPortalAlertStatus(false, true, T.t('overlord.alert.maintenanceEntered'));
        window.postMessage('maintenance', '*'); // what monsters we've become
      })
      .catch(error => {
        console.log(error);
        setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
  };

  exitMaintenance = () => {
    const { T, setPortalAlertStatus } = this.props;
    axios
      .put('/api/v2/domains/maintenance', { enabled: false })
      .then(() => {
        setPortalAlertStatus(false, true, T.t('overlord.alert.maintenanceExited'));
        window.postMessage('maintenance', '*'); // what monsters we've become
      })
      .catch(error => {
        console.log(error);
        setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
  };

  invalidateCdn = () => {
    const { T, setPortalAlertStatus } = this.props;
    axios
      .post('/sys/cdn/antivenin')
      .then(() => {
        setPortalAlertStatus(false, true, T.t('overlord.alert.cdnRefreshed'));
      })
      .catch(error => {
        console.log(error);
        setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      });
  };

  shutdownCluster = () => {
    window.top.location = '/control/shutdown';
  };

  render() {
    const { T } = this.props;
    const { description, enteringMaintenance, endDate, open, image } = this.state;
    const toggleState = () => this.setState(state => ({ open: !state.open }));
    const hideModal = () => this.setState({ enteringMaintenance: false, description: null });
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
              onClick={this.about}
            >
              {T.t('about.page.name')}
            </DropdownItem>
            <DropdownItem
              id="overlord-enter-maintenance"
              onClick={this.enterMaintenance}
            >
              {T.t('overlord.menu.enterMaintenance')}
            </DropdownItem>
            <DropdownItem
              id="overlord-exit-maintenance"
              onClick={this.exitMaintenance}
            >
              {T.t('overlord.menu.exitMaintenance')}
            </DropdownItem>
            <DropdownItem
              id="overlord-invalidate-cdn"
              onClick={this.invalidateCdn}
            >
              {T.t('overlord.menu.invalidateCdn')}
            </DropdownItem>
            <DropdownItem
              id="overlord-shutdown-cluster"
              onClick={this.shutdownCluster}
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
                value={endDate.toISOString()}
                label={T.t('overlord.maintenance.endTime')}
                onChange={endDate => this.setState({ endDate })}
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
                onClick={this.submitMaintenance}
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
  }
}

OverMenu.propTypes = {
  T: LoPropTypes.translations,
  setPortalAlertStatus: PropTypes.func.isRequired,
};

function mapStateToProps(state) {
  return {
    T: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

export default connect(mapStateToProps, mapDispatchToProps)(OverMenu);
