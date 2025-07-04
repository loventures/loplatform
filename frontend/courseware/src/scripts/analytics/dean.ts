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

import axios from 'axios';
import contentsResource from '../resources/ContentsResource';
import { courseReduxStore } from '../loRedux';
import { ContentPlayerPageLink, LearnerCompetencyPlayerPageLink } from '../utils/pageLinks';
import { debounce, get, map, once } from 'lodash';
import qs from 'qs';
import { Store } from 'redux';

type ExternallyIdentifiableEntity = {
  id: number;
  externalId?: string;
};

type CourseId = {
  section: ExternallyIdentifiableEntity;
  assetGuid?: string;
  programId?: number;
  offeringId?: number;
  branchId?: number;
  commitId?: number;
};

type ContentId = {
  contentId: string;
  assetId?: string; // This is not optional on the back-end but filled in lazily in the front-end.
};

type CourseEntryFEEvent = {
  eventType: 'CourseEntryFEEvent';
  clientTime: string;
  course: CourseId;
  userAgent: string;
  framed: boolean;
};

type PageNavFEEvent = {
  eventType: 'PageNavFEEvent';
  clientTime: string;
  url: string;
  pageTitle?: string;
  course?: CourseId;
  content?: ContentId;
  impersonatedUserId?: number;
  er: boolean;
};

type TutorialViewFEEvent = {
  eventType: 'TutorialViewFEEvent';
  clientTime: string;
  tutorialName: string;
  autoplay: boolean;
  step: number;
  complete: boolean;
};

type FrontEndEvent = CourseEntryFEEvent | PageNavFEEvent | TutorialViewFEEvent; // | QuestionViewedFEEvent lol;

const hasContent = (event: FrontEndEvent): event is FrontEndEvent & { content: ContentId } =>
  typeof (event as any).content === 'object';

const toFullUrl = (path: string) => window.location.origin + window.location.pathname + '#' + path;

const getContentId = (location: Location): string | undefined => {
  const match =
    ContentPlayerPageLink.match(location.pathname) ||
    LearnerCompetencyPlayerPageLink.match(location.pathname);
  return match ? match.params.contentId : undefined;
};

const getViewingAsId = (location: Location): number | undefined => {
  const viewingAs = location.search && qs.parse(location.search.slice(1)).viewingAsId;
  return viewingAs && typeof viewingAs === 'string' ? parseInt(viewingAs, 10) : undefined;
};

type TrackingState = {
  queued: FrontEndEvent[];
  contentsLoaded: boolean;
};

const trackingState: TrackingState = {
  queued: [],
  contentsLoaded: false,
};

const sendImpl = () => {
  if (!trackingState.contentsLoaded) {
    return;
  }
  const ongoing = trackingState.queued;
  trackingState.queued = [];
  const contents = courseReduxStore.getState().api.contentItems;
  const payload = map(ongoing, event => {
    if (hasContent(event)) {
      event.content.assetId = contents[event.content.contentId]?.node_name;
    }
    return event;
  });
  // loConfig.dean.emit
  return axios
    .post('/api/v2/an/emit', payload)
    .catch(e => {
      console.warn('Analytics error:', e);
      trackingState.queued = trackingState.queued.concat(ongoing);
    })
    .then(() => {});
};

const send = debounce(sendImpl, 300);

export const subcribeDeanToRedux = (store: Store) => {
  const unsubscribe = store.subscribe(() => {
    const userId = window.lo_platform.user.id;
    const courseId = window.lo_platform.course.id;
    const fetching = contentsResource.isFetching(courseId, userId);
    const loadingState = store.getState().ui.contentPlayerLoadingState[userId];
    if (loadingState?.loaded || fetching === 0) {
      trackingState.contentsLoaded = true;
      send();
      unsubscribe();
    }
  });
};

const trackDeanEvent = (event: FrontEndEvent, immediate = false) => {
  if (window.lo_platform.environment.isMock) {
    return;
  }
  trackingState.queued.push(event);
  return immediate ? sendImpl() : send();
};

const courseId = (): CourseId => ({
  section: {
    id: window.lo_platform.course.id,
    externalId: window.lo_platform.course.externalId,
  },
  assetGuid: window.lo_platform.course.asset_guid,
  programId: window.lo_platform.course.program_id,
  offeringId: window.lo_platform.course.offering_id,
  branchId: window.lo_platform.course.branch_id,
  commitId: window.lo_platform.course.commitId,
});

const ensureTrackDeanCourseEntryEvent = once(() => {
  if (!window.lo_platform.course) {
    return;
  }
  trackDeanEvent({
    eventType: 'CourseEntryFEEvent',
    clientTime: new Date().toISOString(),
    course: courseId(),
    userAgent: navigator.userAgent.slice(0, 255),
    framed: window.top !== window.self,
  });
});

export const trackDeanPageNavEvent = (location: Location) => {
  // only track page navigate events for authenticated users
  if (!get(window.lo_platform, ['user', 'id'])) {
    return;
  }

  // only track page navigate events for inside courses
  if (!get(window.lo_platform, ['course', 'id'])) {
    return;
  }

  const url = toFullUrl(location.pathname);
  const viewingAsId = getViewingAsId(location);
  const contentId = getContentId(location);

  ensureTrackDeanCourseEntryEvent();

  const deanEvent: PageNavFEEvent = {
    eventType: 'PageNavFEEvent',
    clientTime: new Date().toISOString(),
    url: url,
    pageTitle: window.document.title,
    course: courseId(),
    impersonatedUserId: viewingAsId,
    er: true,
  };

  if (contentId && +contentId !== window.lo_platform.course.id) {
    deanEvent.content = {
      contentId,
    };
  }

  trackDeanEvent(deanEvent);
};

export const trackTutorialViewEvent = (
  tutorialName: string,
  autoplay: boolean,
  step: number,
  complete: boolean
): void => {
  const deanEvent: TutorialViewFEEvent = {
    eventType: 'TutorialViewFEEvent',
    clientTime: new Date().toISOString(),
    tutorialName,
    autoplay,
    step,
    complete,
  };

  trackDeanEvent(deanEvent);
};
