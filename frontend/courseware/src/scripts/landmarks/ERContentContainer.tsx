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
import Announcements from '../components/announcements/Announcements';
import StickyContainer from '../components/stickies/StickyContainer';
import LoadingEllipsis from '../landmarks/chat/LoadingEllipsis';
import { setPresence } from '../landmarks/PageContainer';
import { ScrollToTopContext } from '../landmarks/ScrollToTopProvider';
import { useContentsResource } from '../resources/ContentsResource';
import { useContentGatingInfoResource } from '../resources/GatingInformationResource';
import { useCourseSelector } from '../loRedux';
import { isEmpty } from 'lodash';
import {
  isCourseWithRelationships,
  selectPageContent,
} from '../courseContentModule/selectors/contentEntrySelectors';
import PreviewAsLearnerLoader from '../loaders/PreviewAsLearnerLoader';
import { presenceChatFeature } from '../utilities/preferences';
import { selectCurrentUser } from '../utilities/rootSelectors';
import React, { Suspense, useContext, useEffect, useMemo } from 'react';
import { Helmet } from 'react-helmet';
import { useIdleTimer } from 'react-idle-timer';

import LearnerPreviewHeader from './learnerPreviewHeader/LearnerPreviewHeader';
import { OnlineUsersPanel } from './presentUsers/presentUsers';
import ReviewPeriodBanner from './ReviewPeriodBanner';
import { lojector } from '../loject';

export type ERContentContainerProps = {
  className?: string;
  title: string;
} & React.PropsWithChildren;

/**
 * Wrapper for course-wide functions such as chat, and learner preview
 * Very similar to PageContainer but only for the main-grid area of ER
 */
const ERContentContainer: React.FC<ERContentContainerProps> = ({ children, className, title }) => {
  const currentUser = useCourseSelector(selectCurrentUser);
  // use a resource simply as a way to ensure things are loaded.
  useContentsResource(currentUser.id);

  const presenceDependency = usePresenceDependency(currentUser.id);
  useEffect(() => {
    isEmpty(presenceDependency)
      ? setPresence()
      : setPresence(presenceDependency[0], presenceDependency[1]);
  }, [presenceDependency.join()]);

  const IdleService: any = lojector.get('IdleService');

  useIdleTimer({
    timeout: 1000, // 1 sec
    onIdle: () => IdleService.onIdleStart(),
    onActive: () => IdleService.onIdleEnd(),
    debounce: 500,
  });

  const content = useCourseSelector(selectPageContent);
  const scrollToTop = useContext(ScrollToTopContext);
  useEffect(() => scrollToTop(), [content.id]);

  return (
    <PreviewAsLearnerLoader>
      <Helmet title={title} />
      <main className={classnames('er-content-main content-width', className)}>
        <StickyContainer>
          <LearnerPreviewHeader />
        </StickyContainer>
        <StickyContainer>
          <ReviewPeriodBanner />
        </StickyContainer>
        <Announcements />
        <div className="er-content-area">
          <Suspense fallback={<LoadingEllipsis />}>{children}</Suspense>
        </div>
      </main>
      {presenceChatFeature && /* why here and not in the header? */ <OnlineUsersPanel />}
    </PreviewAsLearnerLoader>
  );
};

export default ERContentContainer;

const usePresenceDependency = (userId: number) => {
  // get the current content. We need a resource for this.
  const content = useCourseSelector(selectPageContent);
  const contentId = typeof content.id === 'string' ? content.id : '_root_';
  const { isLocked } = useContentGatingInfoResource(contentId, userId);
  const dep = useMemo(() => {
    if (isCourseWithRelationships(content) || isLocked) {
      return [];
    } else {
      return [content.id, content.assetId];
    }
  }, [content.id, isLocked]);
  return dep;
};
