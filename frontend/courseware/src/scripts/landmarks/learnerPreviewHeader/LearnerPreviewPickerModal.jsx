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

import { StudentPreviewPicker } from './directives/studentPreviewPicker';
import { connect } from 'react-redux';

import { Modal } from 'reactstrap';

import { toggleLearnerPreviewPickerActionCreator, pickPreviewUserActionCreator } from './actions';

import { selectLearnerPreviewPickerModalState } from './selectors';

const LearnerPreviewPickerModal = ({ status, toggleModal, pickUser }) => (
  <Modal
    isOpen={status}
    toggle={() => toggleModal()}
  >
    {status && (
      <StudentPreviewPicker
        close={user => {
          toggleModal();
          pickUser(user);
        }}
        dismiss={toggleModal}
      />
    )}
  </Modal>
);

export default connect(selectLearnerPreviewPickerModalState, {
  pickUser: pickPreviewUserActionCreator,
  toggleModal: toggleLearnerPreviewPickerActionCreator,
})(LearnerPreviewPickerModal);
