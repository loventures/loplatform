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
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

class DomainLinkHelp extends React.Component {
  render() {
    const { close, section, T } = this.props;
    return (
      <Modal
        isOpen={true}
        backdrop="static"
        size="lg"
      >
        <ModalHeader tag="h2">{T.t(`adminPage.${section}.helpModal.header`)}</ModalHeader>
        <ModalBody>
          <p>{T.t(`adminPage.${section}.helpModal.prompt`)}</p>
          <ul>
            <li>{T.t(`adminPage.${section}.helpModal.params.sectionId`)}</li>
            <li>{T.t(`adminPage.${section}.helpModal.params.userId`)}</li>
          </ul>
        </ModalBody>
        <ModalFooter>
          <Button
            color="secondary"
            onClick={close}
          >
            {T.t(`adminPage.${section}.helpModal.close`)}
          </Button>
        </ModalFooter>
      </Modal>
    );
  }
}

DomainLinkHelp.propsTypes = {
  close: PropTypes.func.isRequired,
  section: PropTypes.string.isRequired,
  T: PropTypes.object.isRequired,
};

export default DomainLinkHelp;
