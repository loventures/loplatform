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

/* An alert that scrolls the modal top into view. */
class ModalBar extends React.Component {
  onRef = el => {
    if (el) {
      document.getElementsByClassName('crudTable-modal')[0].parentNode.scrollTop = 0;
      el.classList.add('show');
    }
  };

  render() {
    const { type, value } = this.props;
    const cls = `row fade ${type}-bar`;
    return (
      <div
        id="reactTable-modalForm-errorBar"
        ref={this.onRef}
        className={cls}
      >
        {value}
      </div>
    );
  }
}

ModalBar.propTypes = {
  type: PropTypes.string.isRequired,
  value: PropTypes.string.isRequired,
};

export default ModalBar;
