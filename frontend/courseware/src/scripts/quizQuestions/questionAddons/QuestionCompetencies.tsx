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

import { ConnectedRouter } from 'connected-react-router';
import Course from '../../bootstrap/course.ts';
import CompetencyInfoModal from '../../instructorPages/competencyList/components/CompetencyInfoModal.tsx';
import competencyResource, {
  Competency,
  CompetencyWithRelations,
} from '../../resources/CompetencyResource.ts';
import { useCourseSelector } from '../../loRedux';
import { WithTranslateProps, withTranslationFor2Angular } from '../../i18n/translationContext.tsx';
import { history } from '../../utilities/history';
import { react2angularWithNgProvider } from '../../utilities/ngReduxProvider';
import { selectCurrentUser } from '../../utilities/rootSelectors.ts';
import React, { useEffect, useState } from 'react';

type QuestionCompetenciesProps = WithTranslateProps & {
  competencies: Competency[];
};

const QuestionCompetencies: React.FC<QuestionCompetenciesProps> = ({ competencies, translate }) => {
  const [showCompetency, setShowCompetency] = useState<CompetencyWithRelations | undefined>(
    undefined
  );
  const [competenciesWithRelations, setCompetenciesWithRelations] = useState<
    CompetencyWithRelations[]
  >([]);
  const viewingAs = useCourseSelector(selectCurrentUser);
  useEffect(() => {
    competencyResource
      .getFlattenedTree(Course.id, {
        redux: true,
      })
      .then(flat => setCompetenciesWithRelations(flat));
  }, []);
  const competencyIds = new Set(competencies.map(c => c.id));
  const orderedCompetencies = competenciesWithRelations.filter(c => competencyIds.has(c.id));
  return (
    <section className={'alert alert-info question-competencies mb-3 text-dark'}>
      <header className="fw-bold">{translate('QUESTION_COMPETENCIES')}</header>
      <ul className="mb-0 list-unstyled">
        {orderedCompetencies.map(competency => {
          const activities = competency.relations.filter(r => !r.isForCredit).length ?? 0;
          return (
            <li
              key={competency.id}
              className="mt-2"
            >
              <span>{competency.title}</span>{' '}
              {activities ? (
                <button
                  className="ms-1 btn-link p-0 border-0 bg-transparent review-now"
                  onClick={() => setShowCompetency(competency)}
                >
                  {translate('QUESTION_COMPETENCIES_REVIEW_ACTIVITIES', {
                    activities,
                  })}
                </button>
              ) : null}
            </li>
          );
        })}
      </ul>
      {showCompetency && (
        <ConnectedRouter history={history}>
          <CompetencyInfoModal
            competency={showCompetency}
            viewingAs={viewingAs}
            initialFilter="ACTIVITIES_ONLY"
            toggleModal={() => setShowCompetency(undefined)}
          />
        </ConnectedRouter>
      )}
    </section>
  );
};

export default QuestionCompetencies;

export const questionCompetenciesComponent = angular
  .module('lo.questions.addons.questionCompetencies', [])
  .component(
    'questionCompetencies',
    react2angularWithNgProvider(
      withTranslationFor2Angular(QuestionCompetencies),
      ['competencies'],
      []
    )
  );
