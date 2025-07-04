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

import Course from '../../bootstrap/course';
import { createUrl, loConfig } from '../../bootstrap/loConfig';
import ERHeaderPresenceWidget from '../../landmarks/pageHeader/parts/ERHeaderPresenceWidget';
import ERPlayTutorialButton from '../../landmarks/pageHeader/parts/ERPlayTutorialButton';
import SurveyCollector from '../../landmarks/pageHeader/parts/SurveyCollector';
import { useCourseSelector } from '../../loRedux';
import * as tutorialSlice from '../../tutorial/tutorialSlice';
import { SearchLink } from '../../utils/pageLinks';
import { AuthoringAppRight } from '../../utils/rights';
import dayjs from 'dayjs';
import {
  isCourseWithRelationships,
  selectPageContent,
} from '../../courseContentModule/selectors/contentEntrySelectors';
import { useTranslation } from '../../i18n/translationContext';
import { appIsFramed } from '../../utilities/deviceType';
import {
  allowDirectMessaging,
  contentSearch,
  groupChatFeature,
  presenceChatFeature,
  surveyCollectorUrl,
} from '../../utilities/preferences';
import { selectCurrentUser } from '../../utilities/rootSelectors';
import React from 'react';
import { IoLeafOutline, IoSearchOutline } from 'react-icons/io5';
import { Link } from 'react-router-dom';

import PageHeaderSendMessageLink from '../pageHeader/parts/PageHeaderSendMessageLink';
import AppNavUserDropdown from './AppNavUserDropdown';
import DomainLogo from './DomainLogo';

const ERAppNav: React.FC = () => {
  const translate = useTranslation();
  const currentUser = useCourseSelector(selectCurrentUser);
  const isAuthor = currentUser?.rights?.includes(AuthoringAppRight) ?? false;
  const activeContent = useCourseSelector(selectPageContent);
  const authoringUrl = isCourseWithRelationships(activeContent)
    ? createUrl(loConfig.authoring.course, { courseId: Course.id })
    : createUrl(loConfig.authoring.content, {
        courseId: Course.id,
        contentId: activeContent.id,
      });

  const showChat =
    !(Course.endDate && dayjs().isAfter(Course.endDate)) &&
    (presenceChatFeature || groupChatFeature);
  const isMeek = activeContent?.id === Course.contentItemRoot;
  const showMessageLink = allowDirectMessaging && !isMeek;
  const isPreviewing = currentUser?.user_type === 'Preview';

  const showTutorialBtn = useCourseSelector(tutorialSlice.selectShowManuallyPlay);
  const glowTutorialBtn = useCourseSelector(tutorialSlice.selectShowManuallyPlayGlow);

  return Course.noHeader ? null : (
    <nav
      className={'app-header er-dash-enabled'}
      aria-label={translate('TOP_NAV_LABEL')}
    >
      {!appIsFramed && (
        <div className="navbar-brand flex-center-center py-0">
          <DomainLogo />
        </div>
      )}
      <div className="navbar-nav ms-auto">
        {surveyCollectorUrl ? <SurveyCollector /> : null}

        {isAuthor && authoringUrl && (
          <a
            className="goto-authoring btn border-white btn-outline-primary d-none d-md-flex"
            href={authoringUrl}
            title={translate('PAGE_HEADER_EDIT_IN_AUTHORING')}
          >
            <IoLeafOutline
              size="1.5rem"
              style={{ transform: 'scaleX(-1)' }}
              className="thin-leaf-icon"
              aria-hidden
            />
            <span className="sr-only">{translate('PAGE_HEADER_EDIT_IN_AUTHORING')}</span>
          </a>
        )}

        {currentUser.isStudent && !isPreviewing && showTutorialBtn && (
          <ERPlayTutorialButton glow={glowTutorialBtn} />
        )}

        {contentSearch && (
          <Link
            to={SearchLink.toLink}
            className="btn btn-outline-primary border-white d-none d-md-flex"
            title={translate('PAGE_HEADER_SEARCH')}
          >
            <IoSearchOutline
              size="1.5rem"
              className="thin-search-icon"
              aria-hidden
            />
            <span className="sr-only">{translate('PAGE_HEADER_SEARCH')}</span>
          </Link>
        )}

        {showChat && (
          <ERHeaderPresenceWidget
            showPresenceChat={presenceChatFeature}
            showGroupChat={groupChatFeature}
          />
        )}

        {showMessageLink ? <PageHeaderSendMessageLink /> : null}

        <AppNavUserDropdown authoringUrl={authoringUrl} />
      </div>
    </nav>
  );
};

export default ERAppNav;
