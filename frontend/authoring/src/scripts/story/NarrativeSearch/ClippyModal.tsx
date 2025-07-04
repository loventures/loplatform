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
import { GiAngryEyes } from 'react-icons/gi';
import { TfiClip } from 'react-icons/tfi';
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

export const ClippyModal: React.FC<{
  isOpen: boolean;
  toggle: () => void;
}> = ({ isOpen, toggle }) => (
  <Modal
    id="search-clippy-modal"
    isOpen={isOpen}
    toggle={toggle}
  >
    <ModalHeader>Search Help</ModalHeader>
    <ModalBody className="d-flex align-items-center">
      <div className="position-relative me-4">
        <TfiClip size="3rem" />
        <GiAngryEyes
          size="3rem"
          style={{ position: 'absolute', top: '-1.875em', right: 0 }}
        />
      </div>
      <div>
        <p>It looks like you want a boolean search!</p>
        <div>
          Don&apos;t use AND and OR for this. Instead, please read the instructions for help on
          search syntax.
        </div>
      </div>
    </ModalBody>
    <ModalFooter>
      <Button
        id="search-clippy-ok-button"
        color="primary"
        onClick={toggle}
      >
        Dismiss
      </Button>
    </ModalFooter>
  </Modal>
);
