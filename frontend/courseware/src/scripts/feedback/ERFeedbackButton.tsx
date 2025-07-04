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

import { useSidepanelOpen } from '../commonPages/sideNav/SideNavStateService';
import { useFeedbackDtos } from './feedback';
import { getFeedback } from './feedbackApi';
import { setFeedbackDtos } from './feedbackReducer';
import { useFeedbackOpen } from './FeedbackStateService';
import { useCourseSelector } from '../loRedux';
import { selectContent } from '../courseContentModule/selectors/contentEntrySelectors';
import { useTranslation } from '../i18n/translationContext';
import React, { useEffect } from 'react';
import { VscFeedback } from 'react-icons/vsc';
import { useDispatch } from 'react-redux';
import { useMedia } from 'react-use';
import { Badge, Button, DropdownItem } from 'reactstrap';

const ERFeedbackButton: React.FC<{ dropdown?: boolean }> = ({ dropdown }) => {
  const translate = useTranslation();
  const dispatch = useDispatch();
  const [, , toggleFeedback] = useFeedbackOpen();
  const [, toggleSidepanel] = useSidepanelOpen();
  const content = useCourseSelector(selectContent);
  const feedback = useFeedbackDtos(content.id);
  const wide = useMedia('(min-width: 94em)'); // epicscreen

  useEffect(() => {
    getFeedback(content).then(feedback => dispatch(setFeedbackDtos({ id: content.id, feedback })));
  }, [content.id]);

  const onClick = () => {
    if (!wide) toggleSidepanel(false);
    toggleFeedback();
  };

  return dropdown ? (
    <DropdownItem
      onClick={onClick}
      id="er-feedback-button"
      aria-controls="FeedbackSidebar"
    >
      {translate('FEEDBACK_SIDEBAR_TITLE')}
    </DropdownItem>
  ) : (
    <Button
      color="primary"
      outline
      className="border-white px-2 position-relative"
      onClick={onClick}
      id="er-feedback-button"
      title={translate('FEEDBACK_BUTTON')}
      aria-label={translate('FEEDBACK_BUTTON')}
      aria-controls="FeedbackSidebar"
    >
      <VscFeedback
        style={{ fontSize: '2rem' }}
        strokeWidth={0}
        aria-hidden={true}
      />
      {feedback.length > 0 && (
        <Badge
          color="dark"
          className="position-absolute"
          style={{ right: 0, bottom: 0, top: 'auto' }}
        >
          {feedback.length}
        </Badge>
      )}
    </Button>
  );
};

export default ERFeedbackButton;
