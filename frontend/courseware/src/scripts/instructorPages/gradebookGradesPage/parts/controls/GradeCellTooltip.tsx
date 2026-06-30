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

import { withTranslation, WithTranslateProps } from '../../../../i18n/translationContext';

import { gotoLink } from '../../../../utilities/routingUtils';
import { InstructorGraderPageLink } from '../../../../utils/pageLinks';
import { Modal, ModalBody, ModalFooter, ModalHeader, Tooltip } from 'reactstrap';
import { getAttemptOverviews } from '../../../../api/attemptOverviewApi';
import { useState } from 'react';
import { useDebounce } from 'react-use';

interface GradeCellTooltipProps extends WithTranslateProps {
  isOpen: boolean;
  showExplanation: boolean;
  setShowExplanation: (show: boolean) => void;
  gotoGrading: () => void;
  target: string;
}

const GradeCellTooltip = ({
  translate,
  isOpen,
  showExplanation,
  setShowExplanation,
  gotoGrading,
  target,
}: GradeCellTooltipProps) => {
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

const GradeCellTooltipTranslated = withTranslation(GradeCellTooltip);

const GradeCellTooltipWrapper = (props: any) => {
  const { grade } = props;
  const [showExplanation, setShowExplanation] = useState(false);
  const gotoGrading = () => {
    getAttemptOverviews([grade.column_id], grade.user_id)
      .then((overview: any) => overview[0] && overview[0].allAttempts > 0)
      .then((isGradable: boolean) => {
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
  };
  return (
    <GradeCellTooltipTranslated
      {...props}
      showExplanation={showExplanation}
      setShowExplanation={setShowExplanation}
      gotoGrading={gotoGrading}
    />
  );
};

export default GradeCellTooltipWrapper;
