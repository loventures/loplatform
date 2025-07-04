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

import { values, flatMap } from 'lodash';
import { createMessagesForContent } from './policyMessages.js';
import { lojector } from '../../loject.js';

export const getProgressLabel = (translate, content) => {
  return content.progress && content.progress.completions
    ? translate('ASSET_PROGRESS', content.progress)
    : translate('ASSET_PROGRESS_NOT_STARTED');
};

export const getGradeLabel = (translate, content) => {
  const gradeFilter = lojector.get('gradeFilter');
  return content.grade
    ? translate('CONTENT_GRADE_OVERALL', { grade: gradeFilter(content.grade) })
    : translate('CONTENT_GRADE_NO_SCORE');
};

export const getDueDateLabel = (translate, content) => {
  if (!content.dueDate) {
    return '';
  }
  return translate('DUE_DATE_TIME', { time: content.dueDate });
};

export const getLockInfoLabel = (translate, content, viewingAs) => {
  const isLocked = content.availability && content.availability.isLocked;
  const isInstructor = viewingAs && viewingAs.isInstructor;

  //Content is unlocked to instructors, but they still need access to lock information
  if (isInstructor || isLocked) {
    //For students, tell them it is locked. For instructors, just list the requirements.
    const messagePrefix = isInstructor ? '' : `${translate('CONTENT_IS_LOCKED_TITLE')} `;
    const allMessages = viewingAs && createMessagesForContent(translate, content, viewingAs);
    return messagePrefix + flatMap(allMessages, gateMessages => values(gateMessages)).join('\n');
  } else {
    return '';
  }
};

//Yeah the weird indentation here is intentional
export const summarizeToLabel = (translate, content, viewingAs) => {
  if (content.activity.discussionSummary) {
    const summary = content.activity.discussionSummary;
    return `
${content.name}
${content.description || ''}
${translate(content.typeId)}
${getLockInfoLabel(translate, content, viewingAs)}
${getProgressLabel(translate, content, viewingAs)}
${
  summary.newPostCount > 0 && viewingAs.isStudent
    ? `${summary.newPostCount} ${translate('DISCUSSION_PREVIEW_POST_COUNT_NEW')}`
    : ''
}
${
  summary.unreadPostCount > 0 && viewingAs.isInstructor
    ? `${summary.unreadPostCount} ${translate('DISCUSSION_PREVIEW_POST_COUNT_UNREAD')}`
    : ''
}
${
  summary.unrespondedThreadCount > 0 && viewingAs.isInstructor
    ? `${summary.unrespondedThreadCount} ${translate('DISCUSSION_PREVIEW_POST_COUNT_UNRESPONDED')}`
    : ''
}
${content.activity.discussionSummary.postCount} ${translate('DISCUSSION_PREVIEW_POST_COUNT')}
${content.activity.discussionSummary.participantCount} ${translate(
      'DISCUSSION_PREVIEW_PARTICIPANT_COUNT'
    )}
        `;
  }
  return `
${content.name}
${content.description || ''}
${translate(content.typeId)}
${getLockInfoLabel(translate, content, viewingAs)}
${getGradeLabel(translate, content, viewingAs)}
${getProgressLabel(translate, content, viewingAs)}
${getDueDateLabel(translate, content, viewingAs)}
    `;
};
