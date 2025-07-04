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

import {
  LearnerListComponent,
  setFilters,
} from '../../../instructorPages/learnerList/learnerListActions';
import { useTranslation } from '../../../i18n/translationContext';
import * as preferences from '../../../utilities/preferences';
import React, { useState } from 'react';
import { ConnectedProps, connect } from 'react-redux';

import {
  learnerTableDropLearnerActionCreator,
  learnerTableGotoMessagingActionCreator,
} from '../actions/bulkActions';

type LearnerTableBulkActionsProps = LearnerListComponent;

const connector = connect(null, {
  doBulkAction: learnerTableGotoMessagingActionCreator,
  doDropLearners: learnerTableDropLearnerActionCreator,
});

const LearnerTableBulkActions: React.FC<
  ConnectedProps<typeof connector> & LearnerTableBulkActionsProps
> = ({ state, _dispatch, doBulkAction, doDropLearners }) => {
  const [selected, setSelected] = useState('');
  const translate = useTranslation();

  const selectedLearners =
    state.students?.filter(student => state.selectedStudents.has(student.id)) ?? [];
  const noSelectionCheck = selected !== 'entireClass' && selectedLearners.length === 0;
  const disabled = !selected || noSelectionCheck;

  const bulkHandler = () => {
    if (selected === 'dropLearners') {
      if (
        window.confirm(
          translate(
            'STUDENT_LIST_CONFIRM_BULK_DROP',
            { count: selectedLearners.length },
            'messageformat'
          )
        )
      ) {
        doDropLearners(selectedLearners, () => _dispatch(setFilters({})));
      }
    } else {
      doBulkAction(selected === 'entireClass', selectedLearners);
    }
  };

  return (
    <div className="bulk-actions flex-row-content">
      <div className="lo-select-wrap">
        <select
          className="form-control"
          value={selected}
          onChange={e => setSelected(e.target.value)}
        >
          <option value="">{translate('STUDENT_LIST_BULK_ACTION_PLACEHOLDER')}</option>
          {preferences.allowDirectMessaging && (
            <>
              <option value="entireClass">{translate('STUDENT_LIST_BULK_EMAIL_ALL')}</option>
              <option value="notEntireClass">
                {translate('STUDENT_LIST_BULK_EMAIL_SELECTED')}
              </option>
            </>
          )}
          {preferences.instructorRoster && (
            <option value="dropLearners">{translate('STUDENT_LIST_DROP_SELECTED_LEARNERS')}</option>
          )}
        </select>
      </div>

      <button
        className="btn btn-small btn-primary"
        disabled={disabled}
        onClick={bulkHandler}
      >
        {translate('STUDENT_LIST_BULK_ACTION_BUTTON')}
      </button>
    </div>
  );
};

export default connector(LearnerTableBulkActions) as React.FC<LearnerTableBulkActionsProps>;
