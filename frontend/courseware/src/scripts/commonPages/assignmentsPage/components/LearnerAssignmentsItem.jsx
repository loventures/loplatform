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

import { withTranslation } from '../../../i18n/translationContext';

import ContentLink from '../../../contentPlayerComponents/parts/ContentLink';
import ContentLockableContainer from '../../../contentPlayerComponents/parts/ContentLockableContainer';

import LearnerAssignmentsItemInfo from './LearnerAssignmentsItemInfo';
import LearnerAssignmentsItemGrade from './LearnerAssignmentsItemGrade';

const LearnerAssignmentsItem = ({
  content,
  viewingAs,
  gradebookColumn,

  isLocked = content.availability && content.availability.isLocked,
}) => (
  <ContentLockableContainer
    content={content}
    viewingAs={viewingAs}
  >
    <ContentLink
      content={content}
      viewingAs={viewingAs}
      style={{ textDecoration: 'none' }}
      disabled={isLocked}
    >
      <div
        className={classnames([
          'card assignment-list-item view-item due-date-effects-container',
          isLocked && 'locked',
        ])}
      >
        <div className="card-body overflow-hidden">
          <div className="flex-row-content align-items-start">
            <LearnerAssignmentsItemInfo
              content={content}
              gradebookColumn={gradebookColumn}
            />
            {!isLocked && (
              <LearnerAssignmentsItemGrade
                content={content}
                gradebookColumn={gradebookColumn}
              />
            )}
          </div>
        </div>
      </div>
    </ContentLink>
  </ContentLockableContainer>
);

export default withTranslation(LearnerAssignmentsItem);
