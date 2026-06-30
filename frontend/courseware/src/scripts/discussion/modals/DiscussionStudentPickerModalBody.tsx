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

import type { ModalControls } from '../../directives/modalHost/reactModalHost';
import { SrsList } from '../../srs/react/SrsList';
import { SrsStore } from '../../srs/react/useSrsStore';
import React from 'react';

/**
 * React port of discussionStudentPickerModal.html (the discussion "user posts"
 * jumper student picker). Renders the srs list of students (name + post count);
 * clicking a row resolves that user (the old `close({ $value: user })`), and the
 * header close button dismisses. DOM preserved: `.card-list.list-group`,
 * `ul.card-list-striped-body > li` with `.flex-row-content`.
 */
export const DiscussionStudentPickerModalBody: React.FC<ModalControls<any> & { store: SrsStore }> = ({
  close,
  dismiss,
  store,
}) => {
  const renderItem = (user: any) => (
    <li onClick={() => close(user)}>
      <div className="flex-row-content">
        <span
          className="flex-col-fluid"
          title={user.fullName}
        >
          {user.fullName}
        </span>
        <span>{user.totalPosts}</span>
      </div>
    </li>
  );

  return (
    <SrsList
      store={store}
      className="list-group"
      iconCls="icon-users"
      headerText="COURSES_STUDENTS"
      headerButton={{ label: 'MODAL_CLOSE', onClick: () => dismiss() }}
      getItemKey={user => user.handle}
      renderItem={renderItem}
    />
  );
};
