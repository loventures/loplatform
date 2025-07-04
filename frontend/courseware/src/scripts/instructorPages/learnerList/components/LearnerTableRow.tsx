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
  toggleStudent,
} from '../../../instructorPages/learnerList/learnerListActions';
import GradeBadge from '../../../directives/GradeBadge';
import ProgressBadge from '../../../directives/ProgressBadge';
import { useTranslation } from '../../../i18n/translationContext';
import * as preferences from '../../../utilities/preferences';
import React from 'react';
import { ConnectedProps, connect } from 'react-redux';

import { openModalActionCreator } from '../actions/modalActions';
import { LearnerTableRecord, NameFormat, NameFormatter } from '../learnerListStore';
import LearnerNameWithPreview from './LearnerNameWithPreview';
import { lojector } from '../../../loject';

const formatLastActivity = (time?: string) =>
  time && (lojector.get('formatDayjsFilter') as any)(time, 'M/D/YYYY h:mm A z');

const nameFormatter =
  (nameFormat: NameFormat): NameFormatter =>
  learner =>
    nameFormat === 'LAST_FIRST'
      ? `${learner.familyName ?? ''}, ${learner.givenName ?? ''} ${learner.middleName ?? ''} <${
          learner.emailAddress
        }>`
      : `${learner.givenName ?? ''} ${learner.middleName ?? ''} ${learner.familyName ?? ''} <${
          learner.emailAddress
        }>`;

type LearnerTableRowProps = LearnerTableRecord & LearnerListComponent;

const connector = connect(null, {
  openModal: openModalActionCreator,
});

const LearnerTableRow: React.FC<ConnectedProps<typeof connector> & LearnerTableRowProps> = ({
  learner,
  progress,
  grade,
  state,
  _dispatch,
  openModal,
}) => {
  const translate = useTranslation();
  const allowDirectMessaging = preferences.allowDirectMessaging;
  const isRowSelected = state.selectedStudents.has(learner.id);
  const formatName = nameFormatter(state.filters.nameFormat);
  return (
    <tr className="body-row">
      {allowDirectMessaging && (
        <td className="checkbox-cell">
          <input
            type="checkbox"
            checked={isRowSelected}
            title={translate('LEARNER_TABLE_SELECT_LEARNER')}
            onChange={() => _dispatch(toggleStudent(learner.id))}
          />
        </td>
      )}

      <td className="name-cell">
        <LearnerNameWithPreview
          learner={learner}
          formatName={formatName}
          openModal={allowDirectMessaging ? openModal : undefined}
        />
      </td>

      <td className="last-activity-cell">
        <span>{progress && formatLastActivity(progress.lastModified)}</span>
      </td>

      <td className="progress-cell">
        {progress ? <ProgressBadge progress={progress.progress._root_} /> : '…'}
      </td>

      <td className="grade-cell">
        {grade ? (
          <GradeBadge
            grade={grade}
            display={'percentSign'}
            showEmptyGrade={true}
          />
        ) : (
          '… '
        )}
      </td>
    </tr>
  );
};

export default connector(LearnerTableRow) as React.FC<LearnerTableRowProps>;
