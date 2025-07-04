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

import { LearnerListComponent } from '../../../instructorPages/learnerList/learnerListActions';
import { useTranslation } from '../../../i18n/translationContext';
import React from 'react';

type LearnerTableLoadingMessagesProps = LearnerListComponent & {
  className: string;
};

const LearnerTableLoadingMessages: React.FC<LearnerTableLoadingMessagesProps> = ({
  state,
  className,
}) => {
  const translate = useTranslation();
  return state.students == null ? (
    <div className={className}>
      <div className="alert alert-info m-0">{translate('Loading')}</div>
    </div>
  ) : state.students.totalCount === 0 ? (
    <div className={className}>
      <div className="alert alert-warning m-0">{translate('SRS_STORE_EMPTY')}</div>
    </div>
  ) : state.students.filterCount === 0 ? (
    <div className={className}>
      <div className="alert alert-warning m-0">{translate('SRS_STORE_FILTERED')}</div>
    </div>
  ) : null;
};

export default LearnerTableLoadingMessages;
