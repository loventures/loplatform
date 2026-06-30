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

import React, { useMemo } from 'react';

import { GradingQueueStoreLight } from '../../../assignment/GradingQueueStoreLight.ts';
import { SrsList } from '../../../srs/react/SrsList';
import { SrsStore } from '../../../srs/react/useSrsStore';
import { gotoLink } from '../../../utilities/routingUtils';
import { InstructorAssignmentListPageLink, InstructorGraderPageLink } from '../../../utils/pageLinks';

/**
 * React port of the `gradingQueue` directive (B2): the instructor-dashboard "Assignments to Grade"
 * widget. Previously an Angular component (a thin controller over the Angular `grading-assignment-list`
 * directive, which itself rendered `<srs-list>`) bridged into React via angular2react. It's now native
 * React over the React SRS list stack (`SrsList` + `useSrsStore`), driving the same Angular
 * `GradingQueueStoreLight` resource store via lojector. Its only renderer is the React
 * `ERInstructorDashboard`. DOM preserved from the old srs-list template: `ul.card-list-striped-body > li`,
 * `.flex-row-content`, `.icon-<activityType>`, `.flex-col-fluid` title, `.badge.badge-pill.badge-primary`.
 */
export const GradingQueue: React.FC = () => {
  const store = useMemo(() => new GradingQueueStoreLight() as unknown as SrsStore, []);

  const gradeAssignment = (assignment: any) =>
    // preserve the original link (contentId only; no forLearnerId) — the grader resolves the learner.
    gotoLink(InstructorGraderPageLink.toLink({ contentId: assignment.id } as any));
  const viewAssignmentsPage = () => gotoLink(InstructorAssignmentListPageLink.toLink());

  // Preserve the old angular2react host element (`<grading-queue-react>`) — the InstructorDashboard
  // Selenide page object locates this widget by it (`grading-queue-react .card-list`), exactly as it
  // still does the sibling `active-discussions-react`.
  return (
    <GradingQueueHost>
      <SrsList
        store={store}
        headerText="Assignments to Grade"
        emptyMsg="GRADING_ASSIGNMENT_EMPTY_QUEUE"
        filteredMsg="GRADING_ASSIGNMENT_EMPTY_QUEUE"
        emptyIsGood
        headerButton={{ label: 'GRADING_ASSIGNMENT_VIEW_ALL', onClick: viewAssignmentsPage }}
        getItemKey={(assignment: any) => assignment.id}
        renderItem={(assignment: any) => (
          // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions, jsx-a11y/click-events-have-key-events
          <li
            title={assignment.title}
            onClick={() => gradeAssignment(assignment)}
          >
            <div className="flex-row-content">
              <span className={`icon icon-${assignment.activityType}`} />
              <span className="flex-col-fluid">{assignment.title}</span>
              <span className="badge badge-pill badge-primary">{assignment.activeCount}</span>
            </div>
          </li>
        )}
      />
    </GradingQueueHost>
  );
};

// Custom-element host tag (rendered literally as `<grading-queue-react>`), kept for the Selenide
// page-object selector that the old angular2react bridge used to produce.
const GradingQueueHost = 'grading-queue-react' as any;

