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

import ERBanner from '../commonPages/contentPlayer/ERBanner';
import { ActiveDiscussions } from '../instructorPages/dashboard/parts/ActiveDiscussions';
import { GradingQueue } from '../instructorPages/dashboard/parts/GradingQueue';
import { ERLandmark } from '../landmarks/ERLandmarkProvider';
import { useTranslation } from '../i18n/translationContext';
import React from 'react';
import { Col, Row } from 'reactstrap';

import ERContentContainer from '../landmarks/ERContentContainer';
import { qnaEnabled } from '../utilities/preferences';
import UnansweredQna from './dashboard/parts/UnansweredQna';

/**
 * Dashboard component needs lots of love.
 *
 * */
const ERInstructorDashboard: React.FC = () => {
  const translate = useTranslation();

  return (
    <ERContentContainer title={translate('INSTRUCTOR_DASHBOARD')}>
      <ERLandmark
        landmark="content"
        id="er-instructor-dashboard"
      >
        <ERBanner showCircles={false} />
        <div className="container p-3 p-lg-4 p-xl-5">
          {qnaEnabled && (
            <Row className="mb-3">
              <Col>
                <UnansweredQna />
              </Col>
            </Row>
          )}
          <Row>
            <Col
              size={12}
              lg={6}
            >
              <GradingQueue />
            </Col>
            <Col
              size={12}
              lg={6}
              className="pt-3 pt-lg-0"
            >
              <ActiveDiscussions />
            </Col>
          </Row>
        </div>
      </ERLandmark>
    </ERContentContainer>
  );
};

export default ERInstructorDashboard;
