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

import { withTranslation } from '../../i18n/translationContext';
import { surveyCollectorUrl } from '../../utilities/preferences';
import * as React from 'react';
import { connect } from 'react-redux';

import { togglePlaylistNavActionCreator } from './actions';
import { headerPageSelector } from './pageHeaderSelectors';
import InstructorNavMenu from './parts/InstructorNavMenu';
import LearnerNavMenu from './parts/LearnerNavMenu';
import PageHeaderPlaylistNavToggle from './parts/PageHeaderPlaylistNavToggle';
import PageHeaderPlayTutorialButton from './parts/PageHeaderPlayTutorialButton';
import { PageHeaderPresenceWidget } from './parts/PageHeaderPresenceWidget';
import PageHeaderSendMessageLink from './parts/PageHeaderSendMessageLink';
import SurveyCollector from './parts/SurveyCollector';

const PageHeader = ({
  authoringUrl,
  show,
  isAuthor,
  isStudent,
  togglePlaylistNav,
  translate,
  navPanel,
  children,
}: any) =>
  show.pageHeader && (
    <div className="page-navbar-container bg-primary">
      <nav
        className="page-navbar navbar navbar-dark bg-primary"
        role="navigation"
        aria-label={translate('PAGE_HEADER_NAVBAR')}
      >
        <div className="navbar-nav">
          {isStudent && show.studentNav && <LearnerNavMenu />}
          {!isStudent && <InstructorNavMenu />}

          {show.breadCrumbs ? children : <div className="page-breadcrumbs"></div>}

          {surveyCollectorUrl && <SurveyCollector />}

          {isAuthor && authoringUrl && (
            <a
              className="goto-authoring"
              href={authoringUrl}
              title={translate('PAGE_HEADER_EDIT_IN_AUTHORING')}
            >
              <span className="nav-icon icon-leaf"></span>
              <span className="sr-only">{translate('PAGE_HEADER_EDIT_IN_AUTHORING')}</span>
            </a>
          )}

          {isStudent && show.tutorial && <PageHeaderPlayTutorialButton glow={show.tutorialGlow} />}

          {(show.presenceChat || show.groupChat) && (
            <PageHeaderPresenceWidget
              showPresenceChat={show.presenceChat}
              showGroupChat={show.groupChat}
            />
          )}

          {show.messageLink && <PageHeaderSendMessageLink />}

          {show.playlistNav && (
            <PageHeaderPlaylistNavToggle togglePlaylistNav={togglePlaylistNav} />
          )}
          {show.playlistNav && navPanel}
        </div>
      </nav>
    </div>
  );

type PageHeaderOwnProps = {
  children: any;
  navPanel: JSX.Element | null;
};

type PageHeaderStateProps = {
  isStudent: boolean;
  show: any;
};

type PageHeaderDispatchProps = {
  togglePlaylistNav: () => void;
};

//TODO: type all the selectors?  This might fix typing being lost when connecting
function mapStateToProps(state: any, props: any): PageHeaderStateProps {
  return headerPageSelector(state, props);
}

const ConnectedPageHeader = connect<
  PageHeaderStateProps,
  PageHeaderDispatchProps,
  PageHeaderOwnProps
>(mapStateToProps, {
  togglePlaylistNav: togglePlaylistNavActionCreator,
})(withTranslation(PageHeader) as any);

export default ConnectedPageHeader;
