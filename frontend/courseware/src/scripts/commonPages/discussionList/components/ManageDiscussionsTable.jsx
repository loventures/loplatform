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

import { map } from 'lodash';
import { withTranslation } from '../../../i18n/translationContext';
import withFocusOnMount from '../../../landmarks/chat/WithFocusOnMount';

const ManageDiscussionsTable = ({
  translate,
  discussions,
  closedStatus,
  toggleClosed,
  disabled,
  onRef,
}) => (
  <table className="manage-discussions-table table card-table table-striped">
    <thead>
      <tr>
        <th className="name-column">{translate('MANAGE_DISCUSSIONS_NAME_COLUMN')}</th>
        <th className="option-column">{translate('MANAGE_DISCUSSIONS_CLOSE_COLUMN')}</th>
      </tr>
    </thead>
    <tbody>
      {map(discussions, (discussion, index) => {
        const value =
          discussion.id in closedStatus
            ? closedStatus[discussion.id]
            : discussion.activity.discussion.closed;
        return (
          <tr key={discussion.id}>
            <td
              className="name-column"
              id={discussion.name}
            >
              {discussion.name}
            </td>
            <td className="option-column">
              <input
                ref={index === 0 ? onRef : null}
                aria-labelledby={discussion.name}
                type="checkbox"
                disabled={disabled}
                checked={value}
                onChange={() => toggleClosed(discussion)}
              />
            </td>
          </tr>
        );
      })}
    </tbody>
  </table>
);

export default withFocusOnMount(withTranslation(ManageDiscussionsTable));
