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

import classnames from 'classnames';
import PropTypes from 'prop-types';
import React, { useEffect, useMemo, useState } from 'react';
import IdleTimer from 'react-idle-timer';
import { connect } from 'react-redux';
import { Link, Route, Switch, withRouter } from 'react-router-dom';
import { Alert } from 'reactstrap';
import { compose, lifecycle } from 'recompose';

import ActionBar from './actionbar/ActionBar';
import AnnouncementBar from './announcement/AnnouncementBar';
import CompetenciesEditor from './competency/CompetenciesEditor';
import DropboxRoutes from './dropbox/DropboxRoutes';
import ErrorDcm from './ErrorDcm';
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
  competenciesPath,
  contentSearchPath,
  dropboxPath,
  feedbackPath,
  feedbackPaths,
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
import { loadProgressBar } from './layout/progress.js';

const DcmApp = ({
  branchId,
  projectId,
  structureHidden,
  feedbackOpen,
  revisionMode,
  maintenance,
  dispatch,
}) => {
  const [idleTimerRef, setIdleTimerRef] = useState(null);
  /** The offset of the action bar from the viewport top in the range [0,navBarHeight]. */
  const [actionBarOffset, setActionBarOffset] = useState(56); // 3.5rem default
  const projectAccess = useProjectAccess();
  const visible = useDcmSelector(state => state.user.profile?.user_type !== 'Overlord');

  useEffect(() => {
    if (idleTimerRef && branchId && branchId > 0) {
      PresenceService.init(idleTimerRef);
      PresenceService.start({ branchId, visible });
    }
  }, [idleTimerRef, branchId]);

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
    () => ({
      '--actionbar-offset': `${actionBarOffset}px`,
    }),
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
      <IdleTimer
        ref={setIdleTimerRef}
        element={document}
        onActive={() => dispatch(setIdleState(false))}
        onIdle={() => dispatch(setIdleState(true))}
        timeout={60000}
        format="MM-DD-YYYY HH:MM:ss.SSS"
      />
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
        <Route
          path={storyPath}
          render={() => <NavigationSidebar />}
        />
        <main className="grid-main">
          <AnnouncementBar />
          <Switch>
            <Route
              path={rootPath}
              exact
            >
              <ProjectList />
            </Route>
            <Route path={contentSearchPath}>
              <ContentSearch />
            </Route>
            <Route
              path={storyPath}
              component={NarrativeMode}
            />
            <Route
              path={launchPath}
              component={LaunchAsset}
            />
            <Route
              path={revisionPath}
              component={RevisionMode}
            />
            {projectAccess.ViewObjectives && (
              <Route path={competenciesPath}>
                <CompetenciesEditor />
              </Route>
            )}
            {projectAccess.FeedbackApp && (
              <Route path={feedbackPath}>
                <FeedbackRoutes />
              </Route>
            )}
            {projectAccess.VaultApp && (
              <Route path={dropboxPath}>
                <DropboxRoutes />
              </Route>
            )}
            <Route>
              <div className="p-4">
                <Alert color="danger"><h1 className="mb-0 text-center">404</h1></Alert>
              </div>
            </Route>
          </Switch>
        </main>
        <Route
          path={revisionPath}
          component={RevisionPanel}
        />
        <Route
          path={feedbackPaths}
          render={({ match }) => (
            <FeedbackPanel
              narrative={match.path.includes('/story/')}
              detail={match.path.includes('/feedback/')}
            />
          )}
        />
        <Toast />
        <ModalContainer />
      </div>
    </div>
  );
};

DcmApp.propTypes = {
  structureHidden: PropTypes.bool.isRequired,
  feedbackOpen: PropTypes.bool.isRequired,
  revisionMode: PropTypes.bool.isRequired,
  dispatch: PropTypes.func.isRequired,
};

const mapStateToProps = state => ({
  structureHidden: state.projectStructure.hidden,
  feedbackOpen: state.feedback.open || !!state.feedback.addFeedback,
  revisionMode: ReactRouterService.isRevisionRoute(state),
  branchId: state.layout.branchId,
  projectId: state.layout.project?.id,
  maintenance: state.layout.project?.maintenance && state.user.profile?.user_type !== 'Overlord',
});

loadProgressBar({ showSpinner: false });

export default compose(
  withRouter, // to ensure rerender on path change
  connect(mapStateToProps),
  lifecycle({
    componentDidCatch(err) {
      reactRouterService.goToBranchError(err);
    },
  })
)(DcmApp);
