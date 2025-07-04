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

import { LeftNavListItem } from '../../commonPages/sideNav/LeftNavListItem';
import { useBookmarks } from '../../components/bookmarks/bookmarksReducer';
import { useAllGatingInfoResource } from '../../resources/GatingInformationResource';
import { useFlatLearningPathResource } from '../../resources/LearningPathResource';
import { qnaEnabled } from '../../utilities/preferences';
import { useCourseSelector } from '../../loRedux';
import QnaIcon from '../../qna/QnaIcon';
import {
  BookmarksLink,
  DiscussionListPageLink,
  LearnerAssignmentListPageLink,
  LearnerCompetenciesPageLink,
  QnaPageLink,
} from '../../utils/pageLinks';
import { useTranslation } from '../../i18n/translationContext';
import { isAssignment, isDiscussion } from '../../utilities/contentTypes';
import { selectCurrentUser } from '../../utilities/rootSelectors';
import React from 'react';
import { FiBookmark } from 'react-icons/fi';
import { IoChatbubblesOutline, IoCheckboxOutline, IoShieldCheckmarkOutline } from 'react-icons/io5';

const ERLearnerNavItems: React.FC = () => {
  const translate = useTranslation();
  const user = useCourseSelector(selectCurrentUser);
  const bookmarks = useBookmarks();

  const flatPath = useFlatLearningPathResource(user.id);
  const gatingInfo = useAllGatingInfoResource(user.id);
  const hasAssignments = Boolean(flatPath.find(isAssignment));
  const hasCompetencies = Boolean(flatPath.find(c => c.competencies && c.competencies.length));
  const hasDiscussions = Boolean(
    flatPath.find(c => isDiscussion(c) && !gatingInfo[c.id]?.isLocked)
  );
  return (
    <>
      {hasAssignments ? (
        <LeftNavListItem to={LearnerAssignmentListPageLink}>
          <IoCheckboxOutline
            aria-hidden={true}
            stroke="#2e4954"
            className="me-2"
          />
          {translate('ER_SCORES_TITLE')}
        </LeftNavListItem>
      ) : null}
      {hasDiscussions ? (
        <LeftNavListItem to={DiscussionListPageLink}>
          <IoChatbubblesOutline
            aria-hidden={true}
            className="me-2"
            stroke="#2e4954"
          />
          {translate('ER_DISCUSSIONS_TITLE')}
        </LeftNavListItem>
      ) : null}
      {hasCompetencies ? (
        <LeftNavListItem to={LearnerCompetenciesPageLink}>
          <IoShieldCheckmarkOutline
            aria-hidden={true}
            className="me-2"
            stroke="#2e4954"
          />
          {translate('COURSE_LEARNER_COMPETENCIES')}
        </LeftNavListItem>
      ) : null}
      {!!Object.keys(bookmarks).length && (
        <LeftNavListItem to={BookmarksLink}>
          <FiBookmark
            aria-hidden={true}
            strokeWidth={1.5}
            stroke="#2e4954"
            className="me-2"
          />
          <span>{translate('ER_BOOKMARKS')}</span>
        </LeftNavListItem>
      )}
      {qnaEnabled && (
        <LeftNavListItem to={QnaPageLink}>
          <QnaIcon
            aria-hidden={true}
            className="me-2"
            style={{ width: '1rem', height: '1rem', color: '#2e4954' }}
          />
          <span>{translate('QNA_PAGE_TITLE')}</span>
        </LeftNavListItem>
      )}
    </>
  );
};

export default ERLearnerNavItems;
