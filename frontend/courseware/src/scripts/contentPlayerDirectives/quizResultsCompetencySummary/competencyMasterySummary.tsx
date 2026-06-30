/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import * as React from 'react';
import { groupBy } from 'lodash';
import { useTranslation } from '../../i18n/translationContext.tsx';
import { naturallyOrderBy } from '../../filters/pure/naturallyOrderBy.ts';

type Competency = { id?: unknown; title?: string };

export type CompetencyMasterySummaryProps = {
  competencies?: Competency[];
  quiz?: {
    isLatestSubmittedAttemptFinalized?: boolean;
    latestAttemptCompetencyBreakdown?: { has(id: unknown): boolean };
  };
};

/**
 * The diagnostic proficiency summary — groups a quiz's competencies into
 * demonstrated ("mastered") and upcoming ("remaining") skills. Fully migrated
 * off AngularJS to a plain React component: its only consumer
 * (DiagnosticMasterySection) was already React via angular2react, and the
 * `<competency-mastery-summary>` Angular element was unused, so the Angular
 * component + angular2react bridge are removed entirely. DOM/classes preserved.
 */
export const CompetencyMasterySummary = ({ competencies, quiz }: CompetencyMasterySummaryProps) => {
  const translate = useTranslation();

  const linkedAttemptIsComplete = quiz?.isLatestSubmittedAttemptFinalized;
  const competencyPerformance = quiz?.latestAttemptCompetencyBreakdown;
  const competencySplit = groupBy(competencies, prof =>
    competencyPerformance?.has(prof.id) ? 'mastered' : 'remaining'
  );
  const mastered = competencySplit.mastered ?? [];
  const remaining = competencySplit.remaining ?? [];

  return (
    <>
      {!linkedAttemptIsComplete && (
        <div className="mt-3 alert alert-primary h4">
          <i role="presentation" className="material-icons">
            pending_actions
          </i>
          <span>{translate('QUIZ_RESULTS_COMPETENCIES_PENDING_GRADING')}</span>
        </div>
      )}

      <div className="competency-summary-page">
        {mastered.length > 0 && (
          <div className="mt-3">
            <h2 className="h5 mb-3">{translate('DEMONSTRATED_SKILLS')}</h2>
            <div className="alert alert-success mb-0">
              <p>{translate('DEMONSTRATED_SKILLS_DESCRIPTION')}</p>
              <ul className="mb-0 list-unstyled">
                {naturallyOrderBy(mastered, 'title').map((competency, i) => (
                  <li className="ms-3 mb-2" key={(competency.id as React.Key) ?? i}>
                    <span>{competency.title}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        )}

        {remaining.length > 0 && (
          <div className="mt-3">
            <h2 className="h5">{translate('UPCOMING_SKILLS')}</h2>
            <div className="alert alert-dark mb-0">
              <p>{translate('UPCOMING_SKILLS_DESCRIPTION')}</p>
              <ul className="mb-0 list-unstyled">
                {naturallyOrderBy(remaining, 'title').map((competency, i) => (
                  <li className="ms-3 mb-2" key={(competency.id as React.Key) ?? i}>
                    <span>{competency.title}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        )}
      </div>
    </>
  );
};

export default CompetencyMasterySummary;
