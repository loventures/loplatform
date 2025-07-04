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

import { trackEvent } from '../../analytics/ga';
import { useCourseSelector } from '../../loRedux';
import { useTranslation } from '../../i18n/translationContext';
import { statusFlagToggleActionCreatorMaker } from '../../utilities/statusFlagReducer';
import React, { useEffect, useState } from 'react';
import { FiInfo } from 'react-icons/fi';
import { useDispatch } from 'react-redux';
import { Button, DropdownItem } from 'reactstrap';

const instructingToggleAction = statusFlagToggleActionCreatorMaker({
  sliceName: 'instructingState',
});

const ERInstructionsButton: React.FC<{ stuck: boolean; dropdown?: boolean }> = ({
  stuck,
  dropdown,
}) => {
  const instructions = useCourseSelector(state => state.ui.instructionsState.value);
  const instructing = !!useCourseSelector(state => state.ui.instructingState.status);
  const dispatch = useDispatch();
  const translate = useTranslation();
  const [open, setOpen] = useState(false);

  const showInstructions = open && stuck && !!instructions;
  useEffect(() => {
    dispatch(instructingToggleAction(showInstructions));
  }, [showInstructions]);
  useEffect(() => {
    if (!instructions) setOpen(false);
  }, [instructions]);

  const title = translate(instructing ? 'HIDE_INSTRUCTIONS' : 'SHOW_INSTRUCTIONS');

  const onClick =
    () => {
      setOpen(s => !s);
      trackEvent('Instructions Button', open ? 'Hide Instructions' : 'Show Instructions');
    };

  return stuck && instructions ? (
    dropdown ? (
      <DropdownItem
        id="react-collapsed-toggle-instructions"
        onClick={onClick}
        aria-controls="react-collapsed-panel-instructions"
        arai-expanded={`${instructing}`}
      >
        {title}
      </DropdownItem>
    ) : (
      <Button
        id="react-collapsed-toggle-instructions"
        color="primary"
        outline
        className="border-white px-2"
        onClick={onClick}
        active={instructing}
        title={title}
        aria-label={title}
        aria-controls="react-collapsed-panel-instructions"
        arai-expanded={`${instructing}`}
      >
        <FiInfo
          size="2rem"
          strokeWidth={1.1}
          fill="transparent"
          aria-hidden={true}
        />
      </Button>
    )
  ) : null;
};

export default ERInstructionsButton;
