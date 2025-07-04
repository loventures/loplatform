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

import { map } from 'lodash';
import { Translate, withTranslation } from '../../../../i18n/translationContext.tsx';
import naturalCompare from 'natural-compare';
import React from 'react';

const SubmissionCompetencies: React.FC<{
  translate: Translate;
  competencies: any[];
}> = ({ translate, competencies }) =>
  !competencies.length ? null : (
    <div
      className="activity-competencies mt-4 mb-2 mb-sm-3 alert alert-info feedback-context"
      data-id="alignment"
    >
      <div className="activity-competencies-title mb-1">{translate('COMPETENCIES_ASSESSED')}</div>
      {map(
        competencies.sort((c0, c1) => naturalCompare(c0.title, c1.title)),
        (competency, index) => (
          <div
            className="capability-item"
            key={index}
          >
            {competency.title}
          </div>
        )
      )}
    </div>
  );

export default withTranslation(SubmissionCompetencies);
