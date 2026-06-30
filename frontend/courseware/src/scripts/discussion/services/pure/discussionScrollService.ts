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

import { values } from 'lodash';

/**
 * Pure (framework-free) port of the AngularJS `DiscussionScrollService`. Scrolls to a discussion post
 * once it has rendered and flashes its highlight class. The Angular original used
 * $q/$document.duScrollToElementAnimated/$location.hash/$timeout/angular.element; this uses native
 * Promises, Element.scrollIntoView, location.hash, setTimeout and querySelector/classList. Consumed by
 * the React/redux DiscussionViewActions directly (was a lojector reach-in).
 */
const DiscussionScrollFlashClasses: Record<string, string> = {
  unread: 'flash-unread',
  new: 'flash-unread',
  bookmarked: 'flash-bookmarked',
  unresponded: 'flash-unresponded',
  'user-posts': 'flash-user-posts',
  search: 'flash-search',
  'reported-inappropriate-posts': 'flash-reported-inappropriate-posts',
};

const allClasses = values(DiscussionScrollFlashClasses).join(' ');

const continueRenderedCheck = (selector: string, resolve: (el: Element) => void): void => {
  // need to re-fetch to check whether it is rendered
  const element = document.querySelector(selector);
  if (element) {
    resolve(element);
  } else {
    setTimeout(() => continueRenderedCheck(selector, resolve), 100);
  }
};

const waitForRendering = (hash: string): Promise<Element> =>
  new Promise(resolve => {
    continueRenderedCheck('.discussion-single-thread-view #' + hash, resolve);
  });

const scrollTo = (element: Element, hash: string): Promise<Element> => {
  location.hash = hash;
  element.scrollIntoView({ behavior: 'smooth', block: 'start' });
  return Promise.resolve(element);
};

const flash = (element: Element, type: string): Promise<void> => {
  element.classList.remove('flash', ...allClasses.split(' '));
  const classes = DiscussionScrollFlashClasses[type] || '';
  return new Promise(resolve => {
    setTimeout(() => {
      element.classList.add('flash', ...classes.split(' ').filter(Boolean));
      resolve();
    }, 500);
  });
};

export const discussionScrollService = {
  scrollToAndFlash(postId: any, { flashType }: { flashType: string }): Promise<void> {
    const hash = 'discussion-item-' + postId;
    return waitForRendering(hash)
      .then(element => scrollTo(element, hash))
      .then(element => flash(element, flashType));
  },
};
