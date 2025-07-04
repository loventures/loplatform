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

import Course from '../bootstrap/course';
import StickyContainer from '../components/stickies/StickyContainer';
import { isEmpty } from 'lodash';
import PageContentLoader from '../loaders/PageContentLoader';
import PreviewAsLearnerLoader from '../loaders/PreviewAsLearnerLoader';
import { presenceChatFeature } from '../utilities/preferences';
import React, { useEffect } from 'react';
import { useIdleTimer } from 'react-idle-timer';

import { NgPresenceService } from '../presence/PresenceService';
import LearnerPreviewHeader from './learnerPreviewHeader/LearnerPreviewHeader';
import MainContent from './MainContent';
import PageHeader from './pageHeader/PageHeader';
import { OnlineUsersPanel } from './presentUsers/presentUsers';
import ReviewPeriodBanner from './ReviewPeriodBanner';
import { lojector } from '../loject';

export function setPresence(edgePath?: any, assetId?: any) {
  const PS: NgPresenceService = lojector.get('PresenceService');
  const context: number = Course.id;
  if (typeof edgePath === 'number' || edgePath === undefined) {
    PS.setScenes([{ context }]);
  } else {
    PS.setScenes([{ context }, { context, edgePath, assetId }]);
  }
}

export type PageContainerProps = {
  renderBreadcrumbs?: () => JSX.Element;
  renderNavPanel?: () => JSX.Element;
  renderBanner?: () => JSX.Element;
  wide?: boolean;
  presenceDependency?: Array<number | string>;
} & React.PropsWithChildren;

const PageContainer: React.FC<PageContainerProps> = ({
  renderBreadcrumbs = () => null,
  renderNavPanel = () => null,
  renderBanner = () => null,
  children,
  wide = false,
  presenceDependency = [],
}) => {
  useEffect(() => {
    isEmpty(presenceDependency)
      ? setPresence()
      : setPresence(presenceDependency[0], presenceDependency[1]);
  }, presenceDependency);

  const IdleService: any = lojector.get('IdleService');

  useIdleTimer({
    timeout: 1000, // 1 sec
    onIdle: () => IdleService.onIdleStart(),
    onActive: () => IdleService.onIdleEnd(),
    debounce: 500,
  });

  return (
    <div className="page-container">
      <PreviewAsLearnerLoader>
        <PageContentLoader>
          <StickyContainer>
            <LearnerPreviewHeader />
            <PageHeader
              children={renderBreadcrumbs()}
              navPanel={renderNavPanel()}
            />
          </StickyContainer>
          <main>
            {renderBanner()}
            <StickyContainer>
              <ReviewPeriodBanner />
            </StickyContainer>
            <MainContent
              wide={wide}
              children={children}
            />
          </main>
          {presenceChatFeature && <OnlineUsersPanel />}
        </PageContentLoader>
      </PreviewAsLearnerLoader>
    </div>
  );
};

export default PageContainer;
