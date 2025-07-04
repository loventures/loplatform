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
import useGatingTooltip from '../commonPages/sideNav/useGatingTooltip';
import { AccessCodeWidget } from '../components/accessCodes/AccessCodeWidget';
import { ERLandmark } from '../landmarks/ERLandmarkProvider';
import { useNextUpFullCourse } from '../resources/useNextUp';
import { useCourseSelector } from '../loRedux';
import LearnerNudgeList from '../studentPages/nudges/LearnerNudgeList';
import { ResetLearnerDataWidget } from '../studentPages/ResetLearnerDataWidget';
import Tutorial from '../tutorial/Tutorial';
import { ContentPlayerPageLink } from '../utils/pageLinks';
import { percentFilter } from '../filters/percent';
import { useTranslation } from '../i18n/translationContext';
import { selectCurrentUser } from '../utilities/rootSelectors';
import React from 'react';
import { FiLock } from 'react-icons/fi';
import { GiPartyPopper } from 'react-icons/gi';

import ERBanner from '../commonPages/contentPlayer/ERBanner';
import LoLink from '../components/links/LoLink';
import ERContentContainer from '../landmarks/ERContentContainer';

const ERStudentDashboard: React.FC = () => {
  const translate = useTranslation();
  const viewingAs = useCourseSelector(selectCurrentUser);
  const [nextUp, firstUp] = useNextUpFullCourse();
  const { linkProps, locked, GatingTooltip, showLockIcon } = useGatingTooltip(
    nextUp?.id ?? '_root_',
    'next-up-button'
  );
  const nextUnit = nextUp?.unit;
  const nextModule = nextUp?.module;

  return (
    <ERContentContainer title={translate('ER_STUDENT_DASHBOARD')}>
      <ERLandmark
        landmark="content"
        id="er-student-dashboard"
        className="d-flex flex-column justify-content-between"
      >
        <ERBanner showCircles={true} />
        <div className="m-lg-4">
          <div className="d-flex flex-column justify-content-between container p-4">
            <LearnerNudgeList />
            {nextUp ? (
              <div className="flex-col flex-lg-row card next-up-card border-0">
                <div className="p-3 p-md-4 p-lg-5 flex-grow-1 d-flex flex-column justify-content-center">
                  <h3 className="h4 text-center text-lg-left">
                    {nextModule && nextUnit && (
                      <div className="h6 text-center text-lg-left">{nextUnit.name}</div>
                    )}
                    {(nextModule ?? nextUnit)?.name}
                  </h3>
                  <div className="pb-3">{(nextModule ?? nextUnit)?.description}</div>
                  {nextModule?.progress ? (
                    <div className="d-flex align-items-baseline justify-content-center justify-content-lg-start">
                      <div
                        className="er-progress-bar"
                        style={{
                          maxWidth: '10rem',
                          width: '50%',
                        }}
                      >
                        <div className="er-progress-bar-outer">
                          <div
                            className="er-progress-bar-inner"
                            style={{
                              width: `${nextModule.progress.weightedPercentage}%`,
                            }}
                          />
                        </div>
                      </div>
                      <div className="ms-3 small text-nowrap">
                        {percentFilter(nextModule.progress.weightedPercentage / 100)} complete
                      </div>
                    </div>
                  ) : null}
                </div>
                <div className="border-left p-3 p-md-4 p-lg-5 flex-column-center justify-content-center">
                  <div>{translate(firstUp ? 'FIRST_ACTIVITY_BUTTON' : 'NEXT_ACTIVITY_BUTTON')}</div>
                  <div className="text-center next-activity">{nextUp.name}</div>
                  <div>{nextUp.duration ? translate('AssetNextDuration', nextUp) : ''}</div>
                  <LoLink
                    className={classnames(
                      'btn btn-light btn-lg text-primary my-3 d-flex align-items-center',
                      { locked }
                    )}
                    style={{ textDecoration: 'none' }}
                    to={ContentPlayerPageLink.toLink({ content: nextUp })}
                    {...linkProps}
                  >
                    {translate(firstUp ? 'ER_NEXT_UP_GET_STARTED' : 'ER_NEXT_UP_CONTINUE')}
                    {showLockIcon ? (
                      <FiLock
                        className="ms-2"
                        size="1rem"
                        strokeWidth={2}
                        aria-hidden
                        title={translate('ER_ACTIVITY_LOCKED')}
                      />
                    ) : null}
                    {showLockIcon ? <GatingTooltip /> : null}
                  </LoLink>
                </div>
              </div>
            ) : (
              <GiPartyPopper
                className="text-primary all-done"
                size="8rem"
                aria-hidden
              />
            )}
            <AccessCodeWidget />
            <ResetLearnerDataWidget />
          </div>
        </div>
        {viewingAs.isStudent ? <Tutorial name="course" /> : null}
      </ERLandmark>
    </ERContentContainer>
  );
};

export default ERStudentDashboard;
