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

import GradeBadge from '../../directives/GradeBadge.jsx';
import DueDateBadge from './DueDateBadge.js';
import { CONTENT_TYPE_DIAGNOSTIC } from '../../utilities/contentTypes.js';
import { isNumber, get } from 'lodash';
import { lojector } from '../../loject.js';

const ContentDueDateAndGrade = ({
  content,
  isLocked = content.availability.isLocked,
  isFullyCompleted = content.progress && content.progress.isFullyCompleted,
  isGradable = content.hasGradebookEntry &&
    (content.typeId !== CONTENT_TYPE_DIAGNOSTIC ||
      lojector.get('Settings').isFeatureEnabled('PretestShowGrade')),
}) => (
  <div className="content-due-date-and-grade flex-row-content">
    {!isLocked && content.dueDate && !isFullyCompleted && (
      <DueDateBadge
        date={content.dueDate}
        completed={isFullyCompleted}
        exempt={content.dueDateExempt}
      />
    )}

    {(content.grade || (!isLocked && isGradable && isFullyCompleted)) && (
      <GradeBadge
        grade={content.grade}
        percent="full"
        isPending={!isNumber(get(content, 'grade.pointsAwarded'))}
      />
    )}
  </div>
);

export default ContentDueDateAndGrade;
