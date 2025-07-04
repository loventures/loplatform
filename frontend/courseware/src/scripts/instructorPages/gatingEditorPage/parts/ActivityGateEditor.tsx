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

import { Content } from '../../../api/contentsApi';
import { updateActivityGateOverrides } from '../../../api/gatingPoliciesApi';
import ConfirmModal from '../../../components/ConfirmModal';
import { TranslationContext } from '../../../i18n/translationContext';
import { courseReduxStore } from '../../../loRedux';
import { ensureNotNull } from '../../../utils/utils';
import { find, isEmpty, map } from 'lodash';
import { percentFilter } from '../../../filters/percent';
import React, { useContext, useState } from 'react';

import { getActiveActivityGates } from './GatingEditor';
import GatingEditorContext from './GatingEditorContext';
import { lojector } from '../../../loject';

const ActivityGateEditor: React.FC<{ content: Content; disabled: boolean }> = ({
  content,
  disabled,
}) => {
  const translate = useContext(TranslationContext);
  const { overrides, contents, setGatingData } = ensureNotNull(useContext(GatingEditorContext));
  const [showConfirm, setShowConfirm] = useState(false);
  const [assignmentId, setAssignmentId] = useState<string | null>(null);

  const activeGates = getActiveActivityGates(content, overrides);
  const activities = map(activeGates, gate => {
    const content = find(contents, { id: gate.assignmentId })!;
    return {
      ...gate,
      name: content.name,
    };
  });

  return (
    <>
      {!isEmpty(activities) && (
        <>
          <div className="activity-policy-editor">
            <div className="policy-editor-title">{translate('GATING_POLICY_GRADEBOOK')}</div>
            <ul className="gradebook-policies">
              {map(activities, activity => (
                <li key={activity.assignmentId}>
                  <div className="one-gradebook-policy flex-row-content">
                    <span className="assignment-name">{activity.name}</span>

                    <span className="threshold">
                      {translate('GATING_POLICY_GRADEBOOK_THRESHOLD', {
                        threshold: percentFilter(activity.threshold),
                      })}
                    </span>

                    <button
                      className="remove-button icon-btn icon-btn-danger"
                      disabled={disabled}
                      onClick={() => {
                        setAssignmentId(activity.assignmentId);
                        setShowConfirm(true);
                      }}
                      title={translate('GATING_POLICY_GRADEBOOK_REMOVE_POLICY')}
                    >
                      <span className="icon icon-trash" />
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </div>

          <ConfirmModal
            isOpen={showConfirm}
            confirm={isConfirmed => {
              if (!isConfirmed) {
                setAssignmentId(null);
                setShowConfirm(!showConfirm);
                return Promise.resolve();
              }
              const assignments = assignmentId ? [assignmentId] : [];
              return updateActivityGateOverrides({
                content: content.id,
                enabled: false,
                assignments,
              }).then(updatedOverrides => {
                setAssignmentId(null);
                setShowConfirm(!showConfirm);
                setGatingData(data => {
                  return {
                    ...data,
                    overrides: updatedOverrides,
                  };
                });

                //this is still needed for content view which uses custom modified subslices of api data
                const payload = lojector
                  .get<any>('GatingActions')
                  .activityPolicyRemovedAC(content.id, assignmentId);
                courseReduxStore.dispatch(payload);
              });
            }}
          >
            <p>{translate('GATING_GRADEBOOK_POLICY_EDIT_CANNOT_UNDO')}</p>
          </ConfirmModal>
        </>
      )}
    </>
  );
};

export default ActivityGateEditor;
