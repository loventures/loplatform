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

import classNames from 'classnames';
import SideNavStateService, { useSidepanelOpen } from '../commonPages/sideNav/SideNavStateService';
import AssetFeedback from '../feedback/AssetFeedback';
import { useFeedbackEnabled } from '../feedback/FeedbackStateService';
import AppFooter from '../landmarks/AppFooter';
import { MajickLayoutContext } from '../landmarks/appNavBar/MajickLayoutContext';
import ERPageHeaderContainer from '../landmarks/ERPageHeaderContainer';
import ERSidebarContainer from '../landmarks/ERSidebarContainer';
import { useCourseSelector, useUiState } from '../loRedux';
import QnaContainer from '../qna/QnaContainer';
import { ToastContainer } from '../directives/toast/toastsContainer';
import { selectContent } from '../courseContentModule/selectors/contentEntrySelectors';
import { selectFullscreenState } from '../courseContentModule/selectors/contentLandmarkSelectors';
import { qnaEnabled, smeFeedbackEnabled } from '../utilities/preferences';
import { selectPrintView, selectRouter } from '../utilities/rootSelectors';
import React, { useContext, useEffect, useMemo } from 'react';
import { useDispatch } from 'react-redux';
import { useRouteMatch } from 'react-router';

import { toggleQnaSideBar } from '../qna/qnaActions';

const ERAppContainer: React.FC<React.PropsWithChildren> = ({ children }) => {
  const [sidepanelOpen] = useSidepanelOpen();
  const {
    feedbackOpenState: { status: feedbackOpen },
    graderOpenState: { status: graderOpen },
    qna: { open: qnaOpen },
  } = useUiState();
  const match = useRouteMatch<{ questionId: string }>('/instructor/qna/question/:questionId'); // hack to check if we are using the qna Route
  const qnaRoute = Boolean(match?.params.questionId) ?? false;
  const {
    path,
    searchParams: { qna },
  } = useCourseSelector(selectRouter);
  const printView = useCourseSelector(selectPrintView);
  const fullScreen = !!useCourseSelector(selectFullscreenState);
  const { cssVariables, autohideSidebar } = useContext(MajickLayoutContext);
  const [instructorFeedbackEnabled] = useFeedbackEnabled();
  const content = useCourseSelector(selectContent);
  const feedbackEnabled = smeFeedbackEnabled || instructorFeedbackEnabled;
  const dispatch = useDispatch();

  /** On mobile, start with the sidebar closed. Putting it at this apex component means
   * the sidebar actually starts closed on mobile. */
  useEffect(() => {
    if (!autohideSidebar) SideNavStateService.openSideNav();
  }, [autohideSidebar]);

  /** On mobile, close the sidebar on any navigation event. */
  useEffect(() => {
    if (autohideSidebar) SideNavStateService.closeSideNav();
  }, [autohideSidebar, path]);

  /** On mobile, tap anywhere should close sidebar. */
  useEffect(() => {
    if (autohideSidebar && sidepanelOpen) {
      const callback = () => SideNavStateService.closeSideNav();
      document.addEventListener('click', callback, { once: true });
      return () => document.removeEventListener('click', callback);
    }
  }, [autohideSidebar, sidepanelOpen]);

  /** Close QNA sidebar on any navigation event, unless query param qna=true */
  useEffect(() => {
    if (qna && !qnaOpen) {
      dispatch(toggleQnaSideBar({ open: true }));
    } else if (!qna && qnaOpen) {
      dispatch(toggleQnaSideBar({ open: false }));
    }
  }, [qna, path]);

  const roles = useMemo(
    () =>
      window.lo_platform.user.roles.map(r => `role-${r.replace(/[^-_a-zA-Z0-9]/, '')}`).join(' '),
    [window.lo_platform.user.roles]
  );

  return (
    <div
      id="page-content"
      className={classNames(
        'er-page-grid',
        sidepanelOpen && !printView ? 'er-sidebar-open' : 'er-sidebar-closed',
        autohideSidebar && 'sidebar-autohide',
        fullScreen && 'full-screen',
        (feedbackOpen || graderOpen || qnaOpen) && content.node_name && 'feedback-open',
        qnaRoute && 'feedback-open',
        feedbackEnabled && 'feedback-enabled',
        qnaEnabled && 'qna-enabled',
        roles
      )}
      style={cssVariables}
    >
      {!printView && <ERPageHeaderContainer />}
      {!printView && <ERSidebarContainer />}
      {children} {/*Which is just the app routes*/}
      {feedbackEnabled && content.node_name && <AssetFeedback />}
      {qnaEnabled && <QnaContainer />}
      <AppFooter className="er-footer" />
      <ToastContainer />
    </div>
  );
};

export default ERAppContainer;
