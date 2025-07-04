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

import { selectLearnerAssignmentsPageHeaderComponent } from '../../../commonPages/assignmentsPage/selectors/pageSelectors';
import { useCourseSelector } from '../../../loRedux';
import GradeBadge from '../../../directives/GradeBadge';
import { useTranslation } from '../../../i18n/translationContext';
import React from 'react';
import { Button, UncontrolledTooltip } from 'reactstrap';
import { lojector } from '../../../loject';
import ERNonContentTitle from '../../contentPlayer/ERNonContentTitle.tsx';

type ERLearnerAssignmentsPageHeader = {
  showName?: boolean;
};

const ERLearnerAssignmentsPageHeader: React.FC<ERLearnerAssignmentsPageHeader> = ({
  showName = false,
}) => {
  const translate = useTranslation();
  const { viewingAs, overallGrade, course } = useCourseSelector(
    selectLearnerAssignmentsPageHeaderComponent
  );

  return (
    <>
      <ERNonContentTitle
        label={translate(showName ? 'ER_STUDENT_GRADEBOOK_TITLE' : 'ER_SCORES_TITLE', {
          name: viewingAs.fullName,
        })}
      />
      <div className="d-flex justify-content-between align-items-end aslist-header">
        <div className="d-none d-md-block"></div>
        <Button
          className="align-self-center"
          color="primary"
          target="_blank"
          id="StudentGradebookDownload"
          href={(lojector.get('GradebookAPI') as any).downloadStudentGrades(
            course.id,
            viewingAs.id
          )}
        >
          <span>{translate('DOWNLOAD_STUDENT_GRADEBOOK')}</span>
        </Button>
        <UncontrolledTooltip
          placement="bottom"
          target="StudentGradebookDownload"
        >
          {translate('DOWNLOAD_STUDENT_GRADEBOOK_DETAIL')}
        </UncontrolledTooltip>
        {overallGrade?.grade == null ? (
          <div></div>
        ) : (
          <div
            className="pull-right d-flex justify-content-end"
            style={{ width: 0 }}
          >
            <div className="d-flex align-items-baseline">
              <div>{translate('STUDENT_ASSIGNMENT_OVERALL')}</div>
              <span className="h4 mb-0 ms-2">
                <GradeBadge
                  grade={overallGrade}
                  percent="half"
                />
              </span>
            </div>
          </div>
        )}
      </div>
    </>
  );
};

export default ERLearnerAssignmentsPageHeader;
