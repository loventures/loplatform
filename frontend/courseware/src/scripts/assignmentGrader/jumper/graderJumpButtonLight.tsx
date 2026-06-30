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

import React from 'react';

import { getAttemptOverviews } from '../../api/attemptOverviewApi.ts';
import { useTranslation } from '../../i18n/translationContext.tsx';
import { openErrorModal } from '../../modals/errorModal/errorModal.tsx';
import { isAssessment, isGradableAssignment } from '../../utilities/contentTypes.ts';
import { gotoLink } from '../../utilities/routingUtils.js';
import { InstructorAssignmentOverviewPageLink, InstructorGraderPageLink } from '../../utils/pageLinks.js';

interface GraderJumpButtonProps {
  content: { id: string; typeId: string };
  /** When set, jumps straight to a learner's attempts (otherwise the assignment overview). */
  userId?: number;
  /** Renders the bare "View" link rather than the "Go to grader" button. */
  fromGradebook?: boolean;
}

/**
 * React port of the `graderJumpButtonLight` directive (B2). For a gradable assignment it
 * renders a button that navigates the instructor to the grader (for a given learner) or the
 * assignment overview; for an assessment with a learner it first checks the learner has a
 * submission, otherwise it surfaces the "no submission" error modal. Previously an Angular
 * component bridged into React via angular2react — now native React. Its only renderer is
 * DiscussionActivity (no Angular template mounts `<grader-jump-button-light>`). DOM preserved
 * (`.goto-grade`, `.goto-grade-label`, `.icon-circle-right`).
 */
export const GraderJumpButton: React.FC<GraderJumpButtonProps> = ({ content, userId, fromGradebook }) => {
  const translate = useTranslation();

  // isGradableAssignment only inspects typeId; the consumer passes a fuller content object.
  if (!content || !isGradableAssignment(content as any)) return null;

  const navToGrader = () => {
    if (userId) {
      gotoLink(InstructorGraderPageLink.toLink({ contentId: content.id, forLearnerId: userId }));
    } else {
      gotoLink(InstructorAssignmentOverviewPageLink.toLink({ contentId: content.id }));
    }
  };

  const checkIsUserGradable = (): Promise<boolean> =>
    userId && isAssessment(content)
      ? getAttemptOverviews([content.id], userId).then(overview => !!overview[0] && overview[0].allAttempts > 0)
      : Promise.resolve(true);

  const gotoGrader = () => {
    checkIsUserGradable().then(isUserGradable => {
      if (isUserGradable) {
        navToGrader();
      } else {
        openErrorModal({
          title: 'StudentHasNoSubmission',
          message: 'CannotGradeTillSubmit',
          actions: [],
          buttons: { hideSecondaryButton: true },
        });
      }
    });
  };

  return (
    <div className="goto-grade">
      {fromGradebook ? (
        // eslint-disable-next-line jsx-a11y/no-static-element-interactions
        <div onMouseDown={gotoGrader}>
          {/* TODO: this isn't a very good translation key */}
          <span className="goto-grade-label">{translate('View')}</span>
          <i className="icon-circle-right" />
        </div>
      ) : (
        <button
          className="btn btn-secondary"
          onMouseDown={gotoGrader}
        >
          <span className="goto-grade-label">{translate('GRADER_GO_TO')}</span>
          <i className="icon-circle-right" />
        </button>
      )}
    </div>
  );
};

