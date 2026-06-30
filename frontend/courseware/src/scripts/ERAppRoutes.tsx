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

import { trackPageViewEvent } from './analytics/trackEvents';
import ERSendMessagePage from './commonPages/directMessage/ERSendMessagePage';
import { useCourseSelector } from './loRedux';
import { redirectPreserveParams } from './utils/linkUtils';
import LoadingSpinner from './directives/loadingSpinner';
import { ContentWithRelationships } from './courseContentModule/selectors/assembleContentView';
import { selectPageContent } from './courseContentModule/selectors/contentEntrySelectors';
import { history } from './utilities/history';
import { allowDirectMessaging } from './utilities/preferences';
import { selectActualUser, selectCurrentUser } from './utilities/rootSelectors';
import React, { Suspense, useEffect } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate, useSearchParams } from 'react-router-dom';

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
  const actualUser = useCourseSelector(selectActualUser);
  // Source the preview flag from the React Router (deferred) location rather than the eagerly-synced
  // redux router state. Under v7's startTransition the redux path (synced synchronously via
  // history.listen) flips before the matched <Routes> location, so a redux-derived role gate fires a
  // transient RoleRedirect and bounces preview enter/exit. Reading previewAsUserId off useSearchParams
  // keeps the role decision atomic with the route match below. Role flags reduce to: previewing always
  // means the student view (see withRoleInfo — a preview user is never the actual user), otherwise the
  // actual user's role applies.
  const [searchParams] = useSearchParams();
  const previewing = !!searchParams.get('previewAsUserId');
  const isStudent = previewing || actualUser.isStudent;
  const isInstructor = !previewing && actualUser.isInstructor;
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
      <Routes>
        <Route
          path="/instructor/*"
          element={
            isInstructor ? (
              <ERInstructorPageRoutes />
            ) : (
              <RoleRedirect
                from="/instructor/"
                to="/student/"
              />
            )
          }
        />

        <Route
          path="/student/*"
          element={
            isStudent ? (
              <ERLearnerPageRoutes />
            ) : (
              <RoleRedirect
                from="/student/"
                to="/instructor/"
              />
            )
          }
        />

        {allowDirectMessaging && (
          <Route
            path="/send-message"
            element={<ERSendMessagePage />}
          />
        )}

        <Route
          path="*"
          element={
            <DefaultRedirect
              to={isStudent ? '/student/dashboard' : '/instructor/dashboard'}
            />
          }
        />
      </Routes>
    </Suspense>
  );
};

// Role-mismatch and fallback redirects need the current location (for param preservation),
// which v6 exposes via a hook rather than a render-prop, so they live in small components.
const RoleRedirect: React.FC<{ from: string; to: string }> = ({ from, to }) => {
  const location = useLocation();
  return (
    <Navigate
      replace
      to={redirectPreserveParams(location.pathname.replace(from, to), location)}
    />
  );
};

const DefaultRedirect: React.FC<{ to: string }> = ({ to }) => {
  const location = useLocation();
  return (
    <Navigate
      replace
      to={redirectPreserveParams(to, location)}
    />
  );
};

const useLoNav = () => {
  const navigate = useNavigate();
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
        else navigate(to);
      }
    };
    return () => {
      delete window.lonav;
    };
  }, [hyperlinks, currentUser, navigate]);
};

export default ERAppRoutes;
