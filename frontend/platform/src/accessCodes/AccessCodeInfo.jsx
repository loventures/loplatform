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

import React from 'react';
import PropTypes from 'prop-types';
import {
  Button,
  Modal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  ListGroup,
  ListGroupItem,
  ListGroupItemText,
} from 'reactstrap';
import moment from 'moment';

class AccessCodeInfo extends React.Component {
  render() {
    const { accessCodeInfo, T, close } = this.props;
    const fmt = T.t('format.dateTime.full');
    return (
      <Modal
        id="accessCodes-details-modal"
        isOpen={true}
        size="lg"
        toggle={close}
      >
        <ModalHeader
          id="accessCodes-details-modal-header"
          tag="h2"
        >
          {accessCodeInfo.accessCode}
        </ModalHeader>
        <ModalBody>
          <ListGroup>
            <ListGroupItem id="accessCodes-details-modal-batchId">
              <strong>{T.t('adminPage.accessCodes.accessCodeInfo.batchName')}:</strong>{' '}
              {accessCodeInfo.batch.name}
            </ListGroupItem>
            <ListGroupItem id="accessCodes-details-modal-batchDetails">
              <strong>{T.t('adminPage.accessCodes.accessCodeInfo.batchDetails')}:</strong>{' '}
              {accessCodeInfo.batch.description}
            </ListGroupItem>
            <ListGroupItem id="accessCodes-details-modal-redemptions">
              <ListGroupItemText className="mb-0">
                <strong>{T.t('adminPage.accessCodes.accessCodeInfo.redemptions')}:</strong>
              </ListGroupItemText>
              {accessCodeInfo.redemptions.map(red => (
                <ListGroupItemText
                  key={red.id}
                  className="mb-0"
                >
                  {`${red.user.fullName} <${red.user.emailAddress}> on ${moment(red.date).format(fmt)}`}
                </ListGroupItemText>
              ))}
            </ListGroupItem>
          </ListGroup>
        </ModalBody>
        <ModalFooter>
          <Button
            id="accessCodes-details-modal-close"
            color="secondary"
            onClick={close}
          >
            {T.t('adminPage.accessCodes.accessCodeInfo.close')}
          </Button>
        </ModalFooter>
      </Modal>
    );
  }
}

AccessCodeInfo.propTypes = {
  accessCodeInfo: PropTypes.object.isRequired,
  T: PropTypes.object.isRequired,
  close: PropTypes.func.isRequired,
};

export default AccessCodeInfo;
