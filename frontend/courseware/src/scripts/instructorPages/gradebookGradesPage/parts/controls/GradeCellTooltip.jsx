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

import { compose, withHandlers, withState } from 'recompose';
import { withTranslation } from '../../../../i18n/translationContext';

import { gotoLink } from '../../../../utilities/routingUtils';
import { InstructorGraderPageLink } from '../../../../utils/pageLinks';
import { Modal, ModalBody, ModalFooter, ModalHeader, Tooltip } from 'reactstrap';
import { getAttemptOverviews } from '../../../../api/attemptOverviewApi';
import { useState } from 'react';
import { useDebounce } from 'react-use';

const GradeCellTooltip = ({
  translate,
  isOpen,
  showExplanation,
  setShowExplanation,
  gotoGrading,
  target,
}) => {
  const [open, setOpen] = useState(false);
  useDebounce(() => setOpen(isOpen), 1, [isOpen]);
  return (
    <div>
      <Tooltip
        className="tooltip tooltip-secondary"
        isOpen={open}
        target={target}
        container={target}
      >
        <button
          className="goto-grade-button icon-btn"
          onMouseDown={gotoGrading}
          title={translate('GRADER_GO_TO')}
        >
          <i className="icon-circle-right text-white"></i>
          <span className="sr-only">{translate('GRADER_GO_TO')}</span>
        </button>
      </Tooltip>
      {showExplanation && (
        <Modal
          isOpen={true}
          toggle={() => setShowExplanation(false)}
        >
          <ModalHeader>{translate('StudentHasNoSubmission')}</ModalHeader>
          <ModalBody>
            <span>{translate('CannotGradeTillSubmit')}</span>
          </ModalBody>
          <ModalFooter>
            <button
              className="btn btn-primary"
              onClick={() => setShowExplanation(false)}
            >
              {translate('OK')}
            </button>
          </ModalFooter>
        </Modal>
      )}
    </div>
  );
};

export default compose(
  withState('showExplanation', 'setShowExplanation', false),
  withHandlers({
    gotoGrading:
      ({ grade, setShowExplanation }) =>
      () => {
        getAttemptOverviews([grade.column_id], grade.user_id)
          .then(overview => overview[0] && overview[0].allAttempts > 0)
          .then(isGradable => {
            if (isGradable) {
              gotoLink(
                InstructorGraderPageLink.toLink({
                  contentId: grade.column_id,
                  forLearnerId: grade.user_id,
                })
              );
            } else {
              setShowExplanation(true);
            }
          });
      },
  }),
  withTranslation
)(GradeCellTooltip);
