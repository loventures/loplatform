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
import Course from '../../bootstrap/course';
import { withTranslation } from '../../i18n/translationContext';
import React from 'react';
import { Button, Card, CardBody, CardHeader, FormGroup, Input, Label } from 'reactstrap';
import { withHandlers, withState } from 'recompose';
import { compose } from 'redux';

const DueDatePolicies = ({
  translate,
  enforceDueDate,
  saving,
  dirty,
  updateForm,
  updateDueDatePolicies,
}: any) => (
  <Card>
    <CardHeader>{translate('DUE_DATE_POLICY')}</CardHeader>
    <CardBody>
      <FormGroup check>
        <Label check>
          <Input
            type="radio"
            id="prevent-late-submission"
            name="enforce-due-date"
            value="true"
            checked={enforceDueDate === true}
            onChange={updateForm}
          />
          {translate('DUE_DATE_PREVENT_SUBMISSION')}
        </Label>
      </FormGroup>
      <FormGroup check>
        <Label check>
          <Input
            type="radio"
            id="allow-late-submission"
            name="enforce-due-date"
            value="false"
            checked={enforceDueDate === false}
            onChange={updateForm}
          />
          {translate('DUE_DATE_ALLOW_SUBMISSION')}
        </Label>
      </FormGroup>
      <div className="d-flex justify-content-end mt-2">
        <Button
          id="update-due-date-policies-btn"
          color="primary"
          disabled={saving || !dirty}
          onClick={updateDueDatePolicies}
        >
          {translate('SAVE')}
        </Button>
      </div>
    </CardBody>
  </Card>
);

export default compose<React.ComponentType>(
  withState('saving', 'setSaving', false),
  withState('dirty', 'setDirty', false),
  withState('enforceDueDate', 'setEnforceDueDate', () => {
    return window.lo_platform.preferences.strictDueDate;
  }),
  withHandlers({
    updateForm: (props: any) => (event: any) => {
      const value = event.target.value === 'true';
      props.setEnforceDueDate(value);
      props.setDirty(true);
    },
    updateDueDatePolicies: (props: any) => () => {
      // loConfig.instructorCustomizations.dueDate
      const url = `/api/v2/contentConfig/dueDate;context=${Course.id}`;
      props.setSaving(true);
      axios
        .post(url, {
          strictDueDate: props.enforceDueDate,
        })
        .then(() => {
          window.lo_platform.preferences.strictDueDate = props.enforceDueDate;
          props.setSaving(false);
          props.setDirty(false);
        });
    },
  }),
  withTranslation
)(DueDatePolicies);
