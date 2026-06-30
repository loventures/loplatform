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
import { Button, Input, Table } from 'reactstrap';

import { AdminFormTitle } from '../components/adminForm';
import WaitDotGif from '../components/WaitDotGif';
import { SrsCollection } from '../srs';

interface RightTreeNode {
  clasz: string;
  name: string;
  description: string;
  children: RightTreeNode[];
}

interface Right {
  clasz: string;
}

interface Role {
  id: number;
  rightIds: string[];
  roleType: { name: string };
}

type RoleMap = Record<number, Record<string, boolean>>;
type RightsLists = Record<number, string[]>;

interface RightsTreeProps {
  rolesUrl: string;
  rightsUrl: string;
  rightTreeUrl: string;
  postUrl: string;
  translations: Polyglot;
  setPortalAlertStatus: (error: boolean, success: boolean, message: string) => void;
  refreshRef?: (refresh: () => void) => void;
  contextId?: number;
}

const RightsTree: React.FC<RightsTreeProps> = ({
  rolesUrl,
  rightsUrl,
  rightTreeUrl,
  postUrl,
  translations: T,
  setPortalAlertStatus,
  refreshRef = () => null,
  contextId,
}) => {
  const [loaded, setLoaded] = useState(false);
  const [roles, setRoles] = useState<Role[]>([]);
  const [rights, setRights] = useState<Right[]>([]);
  const [roleMap, setRoleMap] = useState<RoleMap>({});
  const [rightTree, setRightTree] = useState<RightTreeNode>({
    clasz: '',
    name: '',
    description: '',
    children: [],
  });
  const [rightsLists, setRightsLists] = useState<RightsLists>({});
  const [submitting, setSubmitting] = useState(false);

  const createRoleMap = (roleList: Role[], rightList: Right[]): RoleMap => {
    return roleList.reduce<RoleMap>(
      (obj, role) => ({
        ...obj,
        [role.id]: rightList
          .map(right => ({ [right.clasz]: role.rightIds.indexOf(right.clasz) >= 0 }))
          .reduce((obj1, rightBool) => ({ ...obj1, ...rightBool }), {}),
      }),
      {}
    );
  };

  const computeRightsList = (
    flags: Record<string, boolean>,
    node: RightTreeNode,
    previous: boolean,
    result: string[]
  ): string[] => {
    const flagged = flags[node.clasz];
    if (previous !== flagged) {
      result.push((flagged ? '' : '-') + node.clasz);
    }
    node.children.forEach(n => computeRightsList(flags, n, flagged, result));
    return result;
  };

  const createRightsLists = (rMap: RoleMap, tree: RightTreeNode): RightsLists =>
    Object.keys(rMap).reduce<RightsLists>(
      (o, roleId) => ({
        ...o,
        [roleId]: computeRightsList(rMap[Number(roleId)], tree, false, []),
      }),
      {}
    );

  const refresh = () => {
    Promise.all([
      axios.get<SrsCollection<Role>>(rolesUrl),
      axios.get<SrsCollection<Right>>(rightsUrl),
      axios.get<RightTreeNode>(rightTreeUrl),
    ]).then(([roleRes, rightRes, rightTreeRes]) => {
      const newRoles = roleRes.data.objects;
      const newRights = rightRes.data.objects;
      const newRightTree = rightTreeRes.data;
      const newRoleMap = createRoleMap(newRoles, newRights);
      const newRightsLists = createRightsLists(newRoleMap, newRightTree);
      setLoaded(true);
      setRoles(newRoles);
      setRights(newRights);
      setRightTree(newRightTree);
      setRightsLists(newRightsLists);
      setRoleMap(newRoleMap);
      refreshRef(refresh);
    });
  };

  useEffect(() => {
    refresh();
    return () => {
      setPortalAlertStatus(false, false, '');
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const findNodeByRightId = (rightId: string): RightTreeNode | null => {
    const stack: RightTreeNode[] = [rightTree];
    while (stack.length) {
      const curr = stack.pop()!;
      if (curr.clasz === rightId) {
        return curr;
      }
      curr.children.forEach(child => stack.push(child));
    }
    return null;
  };

  const getAllDescendants = (node: RightTreeNode): RightTreeNode[] => {
    const stack = [node];
    const result: RightTreeNode[] = [];
    while (stack.length) {
      const curr = stack.pop()!;
      result.push(curr);
      curr.children.forEach(o => stack.push(o));
    }
    return result;
  };

  const onRoleRightChange = (checked: boolean, roleId: number, rightId: string) => {
    const node = findNodeByRightId(rightId);
    const update = getAllDescendants(node!).reduce<Record<string, boolean>>(
      (o, el) => ({ ...o, [el.clasz]: checked }),
      {}
    );
    const newRoleMap = { ...roleMap, [roleId]: { ...roleMap[roleId], ...update } };
    setRoleMap(newRoleMap);
  };

  const updateRights = () => {
    const newRightsLists = createRightsLists(roleMap, rightTree);
    const rolesToRights = Object.keys(roleMap).reduce<Record<string, string[]>>((o, roleId) => {
      const oldRights = rightsLists[Number(roleId)];
      const newRights = newRightsLists[Number(roleId)];
      return oldRights.join(',') === newRights.join(',') ? o : { ...o, [roleId]: newRights };
    }, {});
    setSubmitting(true);
    axios
      .post<SrsCollection<Role>>(postUrl, { rolesToRights, contextId })
      .then(res => {
        setPortalAlertStatus(false, true, T.t('adminPage.rights.rightsUpdated'));
        setRoleMap(createRoleMap(res.data.objects, rights));
        setRightsLists(newRightsLists);
      })
      .catch(err => {
        console.log(err);
        setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
      })
      .then(() => {
        setSubmitting(false);
      });
  };

  const sortByName = (a: Role, b: Role) => {
    return a.roleType.name.toLowerCase().localeCompare(b.roleType.name.toLowerCase());
  };

  const renderVerticalHeaders = () => {
    return roles.sort(sortByName).map(role => (
      <th
        key={role.id}
        className="vertical"
        role="columnheader"
      >
        <div className="vertical rights-table-role-name">{role.roleType.name}</div>
      </th>
    ));
  };

  const createTableRows = (stack: Array<{ data: RightTreeNode; depth: number }>) => {
    const rows: Array<{ data: RightTreeNode; depth: number }> = [];
    while (stack.length) {
      const curr = stack.pop()!;
      rows.push(curr);
      curr.data.children.forEach(child => {
        stack.push({ data: child, depth: 1 + curr.depth });
      });
    }
    return rows;
  };

  const renderTableBody = () => {
    const node = {
      data: rightTree,
      depth: 0,
    };
    const tableRows = createTableRows([node]);
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
          {roles.sort(sortByName).map(role => {
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
                    onRoleRightChange(evt.target.checked, role.id, node.data.clasz)
                  }
                  type="checkbox"
                  checked={checked}
                  defaultValue={checked ? 'true' : ''}
                  aria-label={label}
                />
              </td>
            );
          })}
        </tr>
      );
    });
  };

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
                {renderVerticalHeaders()}
              </tr>
            </thead>
            <tbody role="rowgroup">{renderTableBody()}</tbody>
          </Table>
        </div>
        <div className="row py-4">
          <Button
            color="primary"
            className="px-5"
            id="rights-table-submit"
            onClick={updateRights}
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
};

export default RightsTree;
