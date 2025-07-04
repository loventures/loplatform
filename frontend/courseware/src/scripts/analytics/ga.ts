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

import ga4 from 'react-ga4';

const isProduction = window.lo_platform.isProduction;
const gaToken = window.lo_platform.preferences.googleAnalyticsToken; // Domain Settings
const gaToken2 = window.lo_platform.domain.googleAnalyticsAccount; // Course Preferences
const loGA4Token = 'G-1L466JHVX7';

const trackers = [gaToken, gaToken2, loGA4Token].filter(x => !!x);
const enabled = trackers.length > 0;

if (enabled) {
  ga4.initialize(
    trackers.map(tracker => {
      return {
        trackingId: tracker,
      };
    }),
    {
      testMode: !isProduction,
    }
  );
  ga4.set({
    courseId: window.lo_platform?.course?.id,
    userId: window.lo_platform?.user?.id,
  });
}

export const sendPageView = (pathname: string) => {
  if (enabled) {
    ga4.set({ page: pathname });
    ga4.send({ hitType: 'pageview', page_path: pathname });
  }
};

export const trackEvent = (category: string, action: string, label = '') => {
  if (enabled) {
    ga4.event(action, { event_label: label, event_category: category });
  }
};
