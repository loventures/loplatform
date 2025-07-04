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

import Course from '../../bootstrap/course';
import { useFeedbackEnabled, useFeedbackOpen } from '../../feedback/FeedbackStateService';
import React from 'react';
import { FiDownload } from 'react-icons/fi';
import { FormGroup, Label, Input } from 'reactstrap';

export const ContentFeedbackPagelet: React.FC = () => {
  const [enabled, toggleFeedback] = useFeedbackEnabled();
  const [, , toggleFeedbackOpen] = useFeedbackOpen();

  const toggle = (enabled: boolean) => {
    toggleFeedback(enabled);
    toggleFeedbackOpen(enabled);
  };

  const downloadUrl = `/api/v2/lwc/${Course.id}/feedback/download.csv`;

  return (
    <div className="feedback-pagelet">
      <p className="mt-3 mb-4">
        The Content Feedback tool turns on the commenting feature so that you can identify and
        revise outdated content directly in the course. Turning Content Feedback on only applies to
        the current login session; when you log out the Content Feedback tool will automatically be
        turned off.
      </p>
      <div className="text-center">
        <FormGroup switch>
          <Label>
            <Input
              id="content-review"
              type="switch"
              checked={enabled}
              onChange={e => toggle(e.target.checked)}
              bsSize={'md' as 'sm'}
            />
            Turn content feedback on
          </Label>
        </FormGroup>
      </div>
      <div className="my-3 text-center">
        <a
          href={downloadUrl}
          target="_blank"
          className="d-inline-flex align-items-center search-download"
        >
          Download Content Feedback CSV <FiDownload className="ms-2" />
        </a>
      </div>
    </div>
  );
};
