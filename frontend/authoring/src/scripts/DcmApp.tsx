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

import classnames from 'classnames';
import React, { useEffect, useMemo, useState } from 'react';
import { useIdleTimer } from 'react-idle-timer';
import { connect } from 'react-redux';
import { Route, Routes, useMatch } from 'react-router-dom';
import { Alert } from 'reactstrap';
import { compose } from 'redux';

import ActionBar from './actionbar/ActionBar';
import AnnouncementBar from './announcement/AnnouncementBar';
import DropboxRoutes from './dropbox/DropboxRoutes';
import FeedbackLoader from './feedback/FeedbackLoader';
import FeedbackPanel from './feedback/FeedbackPanel';
import FeedbackRoutes from './feedback/FeedbackRoutes';
import { useDcmSelector } from './hooks';
import ModalContainer from './modals/ModalContainer';
import Navigation from './nav/Navigation';
import { setIdleState } from './presence/PresenceActions';
import PresenceService from './presence/services/PresenceService';
import { ContentSearch } from './projects/ContentSearch';
import { ProjectList } from './projects/ProjectList';
import { RevisionMode } from './revision/RevisionMode';
import RevisionPanel from './revision/RevisionPanel';
import reactRouterService from './router/ReactRouterService';
import ReactRouterService from './router/ReactRouterService';
import {
  contentSearchPath,
  dropboxPath,
  feedbackItemPath,
  feedbackPath,
  launchPath,
  revisionPath,
  rootPath,
  storyPath,
} from './router/routes';
import { useProjectAccess } from './story/hooks';
import { LaunchAsset } from './story/LaunchAsset';
import { NarrativeMode } from './story/NarrativeMode';
import { NavigationSidebar } from './story/NavigationSidebar';
import StructureLoader from './structurePanel/StructureLoader';
import Toast from './toast/Toast';
import { loadProgressBar } from './layout/progress';
import { DcmState } from './types/dcmState';
import { Dispatch } from 'redux';

interface DcmAppProps {
  branchId?: number;
  projectId?: number;
  structureHidden: boolean;
  feedbackOpen: boolean;
  revisionMode: boolean;
  maintenance?: boolean;
  dispatch: Dispatch;
}

const DcmApp = ({
  branchId,
  projectId,
  structureHidden,
  feedbackOpen,
  revisionMode,
  maintenance,
  dispatch,
}: DcmAppProps) => {
  // react-idle-timer v5 replaced the default-export <IdleTimer> component with the
  // useIdleTimer hook. The returned object is recreated each render but its methods are
  // stable useCallbacks, so capturing it once in PresenceService is safe.
  const idleTimer = useIdleTimer({
    element: document,
    onActive: () => dispatch(setIdleState(false)),
    onIdle: () => dispatch(setIdleState(true)),
    timeout: 60000,
  });
  /** The offset of the action bar from the viewport top in the range [0,navBarHeight]. */
  const [actionBarOffset, setActionBarOffset] = useState(56); // 3.5rem default
  const projectAccess = useProjectAccess();
  const visible = useDcmSelector(state => state.user.profile?.user_type !== 'Overlord');

  // v6 removed standalone <Route> as a conditional render — these layout panels sit outside the
  // main <Routes>, so drive them with useMatch (prefix match via `/*`, mirroring v5 non-exact).
  const onStory = useMatch(`${storyPath}/*`);
  const onRevision = useMatch(`${revisionPath}/*`);
  const onFeedbackItem = useMatch(`${feedbackItemPath}/*`);

  useEffect(() => {
    if (branchId && branchId > 0) {
      PresenceService.init(idleTimer);
      PresenceService.start({ branchId, visible });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [branchId]);

  // record the most recent 10 projects in local storage
  useEffect(() => {
    if (!projectId) return;
    const mrp = localStorage.getItem('MRP')?.split(',') ?? [];
    const projectIds = mrp.filter(s => s && parseInt(s) !== projectId).slice(0, 9);
    localStorage.setItem('MRP', [projectId, ...projectIds].join(','));
  }, [projectId]);

  // This is garbage but reactstrap Navbar doesn't support inner-ref. getElementById is,
  // one presumes, sufficiently performant for this.
  const navBar = document.getElementById('navbar');

  /** In which we compute a `actionBarOffset` value that lets us know how far the nav header
   * has scrolled off-screen in order to maintain maximal sidebar height. Uses an intersection
   * observer so we only maintain a scrollbar listener while the header is on screen.
   */
  useEffect(() => {
    if (navBar && typeof IntersectionObserver === 'function') {
      let scrollWatch = false;
      const calculateHeaderOffset = () => {
        setActionBarOffset(Math.max(0, Math.round(navBar.getBoundingClientRect().bottom)));
      };
      const intersector = new IntersectionObserver(([entry]) => {
        calculateHeaderOffset();
        if (entry.isIntersecting && !scrollWatch) {
          scrollWatch = true;
          window.addEventListener('scroll', calculateHeaderOffset);
        } else if (scrollWatch && !entry.isIntersecting) {
          scrollWatch = false;
          window.removeEventListener('scroll', calculateHeaderOffset);
        }
      });
      calculateHeaderOffset();
      intersector.observe(navBar);
      return () => {
        intersector.disconnect();
        if (scrollWatch) window.removeEventListener('scroll', calculateHeaderOffset);
      };
    }
  }, [navBar, setActionBarOffset]);

  const cssVariables = useMemo(
    () =>
      ({
        '--actionbar-offset': `${actionBarOffset}px`,
      }) as React.CSSProperties,
    [actionBarOffset]
  );

  return maintenance ? (
    <div className="d-flex align-items-center justify-content-center w-100 vh-100">
      <Alert color="danger">
        <strong>Project Unavailable:</strong> This project is temporarily unavailable while it
        undergoes maintenance.
      </Alert>
    </div>
  ) : (
    <div
      className="position-relative"
      style={cssVariables}
    >
      <FeedbackLoader />
      <StructureLoader />
      <div
        className={classnames(
          'grid-container',
          structureHidden && 'structure-hidden',
          (feedbackOpen || revisionMode) && 'feedback-open',
          !actionBarOffset && 'zero-nav'
        )}
      >
        <Navigation hidden={!actionBarOffset} />
        <ActionBar stuck={!actionBarOffset} />
        {onStory && <NavigationSidebar />}
        <main className="grid-main">
          <AnnouncementBar />
          <Routes>
            <Route
              path={rootPath}
              element={<ProjectList />}
            />
            <Route
              path={contentSearchPath}
              element={<ContentSearch />}
            />
            <Route
              path={`${storyPath}/*`}
              element={<NarrativeMode />}
            />
            <Route
              path={`${launchPath}/*`}
              element={<LaunchAsset />}
            />
            <Route
              path={`${revisionPath}/*`}
              element={<RevisionMode />}
            />
            {projectAccess.FeedbackApp && (
              <Route
                path={`${feedbackPath}/*`}
                element={<FeedbackRoutes />}
              />
            )}
            {projectAccess.VaultApp && (
              <Route
                path={`${dropboxPath}/*`}
                element={<DropboxRoutes />}
              />
            )}
            <Route
              path="*"
              element={
                <div className="p-4">
                  <Alert color="danger">
                    <h1 className="mb-0 text-center">404</h1>
                  </Alert>
                </div>
              }
            />
          </Routes>
        </main>
        {onRevision && <RevisionPanel />}
        {(onStory || onFeedbackItem) && (
          <FeedbackPanel
            narrative={!!onStory}
            detail={!!onFeedbackItem}
          />
        )}
        <Toast />
        <ModalContainer />
      </div>
    </div>
  );
};



const mapStateToProps = (state: DcmState) => ({
  structureHidden: state.projectStructure.hidden,
  feedbackOpen: state.feedback.open || !!state.feedback.addFeedback,
  revisionMode: ReactRouterService.isRevisionRoute(state),
  branchId: state.layout.branchId,
  projectId: state.layout.project?.id,
  maintenance: state.layout.project?.maintenance && state.user.profile?.user_type !== 'Overlord',
});

loadProgressBar({ showSpinner: false });

// Replaces recompose's `lifecycle({ componentDidCatch })`: an error-boundary HOC
// (componentDidCatch requires a class, so there is no hook equivalent).
const withBranchErrorBoundary = (Component: React.ComponentType<any>) =>
  class extends React.Component<any> {
    componentDidCatch(err: any) {
      reactRouterService.goToBranchError(err);
    }
    render() {
      return <Component {...this.props} />;
    }
  };

// (v5 withRouter — "to ensure rerender on path change" — is unnecessary in v6: the useMatch hooks
// above subscribe to the router, and connect re-runs on the state.router updates, so DcmApp already
// re-renders on navigation.)
export default compose(
  connect(mapStateToProps),
  withBranchErrorBoundary
)(DcmApp as any) as React.ComponentType<any>;
