/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import * as React from 'react';
import { ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

import ResourceActivity from '../../contentPlayerComponents/activityViews/resource/ResourceActivity.tsx';
import type { ContentWithRelationships } from '../../courseContentModule/selectors/assembleContentView.ts';
import { useTranslation } from '../../i18n/translationContext.tsx';
import { type ModalControls, openReactModal } from '../../directives/modalHost/reactModalHost.tsx';
import { READING_INSTRUCTIONS } from '../../utilities/resource1Types.js';
import type { RemediationResource } from './QuestionResourceRemediationList.tsx';

/**
 * Maps an asset-remediation resource to the `content` shape the React `ResourceActivity` dispatcher
 * consumes. This replaces the old Angular `assetRemediation` dispatcher + per-type sub-components
 * (resource1/fileBundle/html): `ResourceActivity` already routes on `content.typeId` and each sub-activity
 * loads the asset by `content.node_name`. We deliberately leave `id` unset so the HTML activity renders via
 * `getAssetRenderUrl(node_name)` (an asset reference, not a course-asset id) — matching the old
 * `resource1Remediation` controller. New-format remediations only (`_type === 'assetRemediation'`,
 * `assetType` = a CONTENT_TYPE); the long-dead legacy `.ClassName` server format is not handled.
 */
export const remediationToContent = (remediation: any): ContentWithRelationships =>
  ({
    name: remediation.title,
    typeId: remediation.assetType,
    node_name: remediation.reference?.nodeName,
    subType: remediation.resourceType,
    activity: { resourceType: remediation.resourceType || READING_INSTRUCTIONS },
  }) as unknown as ContentWithRelationships;

export const ResourceRemediationModalBody: React.FC<
  ModalControls<void> & { remediation: RemediationResource }
> = ({ dismiss, remediation }) => {
  const translate = useTranslation();
  const content = React.useMemo(() => remediationToContent(remediation), [remediation]);
  return (
    <>
      <ModalHeader tag="h1" toggle={() => dismiss()}>
        {(remediation as any).title}
      </ModalHeader>
      <ModalBody className="remediation-modal-body">
        <ResourceActivity content={content} />
      </ModalBody>
      <ModalFooter>
        <button
          className="btn btn-primary"
          onClick={() => dismiss()}
        >
          {translate('MODAL_CLOSE')}
        </button>
      </ModalFooter>
    </>
  );
};

/** Opens the resource-remediation modal — the React replacement for the deleted `resourceRemediationModal`. */
export const openResourceRemediationModal = (remediation: RemediationResource): void => {
  openReactModal(
    controls => <ResourceRemediationModalBody {...controls} remediation={remediation} />,
    { size: 'lg', className: 'question-remediation-modal' }
  );
};
