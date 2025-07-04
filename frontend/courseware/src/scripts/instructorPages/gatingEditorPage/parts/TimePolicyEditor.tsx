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

import { ContentId } from '../../../api/contentsApi';
import { customizeOneContent } from '../../../api/customizationApi';
import ConfirmModal from '../../../components/ConfirmModal';
import DateEditor from '../../../components/dateTime/DateEditor';
import DateTimeCancelButton from '../../../components/dateTime/DateTimeCancelButton';
import {
  fromInputToISO,
  isValidDateFromInput,
  isValidTimeFromInput,
} from '../../../components/dateTime/dateTimeInputUtils';
import DateTimeSaveButton from '../../../components/dateTime/DateTimeSaveButton';
import DateTimeTrashButton from '../../../components/dateTime/DateTimeTrashButton';
import TimeEditor from '../../../components/dateTime/TimeEditor';
import { TranslationContext } from '../../../i18n/translationContext';
import { courseReduxStore } from '../../../loRedux';
import { contentOverlayUpdateReplace } from '../../../loRedux/contentOverlayUpdateReducer';
import { ensureNotNull } from '../../../utils/utils';
import { Form } from 'reactstrap';
import React, { useContext, useRef, useState } from 'react';

import GatingEditorContext from './GatingEditorContext';
import { lojector } from '../../../loject';

const TimePolicyEditor: React.FC<{
  contentId: ContentId;
  policyDate?: string;
  policyTime?: string;
  disabled?: boolean;
}> = ({ contentId, policyDate, policyTime, disabled = false }) => {
  const translate = useContext(TranslationContext);
  const [showConfirm, setShowConfirm] = useState(false);
  const [isClearAction, setClearAction] = useState(false);
  const { customizations, setCustomisations } = ensureNotNull(useContext(GatingEditorContext));

  const initialValue = useRef({ editorDate: policyDate, editorTime: policyTime });
  const [values, setValues] = useState(initialValue.current);
  const [isSubmitting, setSubmitting] = useState(false);
  const dirty = values !== initialValue.current;
  const isDisabled = disabled || isSubmitting;

  const isValid =
    values.editorDate &&
    isValidDateFromInput(values.editorDate) &&
    values.editorTime &&
    isValidTimeFromInput(values.editorTime);

  return (
    <div>
      <div>
        <label>{translate('GATING_POLICY_TIME')}</label>
        <Form
          className="form-inline temporal-policy-editor d-flex"
          onSubmit={e => {
            e.preventDefault();
            setShowConfirm(true);
          }}
        >
          <DateEditor
            value={values.editorDate}
            onChange={e => setValues({ ...values, editorDate: e.target.value })}
            disabled={isDisabled}
          />
          <span className="mx-1" />
          <TimeEditor
            value={values.editorTime}
            onChange={e => setValues({ ...values, editorTime: e.target.value })}
            disabled={isDisabled}
          />
          {dirty && isValid && (
            <DateTimeSaveButton
              className="ms-1"
              type="submit"
              disabled={isDisabled}
            />
          )}
          {dirty && (
            <DateTimeCancelButton
              onClick={() => {
                setValues(initialValue.current);
              }}
              className="ms-1"
              disabled={isDisabled}
            />
          )}
          {policyDate && policyTime && (
            <DateTimeTrashButton
              className="ms-1"
              type="button"
              disabled={isDisabled}
              onClick={() => {
                setClearAction(true);
                setShowConfirm(true);
                setSubmitting(true);
              }}
            />
          )}
          <ConfirmModal
            isOpen={showConfirm}
            confirm={isConfirmed => {
              if (!isConfirmed) {
                setShowConfirm(!showConfirm);
                setSubmitting(false);
                setClearAction(false);
                return Promise.resolve();
              }

              const gateDate = isClearAction
                ? null
                : fromInputToISO(values.editorDate, values.editorTime);

              return customizeOneContent(contentId, { gateDate })
                .catch(e => {
                  setSubmitting(false);
                  throw e;
                })
                .then(customization => {
                  if (isClearAction) {
                    setValues((initialValue.current = { editorTime: '', editorDate: '' }));
                  } else {
                    initialValue.current = values;
                  }
                  setClearAction(false);
                  setShowConfirm(false);
                  setSubmitting(false);
                  setCustomisations({
                    ...customizations,
                    [contentId]: customization,
                  });
                  courseReduxStore.dispatch(
                    contentOverlayUpdateReplace({
                      [contentId]: customization,
                    })
                  );

                  //this is still needed for content view which uses custom modified subslices of api data
                  const payload = lojector
                    .get<any>('GatingActions')
                    .temporalPolicyUpdatedAC(contentId, customization.gateDate);
                  courseReduxStore.dispatch(payload);
                });
            }}
          >
            <p>{translate('GATING_TIME_POLICY_EDIT_CAN_UNDO')}</p>
          </ConfirmModal>
        </Form>
      </div>
      {values.editorDate && !isValidDateFromInput(values.editorDate) && (
        <div className="invalid-feedback d-block">{translate('DATE_PICKER_INVALID_DATE')}</div>
      )}
      {values.editorTime && !isValidTimeFromInput(values.editorTime) && (
        <div className="invalid-feedback d-block">{translate('DATE_PICKER_INVALID_TIME')}</div>
      )}
    </div>
  );
};

export default TimePolicyEditor;
