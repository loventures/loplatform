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

import { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';

import { closeModal } from '../modals/modalActions';
import { useDcmSelector } from './index';

const useModal = <A = any>(): { modalOpen: boolean; toggleModal: () => void; data?: A } => {
  const dispatch = useDispatch();
  const { openModalId, data } = useDcmSelector(state => state.modal);

  const [modalOpen, setModalOpen] = useState(true);

  const toggleModal = () => setModalOpen(false);

  useEffect(() => {
    // bootstrap $modal-transition timing value is .3s
    !modalOpen && setTimeout(() => dispatch(closeModal(openModalId)), 300);
  }, [modalOpen]);

  return { modalOpen, toggleModal, data };
};

export default useModal;
