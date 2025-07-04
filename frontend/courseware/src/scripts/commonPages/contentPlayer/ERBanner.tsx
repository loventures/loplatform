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
import User from '../../bootstrap/user';
import ERProgressCircle from '../../components/circles/ERProgressCircle';
import LoLink from '../../components/links/LoLink';
import { ERLandmark } from '../../landmarks/ERLandmarkProvider';
import { useFlatLearningPathResource } from '../../resources/LearningPathResource';
import { useCourseSelector } from '../../loRedux';
import { LearnerAssignmentListPageLink } from '../../utils/pageLinks';
import { isLoaded } from '../../types/loadable';
import { percentFilter } from '../../filters/percent';
import { useTranslation } from '../../i18n/translationContext';
import { selectCurrentUserOverallGrade } from '../../selectors/gradeSelectors';
import { selectCurrentUserOverallProgress } from '../../selectors/progressSelectors';
import { isAssignment } from '../../utilities/contentTypes';
import { selectCurrentUser } from '../../utilities/rootSelectors';
import React, { useMemo } from 'react';
import { lojector } from '../../loject';
import classNames from 'classnames';
import ERSidebarButton from '../sideNav/ERSidebarButton.tsx';
import { useMedia } from 'react-use';

type ERBannerProps = {
  showCircles?: boolean;
};

const Circles: React.FC = () => {
  const user = useCourseSelector(selectCurrentUser);
  const flatPath = useFlatLearningPathResource(user.id);
  const hasAssignments = Boolean(flatPath.find(isAssignment));
  const translate = useTranslation();
  const overallProgress = useCourseSelector(selectCurrentUserOverallProgress);
  const overallGrade = useCourseSelector(selectCurrentUserOverallGrade);
  const gradeFilter = lojector.get('gradeFilter') as any;
  const formattedGrade = gradeFilter(overallGrade, 'percentSign');
  const screenReaderFormattedGrade =
    formattedGrade === '–' ? translate('ER_CIRCLE_SCORE_NONE') : formattedGrade;
  return (
    <div className="er-progress-circles flex-shrink-0">
      <ERProgressCircle
        progress={overallProgress.normalizedProgress}
        explanation={translate('ER_CIRCLE_PROGRESS_EXPLANATION', {
          progress: percentFilter(overallProgress.normalizedProgress),
        })}
      >
        <span>{percentFilter(overallProgress.normalizedProgress)}</span>
        <p className="d-none d-md-block">complete</p>
      </ERProgressCircle>
      {hasAssignments ? (
        <>
          <div className="sr-only">
            {translate('ER_CIRCLE_SCORE_EXPLANATION', { score: screenReaderFormattedGrade })}
          </div>
          <LoLink
            className="btn m-1 m-md-2 score-circle"
            to={LearnerAssignmentListPageLink.toLink()}
            title={translate('ER_SCORES_TITLE')}
            id="score-circle-link"
          >
            <div
              className="er-prog-circle"
              role="status"
            >
              <div className="sr-only">{translate('ER_SCORES_TITLE')}</div>
              <div
                style={{ width: '100%', height: '100%' }}
                aria-hidden={true}
              >
                <svg
                  className="prog-circle-arc"
                  width="100%"
                  height="100%"
                  viewBox="0 0 200 200"
                  version="1.1"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <g
                    strokeWidth="8"
                    fill="none"
                  >
                    <circle
                      className="progress-background"
                      cx="100"
                      cy="100"
                      r="95"
                    />
                  </g>
                </svg>
                <div className="prog-circle-content">
                  <span>{formattedGrade}</span>
                  <p className="d-none d-md-block">{translate('ER_CIRCLE_SCORE')}</p>
                </div>
              </div>
            </div>
          </LoLink>
        </>
      ) : null}
    </div>
  );
};

const ERBanner: React.FC<ERBannerProps> = ({ showCircles }) => {
  const contents = useCourseSelector(state => state.api.contents[User.id]);
  const bannerName = useMemo(
    () => (isLoaded(contents) ? contents.data.find(c => c.id == '_root_')?.bannerImage : null),
    [contents]
  );
  const bgUrl = bannerName && createUrl(loConfig.course.banner, { context: Course.id });
  const translate = useTranslation();
  const mediumScreen = useMedia('(min-width: 48em)');
  return (
    <div className="er-dash-banner">
      <div
        className={classNames(
          'er-cover-image d-flex flex-column flex-md-row',
          !bgUrl && 'stock-cover-image'
        )}
        style={{ backgroundImage: bgUrl ? `url(${bgUrl})` : undefined }}
      >
        {!mediumScreen ? (
          <div className="d-flex justify-content-between d-md-none">
            <ERSidebarButton header dark />
            {showCircles ? <Circles /> : null}
          </div>
        ) : null}
        <div className="pt-0 pb-5 pb-md-0 flex-grow-1">
          <div className="h-100 d-flex gap-2 flex-column justify-content-center banner-text">
            <ERLandmark
              landmark="mainHeader"
              tag="h1"
              className="h3 mb-0"
              tabIndex={-1}
            >
              {Course.name}
            </ERLandmark>
            <p className="mb-0">
              {Course.groupId
                ? translate('ER_STUDENT_DASHBOARD_SECTION', { sectionName: Course.groupId })
                : ''}
            </p>
          </div>
        </div>
        {mediumScreen && showCircles ? <Circles /> : null}
      </div>
    </div>
  );
};

export default ERBanner;
