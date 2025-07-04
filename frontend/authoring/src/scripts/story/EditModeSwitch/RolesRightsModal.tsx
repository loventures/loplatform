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

import React, { useEffect, useState } from 'react';
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import { useEditedAsset } from '../../graphEdit';
import { useDcmSelector } from '../../hooks';
import { useProjectGraph } from '../../structurePanel/projectGraphActions';
import { useProjectAccess } from '../hooks';
import { RoleInfo } from './RoleInfo';

export const RolesRightsModal: React.FC<{ open: boolean; toggle: () => void }> = ({
  open,
  toggle,
}) => {
  return (
    <Modal
      size="xl"
      toggle={toggle}
      isOpen={open}
      className="roles-rights-modal"
    >
      <ModalHeader>Rôles and Access</ModalHeader>
      {open && <RolesRightsForm toggle={toggle} />}
      <ModalFooter>
        <Button
          color="secondary"
          onClick={toggle}
        >
          Close
        </Button>
      </ModalFooter>
    </Modal>
  );
};

const RolesRightsForm: React.FC<{ toggle: () => void }> = ({ toggle }) => {
  const { role } = useDcmSelector(state => state.layout);
  const { rights } = useDcmSelector(state => state.user);
  const admin = rights?.includes('loi.authoring.security.right$EditContentAnyProjectRight');
  const { authoringRoles } = useDcmSelector(state => state.configuration);
  const { ContentStatus } = useProjectAccess();
  const [tab, setTab] = useState<string | null>(null);
  useEffect(() => setTab(null), [open]);
  return (
    <ModalBody
      style={{
        overflow: 'auto',
        maxHeight: 'calc(100vh - 12rem)',
      }}
    >
      <CurrentStatus role={role} />
      {Object.entries(authoringRoles)
        .filter(([value]) => admin || ContentStatus || value === role)
        .map(([value, label]) => (
          <RoleInfo
            key={value}
            role={value}
            label={label}
            tab={admin || ContentStatus ? tab : role}
            setTab={setTab}
            toggle={toggle}
            admin={admin}
          />
        ))}
    </ModalBody>
  );
};

const CurrentStatus: React.FC<{ role?: string }> = ({ role }) => {
  const configuration = useDcmSelector(state => state.configuration);
  const { homeNodeName, rootNodeName } = useProjectGraph();
  const projectStatus = useEditedAsset(rootNodeName).data.projectStatus;
  const contentStatus = useEditedAsset(homeNodeName).data.contentStatus;
  return (
    <p>
      Project Status:{' '}
      <em>{projectStatus ? (configuration.projectStatuses[projectStatus] ?? '???') : 'Unset'}</em>,
      Course Status:{' '}
      <em>{contentStatus ? (configuration.contentStatuses[contentStatus] ?? '???') : 'Unset'}</em>,
      Your Role: <em>{role ? (configuration.authoringRoles[role] ?? '???') : 'None'}</em>.
    </p>
  );
};
