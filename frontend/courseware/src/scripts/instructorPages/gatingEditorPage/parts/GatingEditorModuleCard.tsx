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
import { ContentOverlayUpdate } from '../../../api/customizationApi';
import { fromISOToInput } from '../../../components/dateTime/dateTimeInputUtils';
import { isPresent } from '../../../types/option';
import { ensureNotNull } from '../../../utils/utils';
import { includes, isUndefined, map } from 'lodash';
import React, { useContext } from 'react';
import { Card, CardBody, CardHeader } from 'reactstrap';

import ActivityGateEditor from './ActivityGateEditor';
import GatedUserEditor from './GatedUserEditor';
import GatingEditorContentCard from './GatingEditorContentCard';
import GatingEditorContext from './GatingEditorContext';
import OverridePolicyEditor from './OverridePolicyEditor';
import TimePolicyEditor from './TimePolicyEditor';

const GatingEditorModuleCard: React.FC<{
  module: Content;
  customization?: ContentOverlayUpdate;
  gatedChildren: Content[];
}> = ({ module, customization, gatedChildren }) => {
  const { activeStudent, overrides } = ensureNotNull(useContext(GatingEditorContext));

  const disabled = activeStudent !== null || includes(overrides.overall, module.id);

  let temporalGate = customization ? customization.gateDate : void 0;
  if (isUndefined(temporalGate)) {
    const gate = module.gatingInformation.gate;
    temporalGate =
      isPresent(gate) && isPresent(gate.temporalGatingPolicy)
        ? gate.temporalGatingPolicy.lockDate
        : void 0;
  }
  const { date, time } = temporalGate
    ? fromISOToInput(temporalGate)
    : { date: void 0, time: void 0 };
  return (
    <Card className="mb-2 gating-editor-module">
      <CardHeader>{module.name}</CardHeader>
      <CardBody>
        <OverridePolicyEditor contentId={module.id} />
        <TimePolicyEditor
          contentId={module.id}
          policyDate={date}
          policyTime={time}
          disabled={disabled}
        />
        <ActivityGateEditor
          content={module}
          disabled={disabled}
        />
        {activeStudent === null && <GatedUserEditor contentId={module.id} />}
        {map(gatedChildren, child => (
          <GatingEditorContentCard
            content={child}
            key={child.id}
          />
        ))}
      </CardBody>
    </Card>
  );
};

export default GatingEditorModuleCard;
