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
import { Button, Input, Table } from 'reactstrap';

import { AdminFormTitle } from '../components/adminForm';
import WaitDotGif from '../components/WaitDotGif';
import LoPropTypes from '../react/loPropTypes';

class RightsTree extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loaded: false,
      roles: [],
      rights: [],
      roleMap: {},
      rightTree: { children: [] },
      rightsLists: {},
    };
  }

  refresh = () => {
    const { rolesUrl, rightsUrl, rightTreeUrl, refreshRef } = this.props;
    axios.all([axios.get(rolesUrl), axios.get(rightsUrl), axios.get(rightTreeUrl)]).then(
      axios.spread((roleRes, rightRes, rightTreeRes) => {
        const roles = roleRes.data.objects;
        const rights = rightRes.data.objects;
        const rightTree = rightTreeRes.data;
        const roleMap = this.createRoleMap(roles, rights);
        const rightsLists = this.createRightsLists(roleMap, rightTree);
        this.setState({
          loaded: true,
          roles,
          rights,
          rightTree,
          rightsLists,
          roleMap,
        });
        refreshRef(this.refresh);
      })
    );
  };

  componentDidMount() {
    this.refresh();
  }

  componentWillUnmount() {
    this.props.setPortalAlertStatus(false, false, '');
  }

  findNodeByRightId = rightId => {
    const stack = [this.state.rightTree];
    while (stack.length) {
      const curr = stack.pop();
      if (curr.clasz === rightId) {
        return curr;
      }
      curr.children.forEach(child => stack.push(child));
    }
    return null;
  };

  getAllDescendants = node => {
    const stack = [node];
    const result = [];
    while (stack.length) {
      const curr = stack.pop();
      result.push(curr);
      curr.children.forEach(o => stack.push(o));
    }
    return result;
  };

  onRoleRightChange = (checked, roleId, rightId) => {
    const { roleMap } = this.state;
    const node = this.findNodeByRightId(rightId);
    const update = this.getAllDescendants(node).reduce(
      (o, el) => ({ ...o, [el.clasz]: checked }),
      {}
    );
    const newRoleMap = { ...roleMap, [roleId]: { ...roleMap[roleId], ...update } };
    this.setState({ roleMap: newRoleMap });
  };

  createRoleMap = (roles, rights) => {
    return roles.reduce(
      (obj, role) => ({
        ...obj,
        [role.id]: rights
          .map(right => ({ [right.clasz]: role.rightIds.indexOf(right.clasz) >= 0 }))
          .reduce((obj1, rightBool) => ({ ...obj1, ...rightBool }), {}),
      }),
      {}
    );
  };

  computeRightsList = (flags, node, previous, result) => {
    const flagged = flags[node.clasz];
    if (previous !== flagged) {
      result.push((flagged ? '' : '-') + node.clasz);
    }
    node.children.forEach(n => this.computeRightsList(flags, n, flagged, result));
    return result;
  };

  createRightsLists = (roleMap, rightTree) =>
    Object.keys(roleMap).reduce(
      (o, roleId) => ({
        ...o,
        [roleId]: this.computeRightsList(roleMap[roleId], rightTree, false, []),
      }),
      {}
    );

  updateRights = () => {
    const { translations: T, postUrl, contextId } = this.props;
    const { rightTree, rightsLists, roleMap } = this.state;
    const newRightsLists = this.createRightsLists(roleMap, rightTree);
    const rolesToRights = Object.keys(roleMap).reduce((o, roleId) => {
      const oldRights = rightsLists[roleId];
      const newRights = newRightsLists[roleId];
      return oldRights.join(',') === newRights.join(',') ? o : { ...o, [roleId]: newRights };
    }, {});
    this.setState({ submitting: true });
    axios
      .post(postUrl, { rolesToRights, contextId: contextId })
      .then(res => {
        this.props.setPortalAlertStatus(false, true, T.t('adminPage.rights.rightsUpdated'));
        this.setState({
          roleMap: this.createRoleMap(res.data.objects, this.state.rights),
          rightsLists: newRightsLists,
        });
      })
      .catch(err => {
        console.log(err);
        this.props.setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      })
      .then(() => {
        this.setState({ submitting: false });
      });
  };

  sortByName = (a, b) => {
    return a.roleType.name.toLowerCase().localeCompare(b.roleType.name.toLowerCase());
  };

  renderVerticalHeaders = () => {
    const { roles } = this.state;
    return roles.sort(this.sortByName).map(role => (
      <th
        key={role.id}
        className="vertical"
        role="columnheader"
      >
        <div className="vertical rights-table-role-name">{role.roleType.name}</div>
      </th>
    ));
  };

  createTableRows = stack => {
    const rows = [];
    while (stack.length) {
      const curr = stack.pop();
      rows.push(curr);
      curr.data.children.forEach(child => {
        stack.push({ data: child, depth: 1 + curr.depth });
      });
    }
    return rows;
  };

  renderTableBody = () => {
    const { roles, roleMap, rightTree } = this.state;
    const T = this.props.translations;
    const node = {
      data: rightTree,
      depth: 0,
    };
    const tableRows = this.createTableRows([node]);
    return tableRows.map(node => {
      return (
        <tr
          key={node.data.clasz}
          role="row"
        >
          <th
            style={{ paddingLeft: `${node.depth}.5em`, color: 'black' }}
            scope="row"
            role="rowheader"
          >
            <div className="rights-name">{node.data.name}</div>
            <div className="rights-description"> {node.data.description} </div>
          </th>
          {roles.sort(this.sortByName).map(role => {
            const checked = roleMap[role.id][node.data.clasz];
            const label = T.t('adminPage.rights.label.roleRight', {
              role: role.roleType.name,
              right: node.data.name,
            });
            return (
              <td
                className="rights-check"
                key={role.id}
                role="gridcell"
              >
                <Input
                  className="rights-checkbox"
                  onChange={evt =>
                    this.onRoleRightChange(evt.target.checked, role.id, node.data.clasz)
                  }
                  type="checkbox"
                  checked={checked}
                  defaultValue={checked}
                  aria-label={label}
                />
              </td>
            );
          })}
        </tr>
      );
    });
  };

  render() {
    const { loaded, submitting } = this.state;
    const T = this.props.translations;
    return (
      loaded && (
        <div
          id="rights-main"
          className="container"
        >
          <AdminFormTitle title={T.t('adminPage.rights.title.rightsByRole')} />

          <div className="row mt-3">
            <p id="rights-desc">{T.t('adminPage.rights.description')}</p>
            <Table
              bordered
              id="rights-table"
              className="my-0"
              role="grid"
              aria-label={T.t('adminPage.rights.title.rightsByRole')}
              aria-describedby="rights-desc"
            >
              <thead role="rowgroup">
                <tr role="row">
                  <th id="rights-table-role-header">{''}</th>
                  {this.renderVerticalHeaders()}
                </tr>
              </thead>
              <tbody role="rowgroup">{this.renderTableBody()}</tbody>
            </Table>
          </div>
          <div className="row py-4">
            <Button
              color="primary"
              className="px-5"
              id="rights-table-submit"
              onClick={this.updateRights}
              disabled={submitting}
            >
              {T.t('adminPage.rights.saveButton')}
              {submitting && (
                <WaitDotGif
                  className="ms-2 waiting"
                  color="light"
                  size={16}
                />
              )}
            </Button>
          </div>
        </div>
      )
    );
  }
}

RightsTree.propTypes = {
  rolesUrl: PropTypes.string.isRequired,
  rightsUrl: PropTypes.string.isRequired,
  rightTreeUrl: PropTypes.string.isRequired,
  postUrl: PropTypes.string.isRequired,
  translations: LoPropTypes.translations,
  setPortalAlertStatus: PropTypes.func.isRequired,
  refreshRef: PropTypes.func,
};

RightsTree.defaultProps = {
  refreshRef: () => null,
};

export default RightsTree;
