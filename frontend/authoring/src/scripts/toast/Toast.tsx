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

import classnames from 'classnames';
import * as React from 'react';
import { useSelector } from 'react-redux';

import { DcmState } from '../types/dcmState';

const Toast = () => {
  const { toastType, open, message } = useSelector((state: DcmState) => state.toast);

  return (
    <div
      aria-atomic
      aria-live="assertive"
      role="alert"
      className={classnames(`toast toast-${toastType}`, { show: open })}
    >
      {message}
    </div>
  );
};

export default Toast;
