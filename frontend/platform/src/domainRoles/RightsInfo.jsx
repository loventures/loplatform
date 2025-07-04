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

import PropTypes from 'prop-types';
import React from 'react';
import {
  Button,
  ListGroup,
  ListGroupItem,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from 'reactstrap';

class RightsInfo extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }
  render() {
    const { row, T, close } = this.props;
    const rights = row.rights.split(', ');
    return (
      <Modal
        isOpen={true}
        backdrop="static"
        size="lg"
      >
        <ModalHeader
          toggle={this.toggle}
          tag="h2"
        >
          {T.t('adminPage.roles.rightsModal.rights', row)}
        </ModalHeader>
        <ModalBody>
          {row.rights.length ? (
            <ListGroup>
              {rights.sort().map((right, idx) => (
                <ListGroupItem key={idx}>{right}</ListGroupItem>
              ))}
            </ListGroup>
          ) : (
            <h6>{T.t('adminPage.roles.rightsModal.noRights')}</h6>
          )}
        </ModalBody>
        <ModalFooter>
          <Button
            color="secondary"
            onClick={close}
          >
            {T.t('adminPage.roles.rightsModal.close')}
          </Button>
        </ModalFooter>
      </Modal>
    );
  }
}

RightsInfo.propsTypes = {
  row: PropTypes.object.isRequired,
  T: PropTypes.object.isRequired,
  close: PropTypes.func.isRequired,
};

export default RightsInfo;
