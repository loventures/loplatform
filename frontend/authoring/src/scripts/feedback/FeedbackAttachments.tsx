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

import classNames from 'classnames';
import * as React from 'react';
import { useEffect, useState } from 'react';
import { Button } from 'reactstrap';

const FeedbackAttachments: React.FC<{
  id: number;
  attachments: number[];
}> = ({ id, attachments }) => {
  const [expanded, setExpanded] = useState(-1);
  useEffect(() => {
    if (expanded !== -1) {
      const listener = (e: KeyboardEvent) => {
        if (e.key === 'Escape') {
          e.stopPropagation();
          e.preventDefault();
          setExpanded(-1);
        } else if (e.key === 'ArrowRight') {
          e.stopPropagation();
          e.preventDefault();
          if (expanded < attachments.length - 1) setExpanded(1 + expanded);
        } else if (e.key === 'ArrowLeft') {
          e.stopPropagation();
          e.preventDefault();
          if (expanded > 0) setExpanded(expanded - 1);
        }
      };
      window.addEventListener('keydown', listener);
      return () => {
        window.removeEventListener('keydown', listener);
      };
    }
  }, [expanded, setExpanded, attachments]);
  return (
    attachments.length > 0 && (
      <div
        className="d-flex flex-wrap mt-3 align-items-center px-3"
        style={{ gap: '.5rem' }}
      >
        {attachments.map((attachment, jndex) => (
          <div
            key={attachment}
            className={classNames('feedback-attachment', jndex === expanded && 'expanded')}
          >
            <Button
              color="primary image-holder p-0"
              disabled={jndex === expanded}
              onClick={() => setExpanded(jndex)}
            >
              <img
                src={`/api/v2/feedback/${id}/attachments/${attachment}`}
                className="preview"
              />
            </Button>
            {jndex === expanded && (
              <Button
                color="dark"
                className="un-expand material-icons md-36"
                onClick={() => setExpanded(-1)}
              >
                close
              </Button>
            )}
            {jndex === expanded && jndex > 0 && (
              <Button
                color="dark"
                className="un-next material-icons md-36"
                onClick={() => setExpanded(jndex - 1)}
              >
                arrow_back
              </Button>
            )}
            {jndex === expanded && jndex < attachments.length - 1 && (
              <Button
                color="dark"
                className="un-prev material-icons md-36"
                onClick={() => setExpanded(jndex + 1)}
              >
                arrow_forward
              </Button>
            )}
          </div>
        ))}
      </div>
    )
  );
};

export default FeedbackAttachments;
