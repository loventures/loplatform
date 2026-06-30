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
import { WithTranslateProps } from '../../i18n/translationContext.tsx';
import { openResourceRemediationModal } from './ResourceRemediationModal.tsx';

export interface RemediationResource {
  title?: string;
  /** the new-format asset remediation fields the modal maps to a ResourceActivity `content`. */
  assetType?: string;
  resourceType?: string;
  reference?: { nodeName?: string };
  _type?: string;
}

export type QuestionResourceRemediationListProps = WithTranslateProps & {
  resources?: RemediationResource[];
};

/**
 * React port of the `questionResourceRemediationList` addon (B2-quiz). Renders
 * the per-question list of remediation resources shown after a wrong answer;
 * each opens the remediation modal. DOM preserved from the template:
 * `.card.card-body.question-remediation-resources` > a `.h5` heading +
 * `ul.remediation-link > li > button.btn.btn-link` (the Selenide selectors).
 *
 * The modal is now fully React (`openResourceRemediationModal` → the React modal
 * host rendering `ResourceActivity`), replacing the deleted Angular
 * `resource-remediation-modal` + `<asset-remediation>` `$compile` dispatcher.
 */
export const QuestionResourceRemediationList = ({
  resources,
  translate,
}: QuestionResourceRemediationListProps) => {
  const showRemediation = (remediation: RemediationResource) => {
    openResourceRemediationModal(remediation);
  };

  return (
    <div className="card card-body question-remediation-resources">
      <div className="h5">{translate('QUESTION_REMEDIATIONS')}</div>
      <ul className="remediation-link">
        {(resources ?? []).map((remediation, i) => (
          <li key={i}>
            <button
              className="btn btn-link"
              onClick={() => showRemediation(remediation)}
            >
              {remediation.title}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
};
