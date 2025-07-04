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

import { CONTENT_TYPE_CHECKPOINT } from '../../utilities/contentTypes.js';
import { lojector } from '../../loject.js';

const createRightsGatingPolicyMessage = (translate, policy) =>
  translate('RIGHTS_BASED_LOCK_MESSAGE_' + policy.policyType);

const createTemporalGatingPolicyMessage = (translate, policy, name, historic) =>
  translate(historic ? 'CONTENT_TIME_LOCK_BEFORE_HISTORIC' : 'CONTENT_TIME_LOCK_BEFORE', {
    name,
    time: lojector.get('formatDayjsFilter')(policy.lockDate, 'full'),
  });

const createActivityGatingPolicyMessage = (translate, policy, name) =>
  translate(
    policy.typeId === CONTENT_TYPE_CHECKPOINT
      ? 'LOCKING_STATUS_MESSAGE_CHECKPOINT'
      : 'LOCKING_STATUS_MESSAGE_GRADE',
    {
      name,
      grade: lojector.get('gradeFilter')(policy.threshold, 'percentSign'),
      assignments: policy.name,
    }
  );

const isHistoricDate = lockDate => new Date(lockDate).getTime() < new Date().getTime();

//Returns all gating messages in order: Temporal -> Activities -> Rights
const createMessagesForGate = (translate, gate, content) => {
  let messages = [];

  //Rights based gates currently only show for those with special rights (i.e. Trial Learners). Not Instructors.
  //If a rights gate is present, we only show this gate and no others.
  if (gate.rightsGatingPolicy) {
    messages.push(
      createRightsGatingPolicyMessage(translate, gate.rightsGatingPolicy, content.name)
    );
  } else {
    if (gate.temporalGatingPolicy) {
      messages.push(
        createTemporalGatingPolicyMessage(
          translate,
          gate.temporalGatingPolicy,
          content.name,
          isHistoricDate(gate.temporalGatingPolicy.lockDate)
        )
      );
    }

    if (gate.activityGatingPolicy && gate.activityGatingPolicy.gates.length > 0) {
      const gates = gate.activityGatingPolicy.gates;
      messages = messages.concat(
        map(gates, gate => {
          return createActivityGatingPolicyMessage(translate, gate, content.name);
        })
      );
    }
  }

  return messages;
};

export const createMessagesForContent = (translate, content, viewingAs) =>
  map(content.availability.allGates, gate =>
    createMessagesForGate(translate, gate, content, viewingAs)
  );
