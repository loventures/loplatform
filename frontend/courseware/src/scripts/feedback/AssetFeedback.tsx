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

import 'react-circular-progressbar/dist/styles.css';

import Course from '../bootstrap/course';
import {
  SelectionStatus,
  clearIFrameSelection,
  clearMainSelection,
  useIFrameSelectionStatus,
  useMouseSelectionStatus,
} from '../feedback/feedback';
import { AssigneeDto, loadAssignees } from '../feedback/feedbackApi';
import FeedbackGadget from '../feedback/FeedbackGadget';
import FeedbackSidebar from '../feedback/FeedbackSidebar';
import { useFeedbackOpen } from '../feedback/FeedbackStateService';
import { useCourseSelector } from '../loRedux';
import { sortBy } from 'lodash';
import { selectContent } from '../courseContentModule/selectors/contentEntrySelectors';
import React, { useEffect, useRef, useState } from 'react';

const AssetFeedback: React.FC = () => {
  const [feedbackOpen, , toggleFeedbackOpen] = useFeedbackOpen();
  const content = useCourseSelector(selectContent);

  const [assignees, setAssignees] = useState(new Array<AssigneeDto>());
  useEffect(() => {
    if (Course.groupType === 'TestSection' || Course.groupType === 'PreviewSection') {
      loadAssignees().then(assignees => setAssignees(sortBy(assignees, 'fullName')));
    }
  }, [setAssignees]);

  // This holds information about what text the user select and where.
  // It may come from the main window or a content iframe.
  const [status, setStatus] = useState<SelectionStatus>({ active: false });

  const reset = () => {
    setStatus({ active: false });
    clearMainSelection();
    clearIFrameSelection();
  };

  const closeSidebar = () => {
    toggleFeedbackOpen(false);
    reset();
  };

  useEffect(() => {
    reset();
  }, [content.node_name]);

  // This lets the iframe know that we will accept feedback events so it should
  // enable selection machinery and update the selection CSS
  useEffect(() => {
    window.feedbackEnabled = true;
    return () => {
      delete window.feedbackEnabled;
    };
  }, []);

  // There is clearly some type mismatch that I need this undefined as any
  const gadgetRef = useRef<HTMLButtonElement>(undefined as any);

  // If the user selects text in the main window, place the gadget and allow feedback
  useMouseSelectionStatus(gadgetRef, setStatus);

  // If the user selects text in the iframe, place the gadget and allow feedback
  useIFrameSelectionStatus(setStatus);

  const openFeedback = () => {
    setStatus(status => ({ ...status, active: !!status.quote })); // if a quote was made, disable further selection behaviour
    toggleFeedbackOpen(true, true);
  };

  return (
    <>
      {status.quote && !status.active && (
        <FeedbackGadget
          innerRef={gadgetRef}
          status={status}
          openFeedback={openFeedback}
        />
      )}

      <FeedbackSidebar
        status={status}
        opened={feedbackOpen}
        assignees={assignees}
        close={closeSidebar}
        reset={reset}
      />
    </>
  );
};

export default AssetFeedback;
