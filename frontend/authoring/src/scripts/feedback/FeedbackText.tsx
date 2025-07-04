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

import * as React from 'react';
import { useCallback, useState } from 'react';
import { BsClipboard, BsPencil } from 'react-icons/bs';
import { useDispatch } from 'react-redux';
import { Button, Form, Tooltip } from 'reactstrap';

import CodeEditor from '../code/CodeEditor';
import { refreshFeedback } from './feedbackActions';
import { MaxFeedbackLength } from './FeedbackApi';

const FeedbackText: React.FC<{
  id: number;
  html: string;
  editable: boolean;
  onEdit: (html: string) => Promise<any>;
  onDelete?: () => Promise<any>;
}> = ({ id, html, editable, onEdit, onDelete }) => {
  const dispatch = useDispatch();
  const [editing, setEditing] = useState(false);
  const finishEditing = useCallback(() => setEditing(false), [setEditing]);
  const [copied, setCopied] = useState(false);
  return editing ? (
    <EditText
      id={id}
      html={html}
      onCancel={finishEditing}
      onDelete={
        onDelete &&
        (() =>
          onDelete().then(() => {
            finishEditing();
            dispatch(refreshFeedback());
          }))
      }
      onEdit={html =>
        onEdit(html).then(() => {
          finishEditing();
          dispatch(refreshFeedback());
        })
      }
    />
  ) : (
    <div className="feedback-feedback mt-3 py-2 px-3 position-relative">
      <Button
        color="primary"
        outline
        className="p-1 d-flex position-absolute activity-copy-button"
        style={{ right: editable ? '2.5rem' : '.5rem', top: '.5rem' }}
        title="Copy Feedback"
        onClick={() => {
          const text = document.getElementById(`feedback-text-${id}`).innerText;
          const clipboardItem = new ClipboardItem({
            'text/html': new Blob([html], { type: 'text/html' }),
            'text/plain': new Blob([text], { type: 'text/plain' }),
          });
          navigator.clipboard.write([clipboardItem]).then(() => {
            setCopied(true);
            setTimeout(() => setCopied(false), 1500);
          });
        }}
      >
        <BsClipboard />
      </Button>
      {editable && (
        <Button
          color="primary"
          outline
          className="p-1 d-flex position-absolute activity-edit-button"
          style={{ right: '.5rem', top: '.5rem' }}
          title="Edit Feedback"
          onClick={() => setEditing(true)}
        >
          <BsPencil />
        </Button>
      )}
      <div
        id={`feedback-text-${id}`}
        className="overflow-hidden"
        dangerouslySetInnerHTML={{ __html: html }}
      />
      <Tooltip
        isOpen={copied}
        target={`feedback-text-${id}`}
      >
        Copied
      </Tooltip>
    </div>
  );
};

const EditText: React.FC<{
  id: number;
  html: string;
  onCancel: () => void;
  onEdit: (html: string) => void;
  onDelete?: () => void;
}> = ({ id, html, onCancel, onEdit, onDelete }) => {
  const [value, setValue] = useState(html);
  const invalid = value.length > MaxFeedbackLength;
  return (
    <Form
      className="d-flex flex-column add-reply feedback-feedback mt-3 py-2 px-3"
      onSubmit={e => e.preventDefault()}
    >
      <CodeEditor
        id={`activity-${id}`}
        mode="htmlmixed"
        size="inline"
        value={value}
        onChange={setValue}
        lineWrapping
        placeholder="Reply to this feedback."
        toolbar="none"
        focus={true}
      />
      {invalid && <div className="text-danger small mt-2">This reply is too large to submit.</div>}
      <div className="d-flex justify-content-end mt-2">
        {onDelete && (
          <Button
            className="me-2"
            color="danger"
            size="sm"
            outline
            onClick={onDelete}
          >
            Delete
          </Button>
        )}
        <Button
          className="me-2"
          color="primary"
          size="sm"
          outline
          onClick={onCancel}
        >
          Cancel
        </Button>
        <Button
          type="submit"
          color="primary"
          size="sm"
          disabled={!value || invalid}
          onClick={() => onEdit(value)}
        >
          Save
        </Button>
      </div>
    </Form>
  );
};

export default FeedbackText;
