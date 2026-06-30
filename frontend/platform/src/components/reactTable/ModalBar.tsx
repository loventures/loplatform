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

import React from 'react';

interface ModalBarProps {
  type: string;
  value: string;
}

/* An alert that scrolls the modal top into view. */
const ModalBar: React.FC<ModalBarProps> = ({ type, value }) => {
  const onRef = (el: HTMLDivElement | null) => {
    if (el) {
      const parent = document.getElementsByClassName('crudTable-modal')[0]
        ?.parentNode as HTMLElement | null;
      if (parent) parent.scrollTop = 0;
      el.classList.add('show');
    }
  };

  const cls = `row fade ${type}-bar`;
  return (
    <div
      id="reactTable-modalForm-errorBar"
      ref={onRef}
      className={cls}
    >
      {value}
    </div>
  );
};

export default ModalBar;
