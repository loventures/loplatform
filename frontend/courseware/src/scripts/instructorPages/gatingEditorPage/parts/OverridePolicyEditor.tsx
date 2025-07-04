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
import { updateStudentOverrides } from '../../../api/gatingPoliciesApi';
import ConfirmModal from '../../../components/ConfirmModal';
import { LoCheckbox } from '../../../directives/LoCheckbox';
import { TranslationContext } from '../../../i18n/translationContext';
import { ensureNotNull } from '../../../utils/utils';
import { includes } from 'lodash';
import React, { useContext, useState } from 'react';
import { FormGroup } from 'reactstrap';

import GatingEditorContext from './GatingEditorContext';

const OverridePolicyEditor: React.FC<{ contentId: ContentId }> = ({ contentId }) => {
  const translate = useContext(TranslationContext);
  const [showConfirm, setShowConfirm] = useState(false);
  const { overrides, activeStudent, setGatingData } = ensureNotNull(
    useContext(GatingEditorContext)
  );

  const { perUser = {}, overall = [] } = overrides;
  let label, policyDisabled;
  if (activeStudent) {
    label = translate('GATING_EDITOR_DISABLE_FOR_STUDENT', {
      name: activeStudent.fullName,
    });
    policyDisabled = includes(perUser[activeStudent.id], contentId);
  } else {
    label = translate('GATING_EDITOR_DISABLE_POLICIES');
    policyDisabled = includes(overall, contentId);
  }
  const [checked, setChecked] = useState(policyDisabled);

  return (
    <FormGroup className="override-policy-editor">
      <LoCheckbox
        onToggle={checked => {
          setShowConfirm(true);
          setChecked(checked);
        }}
        state={policyDisabled}
        checkboxFor={`${contentId}-gating-checkbox`}
        checkboxLabel={label}
      />
      <ConfirmModal
        isOpen={showConfirm}
        confirm={isConfirmed => {
          if (!isConfirmed) {
            setShowConfirm(!showConfirm);
            setChecked(!checked);
            return Promise.resolve();
          }

          const userIds = activeStudent ? [activeStudent.id] : [];
          return updateStudentOverrides({
            content: [contentId],
            enabled: !checked,
            userIds,
          }).then(updatedOverrides => {
            setShowConfirm(!showConfirm);
            setGatingData(data => {
              return {
                ...data,
                overrides: updatedOverrides,
              };
            });
          });
        }}
      >
        <p>{translate('GATING_EDITOR_EDIT_CAN_UNDO')}</p>
      </ConfirmModal>
    </FormGroup>
  );
};

export default OverridePolicyEditor;
