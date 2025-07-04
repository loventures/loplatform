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

import { Content } from '../../api/contentsApi.ts';
import { useLearningPathResource } from '../../resources/LearningPathResource.ts';
import { useCourseSelector } from '../../loRedux';
import { isEmpty, kebabCase, minBy } from 'lodash';
import { ContentWithNebulousDetails } from '../../courseContentModule/selectors/contentEntry';
import { percentFilter } from '../../filters/percent';
import { useTranslation } from '../../i18n/translationContext.tsx';
import GradebookColumnsLoader from '../../loaders/GradebookColumnsLoader';
import { selectGradebookColumns } from '../../selectors/gradeSelectors.ts';
import React from 'react';

interface ActivityInfoProps {
  content: ContentWithNebulousDetails;
  gradeCalculationType?: string;
  maximumAttempts: number | null;
  attemptNumber?: number;
  maxMinutes?: number | null;
  testsOut?: Record<string, number>;
}

const ActivityInfo: React.FC<ActivityInfoProps> = ({
  content,
  gradeCalculationType,
  maximumAttempts,
  attemptNumber,
  maxMinutes,
  testsOut,
}) => {
  const translate = useTranslation();
  return gradeCalculationType || maxMinutes ? (
    <div className="assignment-info mx-3">
      <div className="card">
        <div className="card-body">
          <ul className="mb-0 ps-3">
            {gradeCalculationType && (
              <li className={`grade-calculation ${kebabCase(gradeCalculationType)}`}>
                {translate(
                  attemptNumber == null || maximumAttempts === 1
                    ? 'ASSIGNMENT_MAXIMUM_ATTEMPTS'
                    : 'ASSIGNMENT_NTH_OF_MAXIMUM_ATTEMPTS',
                  {
                    number: attemptNumber,
                    attempts: maximumAttempts || 0,
                  }
                )}
                <ul className="ps-3">
                  <li>
                    {translate(
                      maximumAttempts === 1
                        ? 'ASSIGNMENT_GRADE_CALCULATION_SINGLE_ATTEMPT'
                        : 'ASSIGNMENT_GRADE_CALCULATION_' + gradeCalculationType
                    )}
                  </li>
                  <GradebookColumnsLoader LoadingMessage={LoadingLI}>
                    <GradeLI content={content} />
                  </GradebookColumnsLoader>
                </ul>
              </li>
            )}
            {!!maxMinutes && (
              <li className="time-limit">
                {translate('ASSIGNMENT_TIME_LIMIT', { maxMinutes })}
                <ul className="ps-3">
                  <li>{translate('ASSIGNMENT_TIMING_AUTOSUBMIT')}</li>
                  <li>{translate('ASSIGNMENT_TIMING_ACCOMMODATION')}</li>
                </ul>
              </li>
            )}
            {!isEmpty(testsOut) && <TestsOutLI testsOut={testsOut} />}
          </ul>
        </div>
      </div>
    </div>
  ) : null;
};

const LoadingLI: React.FC = () => {
  const translate = useTranslation();
  return <li className="text-muted">{translate('LOADING_SPINNER_TEXT')}</li>;
};

const GradeLI: React.FC<{ content: ContentWithNebulousDetails }> = ({ content }) => {
  const gradebookColumns = useCourseSelector(selectGradebookColumns);
  const gradebookColumn = gradebookColumns[content.id];
  const translate = useTranslation();
  return gradebookColumn ? (
    <li>
      {translate(
        content?.isForCredit ? 'ASSIGNMENT_INFO_FOR_CREDIT' : 'ASSIGNMENT_INFO_NOT_FOR_CREDIT',
        {
          percent: percentFilter(gradebookColumn.weight, 1),
        }
      )}
    </li>
  ) : null;
};

const TestsOutLI: React.FC<{ testsOut: Record<string, number> }> = ({ testsOut }) => {
  const translate = useTranslation();
  const contents = new Array<[Content, number]>();
  const add = (content: Content) => {
    const testOut = testsOut[content.id];
    if (testOut != null) contents.push([content, testOut]);
  };
  const learningPath = useLearningPathResource();
  for (const module of learningPath.modules) {
    add(module.content);
    module.elements.forEach(add);
  }
  const min = minBy(contents, t => t[1])?.[1];
  return min ? (
    <li>
      {translate('ASSIGNMENT_INFO_TEST_OUT', { percent: percentFilter(min, 0) })}
      <ul className="ps-3">
        <li>
          {contents
            .map(
              ([content, grade]) =>
                `${content.name}${grade === min ? '' : ` (${percentFilter(grade, 0)})`}`
            )
            .join(', ')}
        </li>
      </ul>
    </li>
  ) : null;
};

export default ActivityInfo;
