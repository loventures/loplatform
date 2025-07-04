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

import { trackPageViewEvent } from './analytics/trackEvents';
import ERSendMessagePage from './commonPages/directMessage/ERSendMessagePage';
import { useCourseSelector } from './loRedux';
import { redirectPreserveParams } from './utils/linkUtils';
import LoadingSpinner from './directives/loadingSpinner';
import { ContentWithRelationships } from './courseContentModule/selectors/assembleContentView';
import { selectPageContent } from './courseContentModule/selectors/contentEntrySelectors';
import { history } from './utilities/history';
import { allowDirectMessaging } from './utilities/preferences';
import { selectCurrentUser } from './utilities/rootSelectors';
import React, { Suspense, useEffect } from 'react';
import { Redirect, Route, Switch, useHistory } from 'react-router';

const ERInstructorPageRoutes = React.lazy(
  () =>
    import(
      /* webpackChunkName: "ERInstructorPageRoutes" */ './instructorPages/ERInstructorPageRoutes'
    )
);

const ERLearnerPageRoutes = React.lazy(
  () => import(/* webpackChunkName: "ERLearnerPageRoutes" */ './studentPages/ERLearnerPageRoutes')
);

const ERAppRoutes = () => {
  const currentUser = useCourseSelector(selectCurrentUser);
  useEffect(() => {
    // Send the initial loaded page
    // @ts-ignore
    trackPageViewEvent(history.location);
    // Listen for all history events to send a page view
    // @ts-ignore
    history.listen(trackPageViewEvent);
  }, [history]);
  useLoNav();
  return (
    <Suspense
      fallback={
        <div className="d-flex align-items-center justify-content-center">
          <div className="mb-4 text-center fade-in text-muted">
            <LoadingSpinner />
          </div>
        </div>
      }
    >
      <Switch>
        <Route
          path="/instructor"
          exact={false}
          render={({ location }) =>
            currentUser.isInstructor ? (
              <ERInstructorPageRoutes />
            ) : (
              <Redirect
                to={redirectPreserveParams(
                  location.pathname.replace('/instructor/', '/student/'),
                  location
                )}
              />
            )
          }
        />

        <Route
          path="/student"
          exact={false}
          render={({ location }) =>
            currentUser.isStudent ? (
              <ERLearnerPageRoutes />
            ) : (
              <Redirect
                to={redirectPreserveParams(
                  location.pathname.replace('/student/', '/instructor/'),
                  location
                )}
              />
            )
          }
        />

        {allowDirectMessaging && (
          <Route path="/send-message">
            <ERSendMessagePage />
          </Route>
        )}

        <Route
          render={({ location }) => (
            <Redirect
              to={redirectPreserveParams(
                currentUser.isStudent ? '/student/dashboard' : '/instructor/dashboard',
                location
              )}
            />
          )}
        />
      </Switch>
    </Suspense>
  );
};

const useLoNav = () => {
  const history = useHistory();
  const { hyperlinks } = useCourseSelector(selectPageContent) as ContentWithRelationships;
  const currentUser = useCourseSelector(selectCurrentUser);
  useEffect(() => {
    window.lonav = (edgeOrEvent: string | MouseEvent) => {
      let edgeId: string, target: string | undefined;
      if (typeof edgeOrEvent === 'string') {
        // legacy javascript: href
        edgeId = edgeOrEvent;
      } else {
        // modern onclick()
        const event = edgeOrEvent;
        const element = event.target as Element;
        event.preventDefault();
        // Matches EDGEID from javascript:lonav('EDGEID')
        edgeId = element.getAttribute('href')!.replace(/.*['"]([^'"]*)['"].*/, '$1');
        target =
          element?.getAttribute('target') ??
          (event.metaKey || event.ctrlKey ? '_blank' : undefined);
      }
      const edgePath = hyperlinks[edgeId];
      if (edgePath) {
        const role = currentUser.isStudent ? 'student' : 'instructor';
        const to = `/${role}/content/${edgePath}`;
        const url = window.location.href.replace(/#.*/, '');
        if (target) window.open(`${url}#${to}`, target);
        else history.push(to);
      }
    };
    return () => {
      delete window.lonav;
    };
  }, [hyperlinks, currentUser, history]);
};

export default ERAppRoutes;
