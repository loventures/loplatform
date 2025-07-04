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

import axios from 'axios';
import { searchStudentByName } from '../../api/rosterApi';
import Course from '../../bootstrap/course';
import { CourseState } from '../../loRedux';
import { map } from 'lodash';
import { withTranslation } from '../../i18n/translationContext';
import React from 'react';
import AsyncSelect from 'react-select/async';
import { Button } from 'reactstrap';
import { withHandlers, withState } from 'recompose';
import { compose } from 'redux';

import { ConnectedLoader } from '../assignments/ConnectedLoader';
import { ExemptLearner, fetchDueDateExemptionsAction } from './dueDateAccommodationsReducer';

type ExemptLearnerFormProps = {
  exemptLearners: ExemptLearner[];
};

const ExemptLearnerFormInner = ({
  translate,
  selectedLearners,
  onChange,
  updateDueDateAccommodations,
  dirty,
  saving,
}: any) => (
  <React.Fragment>
    <AsyncSelect<ExemptLearner>
      // @ts-ignore https://github.com/JedWatson/react-select/issues/5522
      isMulti
      classNamePrefix="react-select"
      id="exempt-learner-select"
      cacheOptions
      defaultOptions
      getOptionValue={learner => learner.id.toString()}
      getOptionLabel={learner => learner.givenName + ' ' + learner.familyName}
      loadOptions={input => searchStudentByName(input).then(({ objects }) => objects)}
      value={selectedLearners}
      onChange={onChange}
      openMenuOnClick={false}
    />
    <div className="d-flex justify-content-end mt-2">
      <Button
        id="update-due-date-accomodations-btn"
        color="primary"
        disabled={saving || !dirty}
        onClick={updateDueDateAccommodations}
      >
        {translate('SAVE')}
      </Button>
    </div>
  </React.Fragment>
);

const ExemptLearnerForm = compose<React.ComponentType<ExemptLearnerFormProps>>(
  withTranslation,
  withState('saving', 'setSaving', false),
  withState('dirty', 'setDirty', false),
  withState('selectedLearners', 'setSelectedLearners', ({ exemptLearners }) => exemptLearners),
  withHandlers<any, any>({
    onChange:
      ({ setSelectedLearners, setDirty }: any) =>
      (learners: any) => {
        setSelectedLearners(learners);
        setDirty(true);
      },
    updateDueDateAccommodations:
      ({ setSaving, setDirty, selectedLearners }: any) =>
      () => {
        // loConfig.instructorCustomization.dueDateAccommodation
        const url = `/api/v2/contentConfig/dueDateAccommodation;context=${Course.id}`;
        setSaving(true);
        axios
          .post(url, {
            exemptLearners: map(selectedLearners, 'id'),
          })
          .then(() => {
            setSaving(false);
            setDirty(false);
          });
      },
  })
)(ExemptLearnerFormInner);

const LoadedExemptLearnerForm = () => (
  <ConnectedLoader
    onMount={(_state, dispatch) => dispatch(fetchDueDateExemptionsAction())}
    selector={(state: CourseState) => state.api.dueDateExemptions}
  >
    {([learners]) => <ExemptLearnerForm exemptLearners={learners} />}
  </ConnectedLoader>
);

export default LoadedExemptLearnerForm;
