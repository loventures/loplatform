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

import { fetchStudents } from '../../api/rosterApi';
import Course from '../../bootstrap/course';
import ERBasicTitle from '../../commonPages/contentPlayer/ERBasicTitle';
import ERContentContainer from '../../landmarks/ERContentContainer';
import { ERLandmark } from '../../landmarks/ERLandmarkProvider';
import ForLearnerLoader from '../../loaders/ForLearnerLoader';
import { selectForLearner } from '../../utilities/rootSelectors';
import { useCourseSelector } from '../../loRedux';
import { useTranslation } from '../../i18n/translationContext';
import React, { useState } from 'react';
import { useHistory } from 'react-router-dom';
import { SingleValue } from 'react-select';
import AsyncSelect from 'react-select/async';

import MetabaseEmbed from './MetabaseEmbed';

const AnalyticsPage: React.FC = () => {
  const translate = useTranslation();

  const [loading, setLoading] = useState(true);
  const history = useHistory();
  const learner = useCourseSelector(selectForLearner);

  const selectLearner = (selectedLearner?: SingleValue<{ value: number }>) => {
    history.push(
      `/instructor/analytics?${
        selectedLearner?.value ? `forLearnerId=${selectedLearner.value}` : ''
      }`
    );
  };

  return (
    <ForLearnerLoader>
      <ERContentContainer title={translate('INSTRUCTOR_ANALYTICS')}>
        <ERLandmark
          landmark="content"
          id="er-instructor-analytics"
          className="p-lg-4"
        >
          <ERBasicTitle label={translate('INSTRUCTOR_ANALYTICS')} />
          <AsyncSelect
            className="pt-3 pb-2 px-4"
            placeholder={translate('SEARCH_BY_NAME')}
            value={learner ? { value: learner.id, label: learner.fullName } : undefined}
            loadOptions={input =>
              fetchStudents(
                input,
                ['givenName', 'familyName', 'userName', 'externalId'],
                'co'
              ).then(learners => {
                setLoading(false);
                return learners.objects.map(learner => ({
                  value: learner.id,
                  label: learner.fullName,
                }));
              })
            }
            onChange={selectLearner}
            isLoading={loading}
            defaultOptions
            cacheOptions
            isClearable
          />
          <MetabaseEmbed
            section={Course.id}
            learner={learner?.id}
          />
        </ERLandmark>
      </ERContentContainer>
    </ForLearnerLoader>
  );
};

export default AnalyticsPage;
