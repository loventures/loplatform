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

import ERNonContentTitle from '../../commonPages/contentPlayer/ERNonContentTitle';
import ERCompetencyNode from '../../instructorPages/competencyList/components/ERCompetencyNode';
import ERContentContainer from '../../landmarks/ERContentContainer';
import { useFlattenedCompetencyTree } from '../../resources/CompetencyResource';
import { useCourseSelector } from '../../loRedux';
import { map } from 'lodash';
import { useTranslation } from '../../i18n/translationContext';
import { selectCurrentUser } from '../../utilities/rootSelectors';
import React from 'react';

const ERInstructorCourseCompetenciesPage: React.FC = () => {
  const translate = useTranslation();
  const viewingAs = useCourseSelector(selectCurrentUser);
  const flattenedTree = useFlattenedCompetencyTree();

  return (
    <ERContentContainer title={translate('COMPETENCIES_PAGE_TITLE')}>
      <div className="container p-0">
        <div className="card er-content-wrapper mb-2 m-md-3 m-lg-4">
          <div className="card-body">
            <ERNonContentTitle label={translate('COMPETENCIES_PAGE_TITLE')} />
            <ul className="competency-list-nested px-md-4 pt-md-2">
              {map(flattenedTree, cs => (
                <ERCompetencyNode
                  key={cs.id}
                  competency={cs}
                  viewingAs={viewingAs}
                />
              ))}
            </ul>
          </div>
        </div>
      </div>
    </ERContentContainer>
  );
};

export default ERInstructorCourseCompetenciesPage;
