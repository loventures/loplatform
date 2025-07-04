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
import { Button, Col, Input, InputGroup, Row } from 'reactstrap';
import { bindActionCreators } from 'redux';

import LoPropTypes from '../react/loPropTypes';
import * as MainActions from '../redux/actions/MainActions';
import RightsTree from '../rights/RightsTree';

class App extends React.Component {
  state = {
    addRoleOptions: [],
    rolesLoaded: false,
    role: '',
  };

  refreshTree = () => {
    this.refresh();
    this.initState();
  };

  initState = () => {
    const { translations: T, setPortalAlertStatus, courseId } = this.props;
    const getKnownRoles = axios.get('/api/v2/domain/knownRoles');
    const getSupportedRoles = axios.get(`/api/v2/roles/byContext/${courseId}`);
    Promise.all([getKnownRoles, getSupportedRoles])
      .then(([knownRolesRes, supportedRolesRes]) => {
        const knownRoles = knownRolesRes.data.objects.sort((a, b) => a.name.localeCompare(b.name));
        const supportedRoles = supportedRolesRes.data.objects;
        const supportedIds = supportedRoles.map(role => role.roleType.id);
        const filtered = knownRoles.filter(role => !supportedIds.includes(role.id));
        const guestSupported = supportedRoles.map(role => role.roleType.id).includes(-1);
        const guest = T.t('adminPage.enrollments.rightsTree.roles.guest');
        const baseOptions = !guestSupported
          ? [
              {
                id: '',
                name: guest,
              },
            ]
          : [];
        const addRoleOptions = baseOptions.concat(filtered);
        this.setState({
          rolesLoaded: true,
          role: !guestSupported ? guest : addRoleOptions[0] && addRoleOptions[0].id,
          addRoleOptions: addRoleOptions,
        });
      })
      .catch(err => {
        console.log(err);
        const msg = T.t('error.unexpectedError');
        setPortalAlertStatus(true, false, msg);
      });
  };

  componentDidMount() {
    this.initState();
  }

  roleChange = e => {
    this.setState({ role: e.target.value });
  };

  addRole = () => {
    const { setPortalAlertStatus, translations: T, courseId } = this.props;
    const { role } = this.state;
    if (role) {
      const roleId = parseInt(role, 10);
      const data = {
        roleId: roleId,
      };
      axios
        .post(`/api/v2/roles/byContext/${courseId}`, data)
        .then(() => {
          this.refreshTree();
          const msg = T.t('adminPage.enrollments.rightsTree.roleAddedAlert');
          setPortalAlertStatus(false, true, msg);
        })
        .catch(err => {
          console.log(err);
          const msg = T.t('error.unexpectedError');
          setPortalAlertStatus(true, false, msg);
        });
    }
  };

  render() {
    const { translations: T, courseId } = this.props;
    const { addRoleOptions, rolesLoaded } = this.state;
    if (!rolesLoaded) return null;
    const options = addRoleOptions.map(role => {
      return (
        <option
          id={role.id}
          key={role.id}
          value={role.id}
        >
          {role.name}
        </option>
      );
    });
    return (
      <Row style={{ margin: '0.5em' }}>
        <Col sm={8}>
          <RightsTree
            {...this.props}
            rolesUrl={`/api/v2/roles/byContext/${courseId}`}
            rightTreeUrl="/api/v2/rights/course"
            rightsUrl="/api/v2/rights/course/all"
            postUrl="/api/v2/rights"
            contextId={courseId}
            refreshRef={refresh => (this.refresh = refresh)}
          />
        </Col>
        <Col sm={4}>
          <InputGroup>
            <Input
              type="select"
              onChange={this.roleChange}
            >
              {options}
            </Input>
            <Button
              color="secondary"
              onClick={this.addRole}
              className="ms-2"
            >
              {T.t('adminPage.enrollments.rightsTree.addRoleBtn.label')}
            </Button>
          </InputGroup>
        </Col>
      </Row>
    );
  }
}

App.propTypes = {
  translations: LoPropTypes.translations,
  setPortalAlertStatus: PropTypes.func.isRequired,
  match: PropTypes.object.isRequired,
  location: PropTypes.object.isRequired,
  courseId: PropTypes.string.isRequired,
};

function mapStateToProps(state) {
  return {
    translations: state.main.translations,
  };
}

function mapDispatchToProps(dispatch) {
  return bindActionCreators(MainActions, dispatch);
}

const UserEnrollments = connect(mapStateToProps, mapDispatchToProps)(App);

export default UserEnrollments;
