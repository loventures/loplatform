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

import { Question } from '../../api/quizApi.ts';
import Course from '../../bootstrap/course.ts';
import CompetencyInfoModal from '../../instructorPages/competencyList/components/CompetencyInfoModal.tsx';
import competencyResource, { CompetencyWithRelations } from '../../resources/CompetencyResource.ts';
import { useCourseSelector } from '../../loRedux';
import { partition } from 'lodash';
import { useTranslation } from '../../i18n/translationContext.tsx';
import { selectCurrentUser } from '../../utilities/rootSelectors.ts';
import React, { useEffect, useState } from 'react';

const CompetencyPerformanceSummary: React.FC<{
  quiz: any;
  viewAttempt: (attempt: any) => void;
}> = ({ quiz }) => {
  const [showCompetency, setShowCompetency] = useState<CompetencyWithRelations | undefined>(
    undefined
  );
  const [competenciesWithRelations, setCompetenciesWithRelations] = useState<
    CompetencyWithRelations[]
  >([]);
  useEffect(() => {
    competencyResource
      .getFlattenedTree(Course.id, {
        redux: true,
      })
      .then(flat => setCompetenciesWithRelations(flat));
  }, []);

  const viewingAs = useCourseSelector(selectCurrentUser);
  const translate = useTranslation();

  const usedCompetencyIds = new Set(
    quiz.latestSubmittedAttempt.questions.flatMap((q: Question) => q.competencies.map(c => c.id))
  );
  const competencies = competenciesWithRelations.filter(c => usedCompetencyIds.has(c.id));
  const linkedAttemptIsComplete: boolean = quiz.isLatestSubmittedAttemptFinalized;
  const competencyPerformance: Set<number> | undefined = quiz.latestAttemptCompetencyBreakdown;
  const [above, below] = partition(competencies, competency =>
    competencyPerformance?.has(competency.id)
  );

  return (
    <>
      <h2 className="h4">{translate('COMPETENCY_SUMMARY_TITLE')}</h2>

      {!linkedAttemptIsComplete ? (
        <p className="alert alert-primary">
          {translate('COMPETENCY_SUMMARY_NOT_READY_DESCRIPTION_UNLINKED')}{' '}
          {translate('COMPETENCY_SUMMARY_NOT_READY_DESCRIPTION_AFTER_LINK')}
        </p>
      ) : (
        <p className="mt-3">
          {translate('COMPETENCY_SUMMARY_DESCRIPTION_UNLINKED')}{' '}
          {translate('COMPETENCY_SUMMARY_DESCRIPTION_AFTER_LINK')}
        </p>
      )}

      {above.length > 0 && (
        <div className="mt-3 competency-performance-summary above">
          <h3 className="h5 mb-3">{translate('COMPETENCIES_WITH_GOOD_PERFORMANCE')}</h3>
          <div className={'alert alert-success mb-0 text-dark'}>
            <p>{translate('COMPETENCIES_WITH_GOOD_PERFORMANCE_DESCRIPTION')}</p>
            <ul className="mb-0 list-unstyled">
              {above.map(competency => (
                <li
                  key={competency.id}
                  className="ms-3 mb-2"
                >
                  {competency.title}
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}

      {below.length > 0 && (
        <div className="mt-3 competency-performance-summary below">
          <h3 className="h5 mb-3">{translate('COMPETENCIES_WITH_BAD_PERFORMANCE')}</h3>
          <div className="alert alert-danger mb-0 text-dark">
            <p>{translate('COMPETENCIES_WITH_BAD_PERFORMANCE_DESCRIPTION')}</p>
            <ul className="mb-0 list-unstyled">
              {below.map(competency => {
                const activities = competency.relations.filter(r => !r.isForCredit).length ?? 0;
                return (
                  <li
                    key={competency.id}
                    className="ms-3 mb-2"
                  >
                    {competency.title}{' '}
                    {activities ? (
                      <button
                        className="ms-1 btn-link p-0 border-0 bg-transparent review-now"
                        onClick={() => setShowCompetency(competency)}
                      >
                        {translate('QUESTION_COMPETENCIES_REVIEW_ACTIVITIES', { activities })}
                      </button>
                    ) : null}
                  </li>
                );
              })}
            </ul>
          </div>
        </div>
      )}

      {showCompetency && (
        <CompetencyInfoModal
          competency={showCompetency}
          viewingAs={viewingAs}
          initialFilter="ACTIVITIES_ONLY"
          toggleModal={() => setShowCompetency(undefined)}
        />
      )}
    </>
  );
};

export default CompetencyPerformanceSummary;
