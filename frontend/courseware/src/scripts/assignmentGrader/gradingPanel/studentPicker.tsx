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

import { openReactModal } from '../../directives/modalHost/reactModalHost.tsx';
import { useTranslation } from '../../i18n/translationContext';
import { GradingPanelStudentPickerModalBody } from './GradingPanelStudentPickerModalBody.tsx';
import { StudentPickerStore } from './StudentPickerStore.ts';

interface GradingPanelStudentPickerProps {
  grader: any;
}

/**
 * React port of the `gradingPanelStudentPicker` component (gradingPanel cluster): the "change student"
 * button in the grader header — shows the active user and opens the (already-React)
 * `GradingPanelStudentPickerModalBody` to pick a different student, then switches to them. Native React
 * opening the modal through the B0 modal host directly; the React `GradingPanel` renders it directly (the
 * old react2angular bridge is gone). The grader + `StudentPickerStore` stay Angular (lojector); the empty
 * module below is kept only to register `StudentPickerStore`.
 *
 * The button text follows `grader.activeUser` (mutated in place as you navigate); the GraderProvider
 * poll re-renders when the active user changes. DOM preserved
 * for Selenide: `button.grading-panel-user-select` with the user's `.name`.
 */
export const GradingPanelStudentPicker: React.FC<GradingPanelStudentPickerProps> = ({ grader }) => {
  const translate = useTranslation();

  const showStudentListModal = () => {
    const store = new StudentPickerStore(grader) as any;

    openReactModal<any>(controls => (
      <GradingPanelStudentPickerModalBody
        {...controls}
        store={store}
        grader={grader}
      />
    ), { size: "lg" })
      .then((studentId: any) => grader.changeUser(studentId))
      .catch(() => {});
  };

  if (!grader.activeUser) return null;

  return (
    <button
      className="grading-panel-user-select"
      type="button"
      title={translate('GRADING_PANEL_SELECT_USER', { name: grader.activeUser.fullName })}
      onClick={showStudentListModal}
    >
      <span className="name">{grader.activeUser.fullName}</span>
      <i className="icon icon-triangle-down" />
    </button>
  );
};

