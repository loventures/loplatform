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
import { grade as gradeFormat, makeGradeDisplayMethods } from '../../filters/pure/grade.ts';
import { openErrorModal } from '../../modals/errorModal/errorModal.tsx';
import { SrsList } from '../../srs/react/SrsList';
import { SrsStore } from '../../srs/react/useSrsStore';
import { coloredGrade } from '../../utilities/colorGradients.js';
import { find } from 'lodash';
import React from 'react';

// Renders the student's grade with the default (percent) display, which takes no `translate`.
const gradeMethods = makeGradeDisplayMethods();

/**
 * React port of studentPickerModal.html (the grading-panel student picker).
 * Renders the srs list of gradable students (name + colored grade); clicking a
 * row runs the original select flow — `grader.confirmDiscardChanges()` →
 * `loadGradableUsers()` → resolve the matched user's id (the old
 * `$uibModalInstance.close(target.id)`), or open the "no submission" error modal
 * (the old `errorService.generic(...)`, now `openErrorModal`). The header close
 * button dismisses. DOM preserved: `.card-list.list-group`,
 * `ul.card-list-striped-body > li` with `.flex-row-content` + `.full-sized-percent`.
 */

interface GradingPanelStudentPickerProps {
  store: SrsStore;
  grader: any;
}

export const GradingPanelStudentPickerModalBody: React.FC<
  ModalControls<any> & GradingPanelStudentPickerProps
> = ({ close, dismiss, store, grader }) => {
  const select = (student: any) => {
    grader
      .confirmDiscardChanges()
      .then(() => grader.loadGradableUsers())
      .then((users: any[]) => {
        const target = find(users, user => student.id === user.id);
        if (target) {
          close(target.id);
        } else {
          // the old errorService.generic('StudentHasNoSubmission', 'CannotGradeTillSubmit', [], …)
          openErrorModal({
            title: 'StudentHasNoSubmission',
            message: 'CannotGradeTillSubmit',
            actions: [],
            buttons: { hideSecondaryButton: true },
          }).catch(() => {});
        }
      });
  };

  const renderItem = (row: any) => (
    <li onClick={() => select(row)}>
      <div className="flex-row-content">
        <span
          className="flex-col-fluid"
          title={row.fullName}
        >
          {row.fullName}
        </span>
        <span className={`full-sized-percent ${coloredGrade(row.grade)}`}>
          {gradeFormat(gradeMethods, row.grade) as React.ReactNode}
        </span>
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
      getItemKey={row => row.id}
      renderItem={renderItem}
    />
  );
};
