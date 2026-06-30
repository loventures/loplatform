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

import React, { useMemo } from 'react';

import { ActiveDiscussionsStoreLight } from '../../../assignment/ActiveDiscussionsStoreLight.ts';
import { useTranslation } from '../../../i18n/translationContext';
import { SrsList } from '../../../srs/react/SrsList';
import { SrsStore, useSrsStore } from '../../../srs/react/useSrsStore';
import { gotoLink } from '../../../utilities/routingUtils';
import { Roles } from '../../../utilities/pure/roles';
import { ContentPlayerPageLink, DiscussionListPageLink } from '../../../utils/pageLinks';

/**
 * React port of the `activeDiscussions` directive (B2): the instructor-dashboard "Active
 * Discussions" widget (sibling of `gradingQueue`). Previously an Angular component (a thin
 * controller over the Angular `active-discussions-list` directive → `<srs-list>`) bridged into
 * React via angular2react. It's now native React over the React SRS list stack (`SrsList` +
 * `useSrsStore`), driving the same Angular `ActiveDiscussionsStoreLight` store via lojector. Its
 * only renderer is the React `ERInstructorDashboard`; the `InstructorDashboardPage` Selenide page
 * object locates it by `active-discussions-react .card-list`, so the native output is wrapped in a
 * literal `<active-discussions-react>` host element (as the old angular2react bridge emitted). DOM
 * preserved from the old srs-list template: `.card-list.active-discussions-list`,
 * `ul.card-list-striped-body > li`, `.flex-row-content`, `.icon-discussion`, `.flex-col-fluid`
 * name, `.badge.badge-primary.badge-pill` unread + `.badge.badge-warning.badge-pill` unresponded.
 */
export const ActiveDiscussions: React.FC = () => {
  const translate = useTranslation();
  const store = useMemo(
    () => new ActiveDiscussionsStoreLight() as unknown as SrsStore & { hasDiscussions?: boolean },
    []
  );
  // Drive the load here (not just inside SrsList) so this component re-renders when the store
  // finishes loading — the `store.hasDiscussions`-gated view-all button below is computed in this
  // render, and SrsList's own re-renders wouldn't refresh a prop passed down from here.
  useSrsStore(store);
  const showUnresponded = !!Roles.isInstructor() && !Roles.isAdvisor();

  const viewDiscussion = (discussion: any) =>
    gotoLink(ContentPlayerPageLink.toLink({ content: discussion, nav: 'none' }));
  const viewDiscussionsPage = () => gotoLink(DiscussionListPageLink.toLink());

  return (
    <ActiveDiscussionsHost>
      <SrsList
        store={store}
        autoload={false}
        className="active-assignments-list active-discussions-list"
        headerText="ACTIVE_DISCUSSION_LIST_TITLE"
        emptyMsg="DASHBOARD_ACTIVE_DISCUSSIONS_NO_ACTIVITY"
        filteredMsg="DASHBOARD_ACTIVE_DISCUSSIONS_NO_ACTIVITY"
        emptyIsGood
        headerButton={
          (store as any).hasDiscussions
            ? { label: 'DISCUSSION_LIST_VIEW_ALL', onClick: viewDiscussionsPage }
            : undefined
        }
        getItemKey={(discussion: any) => discussion.id}
        renderItem={(discussion: any) => (
          // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions, jsx-a11y/click-events-have-key-events
          <li
            title={discussion.name}
            onClick={() => viewDiscussion(discussion)}
          >
            <div className="flex-row-content">
              <span className={`icon icon-${discussion.activityType}`} />
              <div className="flex-col-fluid">{discussion.name}</div>
              <span
                className="badge badge-primary badge-pill"
                title={translate('DISCUSSION_PREVIEW_POST_COUNT_UNREAD')}
              >
                {discussion.activeCount}
              </span>
              {showUnresponded && (
                <span
                  className="badge badge-warning badge-pill"
                  title={translate('DISCUSSION_PREVIEW_POST_COUNT_UNRESPONDED')}
                >
                  {discussion.unrespondedCount}
                </span>
              )}
            </div>
          </li>
        )}
      />
    </ActiveDiscussionsHost>
  );
};

// Custom-element host tag (rendered literally as `<active-discussions-react>`), kept for the
// Selenide page-object selector that the old angular2react bridge used to produce.
const ActiveDiscussionsHost = 'active-discussions-react' as any;

