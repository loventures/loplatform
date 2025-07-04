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

import { trackDeanPageNavEvent } from './dean';
import { sendPageView, trackEvent } from './ga';

export const trackPrintEvent = (contentType: string, contentId: string) => {
  trackEvent(
    'Print',
    'Printing Started',
    JSON.stringify({
      contentId, //need this for lesson
      contentType,
    })
  );
};

export const trackOpenExternalLinkEvent = () => {
  trackEvent('External Link', 'Open External Link');
};

export const trackViewInstructorResourcesEvent = () => {
  trackEvent('Instructor Resources', 'View Instructor Resources');
};

export const trackRichTextFailureEvent = (error: string) => {
  trackEvent('Rich Text', 'Rich Text Compile Failure', JSON.stringify({ error }));
};
export const trackImgCSSAlterationCompile = () =>
  trackEvent('Rich Text', 'Image altered by HTML Compile');

export const trackPrimitivesUsage = (type: string) =>
  trackEvent('Rich Text', 'Primitive directive used', type);

export const trackQuizKeyboard = (keyCode: number) => {
  trackEvent('Quiz Keyboard', `Pressed(${keyCode})`);
};

export const trackPageViewEvent = (location: Location) => {
  trackDeanPageNavEvent(location);
  sendPageView(location.pathname);
};

const surveyCollectorEvent = (action: any) => () => {
  trackEvent('Survey Collect', action);
};

export const trackSurveyCollectorNoEvent = surveyCollectorEvent('Said No');
export const trackSurveyCollectorLaterEvent = surveyCollectorEvent('Said Later');
export const trackSurveyCollectorYesEvent = surveyCollectorEvent('Said Yes');

export const trackSurveySidePanelInteraction = () =>
  trackEvent('Content Survey', 'Survey side panel opened');
export const trackSurveyInlineInteraction = () =>
  trackEvent('Content Survey', 'Survey inline interaction');
export const trackSurveySubmitInteraction = () => trackEvent('Content Survey', 'Survey submitted');

export const trackPrevNextLegacyInteraction = (direction: 'prev' | 'next') => {
  if (direction === 'prev') {
    trackEvent('Content Footer', 'Previous Button');
  } else if (direction === 'next') {
    trackEvent('Content Footer', 'Next Up Button');
  }
};
