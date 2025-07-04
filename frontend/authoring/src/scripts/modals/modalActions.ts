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

export const OPEN_MODAL = 'OPEN_MODAL';

// The type A should key of the modal Id but shrug
export function openModal<A = any>(modalId: string, data: A = null) {
  return {
    type: OPEN_MODAL,
    modalId,
    data,
  };
}

export const CLOSE_MODAL = 'CLOSE_MODAL';
export function closeModal(modalId?: string) {
  return {
    type: CLOSE_MODAL,
    modalId,
  };
}

export const SET_MODAL_DATA = 'SET_MODAL_DATA';
export function setModalData(data) {
  return {
    type: SET_MODAL_DATA,
    data,
  };
}
