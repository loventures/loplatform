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
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import LtiLaunchInfo from '../groups/LtiLaunchInfo';

class LwcLtiInfo extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loaded: false,
      contentItems: [],
    };
  }

  baseUrl = () => {
    const { lo_platform } = this.props;
    return `https://${lo_platform.domain.hostName}/`;
  };

  genericError = e => {
    const { setPortalAlertStatus, T } = this.props;
    console.log(e);
    setPortalAlertStatus(true, false, T.t('error.unexpectedError'));
  };

  componentDidMount() {
    const { row } = this.props;
    axios
      .get(`/api/v2/lwc/courseOfferings/${row.id}/ltiLaunchInfo`)
      .then(ltiRes =>
        this.setState({
          contentItems: ltiRes.data.objects,
          loaded: true,
        })
      )
      .catch(this.genericError);
  }

  render() {
    const { contentItems, loaded } = this.state;
    const { row, T, close } = this.props;
    if (!loaded) return null;
    const baseName = 'adminPage.courseOfferings.ltiLaunchModal';
    const urls = {
      launchUrl: `${this.baseUrl()}lwlti/offering/${row.groupId}`,
      xmlUrl: `${this.baseUrl()}api/lti1/offering/${row.groupId}/lti.xml`,
      ccUrl: `/api/lti1/offering/${row.groupId}/cc.xml`,
      ccUrl2: `/api/lti1/offering/${row.groupId}/cc.xml?modules=true`,
    };
    const dlUrl = `/api/v2/lwc/courseOfferings/${row.id}/links.csv`;
    const withGradeAndUrl = contentItems.map(ci => ({
      ...ci,
      graded: ci.gradable,
      url: `${this.baseUrl()}lwlti/offering/${row.groupId}/${ci.id}`,
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
  }
}

LwcLtiInfo.propTypes = {
  row: PropTypes.object.isRequired,
  T: PropTypes.object.isRequired,
  close: PropTypes.func.isRequired,
  setPortalAlertStatus: PropTypes.func.isRequired,
  lo_platform: PropTypes.object.isRequired,
};

export default LwcLtiInfo;
