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

import { SelectionStatus } from '../feedback/feedback';
import { useTranslation } from '../i18n/translationContext';
import React, { MutableRefObject } from 'react';
import { BiCommentAdd } from 'react-icons/bi';
import { Button, PopoverBody, UncontrolledPopover } from 'reactstrap';

const FeedbackGadget: React.FC<{
  innerRef: MutableRefObject<HTMLButtonElement>;
  status: SelectionStatus;
  openFeedback: () => void;
}> = ({ innerRef, status, openFeedback }) => {
  const translate = useTranslation();
  return (
    <div
      className="feedback-gadget-holder"
      style={{
        left: status.x,
        top: status.y,
      }}
    >
      <Button
        innerRef={innerRef}
        id="feedback-gadget"
        color="primary"
        className="feedback-gadget"
        onClick={openFeedback}
        aria-label={translate('FEEDBACK_POPOVER_TEXT')}
      >
        <BiCommentAdd
          aria-hidden={true}
          size="1.5rem"
        />
      </Button>
      <UncontrolledPopover
        trigger="hover"
        target="feedback-gadget"
        placement="bottom"
        delay={500}
      >
        <PopoverBody>{translate('FEEDBACK_POPOVER_TEXT')}</PopoverBody>
      </UncontrolledPopover>
    </div>
  );
};

export default FeedbackGadget;
