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

/**
 * Initializes the Google Analytics services to be used by our analytics events.
 */

const productionGA4 = 'G-Z83M0H02GX';
const nonProdGA4 = 'G-E96SMPQK8Q';
const getTrackingId = () => (window?.lo_platform?.isProduction ? productionGA4 : nonProdGA4);

/**
 * NOTE: @param action has a maximum length of 40 characters which is very cumbersome but not impossible to
 *        express in typescript. We leave that as an exercise for the reader.
 **/
const trackAuthoringEvent = (action: string, label?: string) => trackEvent(action, label);

const trackEvent = (action: string, label?: string) => {
  ga4.event(action, { event_label: label });
};

const initializeGoogleAnalytics = ({ title }) => {
  ga4.initialize(getTrackingId());
  ga4.set({ title: title || '', page_path: window.location.pathname });
};

export { initializeGoogleAnalytics, trackAuthoringEvent };
