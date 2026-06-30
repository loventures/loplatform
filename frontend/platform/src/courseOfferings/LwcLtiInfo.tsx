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
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import LtiLaunchInfo from '../groups/LtiLaunchInfo';
import { LoPlatform } from '../types/loPlatform';

interface CourseOfferingRow {
  id: number;
  groupId: string;
  [key: string]: unknown;
}

interface ContentItem {
  id: number | string;
  name: string;
  depth: number;
  gradable?: boolean;
  [key: string]: unknown;
}

interface LwcLtiInfoProps {
  row: CourseOfferingRow;
  T: Polyglot;
  close: () => void;
  setPortalAlertStatus: (error: boolean, success: boolean, message: string) => void;
  lo_platform: LoPlatform;
}

const LwcLtiInfo: React.FC<LwcLtiInfoProps> = ({
  row,
  T,
  close,
  setPortalAlertStatus,
  lo_platform,
}) => {
  const [loaded, setLoaded] = useState(false);
  const [contentItems, setContentItems] = useState<ContentItem[]>([]);

  const baseUrl = () => `https://${lo_platform.domain.hostName}/`;

  const genericError = (e: unknown) => {
    console.log(e);
    setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
  };

  useEffect(() => {
    axios
      .get(`/api/v2/lwc/courseOfferings/${row.id}/ltiLaunchInfo`)
      .then(ltiRes => {
        setContentItems(ltiRes.data.objects);
        setLoaded(true);
      })
      .catch(genericError);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!loaded) return null;
  const baseName = 'adminPage.courseOfferings.ltiLaunchModal';
  const urls = {
    launchUrl: `${baseUrl()}lwlti/offering/${row.groupId}`,
    xmlUrl: `${baseUrl()}api/lti1/offering/${row.groupId}/lti.xml`,
    ccUrl: `/api/lti1/offering/${row.groupId}/cc.xml`,
    ccUrl2: `/api/lti1/offering/${row.groupId}/cc.xml?modules=true`,
  };
  const dlUrl = `/api/v2/lwc/courseOfferings/${row.id}/links.csv`;
  const withGradeAndUrl = contentItems.map(ci => ({
    ...ci,
    graded: ci.gradable,
    url: `${baseUrl()}lwlti/offering/${row.groupId}/${ci.id}`,
  }));
  return (
    <Modal
      isOpen={true}
      size="lg"
      toggle={close}
      className="crudTable-modal ltiLaunchModal"
    >
      <ModalHeader tag="h2">{T.t(`${baseName}.title`, row)}</ModalHeader>
      <ModalBody>
        <LtiLaunchInfo
          baseName={baseName}
          urls={urls}
          contentItems={withGradeAndUrl}
          T={T}
          dlUrl={dlUrl}
        />
      </ModalBody>
      <ModalFooter>
        <Button
          id="react-table-close-modal-btn"
          onClick={close}
        >
          {T.t('crudTable.modal.closeButton')}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export default LwcLtiInfo;
