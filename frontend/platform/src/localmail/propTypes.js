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

const addressType = PropTypes.shape({
  address: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
});

export const messageType = PropTypes.shape({
  id: PropTypes.number.isRequired,
  from: addressType.isRequired,
  to: addressType.isRequired,
  subject: PropTypes.string.isRequired,
  body: PropTypes.string.isRequired,
  attachments: PropTypes.array,
});
