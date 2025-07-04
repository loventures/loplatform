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
import CompetencyInfoModal from '../../../instructorPages/competencyList/components/CompetencyInfoModal';
import { CompetencyWithRelations } from '../../../resources/CompetencyResource';
import { useMasteryStatus } from '../../../resources/MasteryResource';
import { countBy } from 'lodash';
import { useTranslation } from '../../../i18n/translationContext';
import { isMasteryEnabled } from '../../../utilities/preferences';
import { UserWithRoleInfo } from '../../../utilities/rootSelectors';
import React, { useMemo, useState } from 'react';

// It is an accessibility travesty that this tree is flattened, but fixing that is beyond me.

const ERCompetencyNode: React.FC<{
  competency: CompetencyWithRelations;
  viewingAs: UserWithRoleInfo;
}> = ({ competency, viewingAs }) => {
  const [modalOpen, setModalOpen] = useState<'ACTIVITIES_ONLY' | 'ASSIGNMENTS_ONLY' | undefined>(
    undefined
  );
  const translate = useTranslation();
  const currentUserMastery = useMasteryStatus(viewingAs.id);
  const mastered = isMasteryEnabled && currentUserMastery.includes(competency.id);
  const relationCounts = useMemo(() => {
    const contentCounts = countBy(competency.relations, c => {
      return c.isForCredit ? 'assignments' : 'activities';
    });
    return {
      all: competency.relations.length ?? 0,
      activities: contentCounts.activities ?? 0,
      assignments: contentCounts.assignments ?? 0,
    };
  }, [competency]);

  return (
    <li
      className={classnames(
        `depth-${competency.level - 1}`,
        competency.level > 1 ? 'child-competency' : 'root-competency'
      )}
    >
      <div
        className={classnames('competency-item', {
          disabled: !competency.relations.length && !competency.hasChildren,
        })}
      >
        <div className="flex-row-content align-items-start">
          <div className="flex-col-fluid competency-name">{competency.title}</div>
          {mastered && (
            <span className="competency-mastery-status mastered">
              {translate('COMPETENCY_MASTERY_MASTERED')}
            </span>
          )}
        </div>

        {competency.relations.length > 0 && (
          <div className="competency-relations">
            <span className="text-muted">
              {translate('COMPETENCY_LINKED_COUNT_PREFIX', relationCounts, 'messageformat')}
              &nbsp;
            </span>
            <button
              className="btn-link p-0 border-0 bg-transparent"
              onClick={() => setModalOpen('ACTIVITIES_ONLY')}
              disabled={!relationCounts.activities}
            >
              {translate('COMPETENCY_ACTIVITY_COUNT', relationCounts, 'messageformat')}
            </button>
            <span className="text-muted">
              &nbsp;{translate('COMPETENCY_LINKED_COUNT_AND')}&nbsp;
            </span>
            <button
              className="btn-link p-0 border-0 bg-transparent"
              onClick={() => setModalOpen('ASSIGNMENTS_ONLY')}
              disabled={!relationCounts.assignments}
            >
              {translate('COMPETENCY_ASSIGNMENT_COUNT', relationCounts, 'messageformat')}
            </button>
            <span className="text-muted">&nbsp;{translate('COMPETENCY_LINKED_COUNT_SUFFIX')}</span>
          </div>
        )}
      </div>
      {modalOpen && (
        <CompetencyInfoModal
          competency={competency}
          viewingAs={viewingAs}
          initialFilter={modalOpen}
          toggleModal={() => setModalOpen(undefined)}
        />
      )}
    </li>
  );
};

export default ERCompetencyNode;
