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

import classnames from 'classnames';
import classNames from 'classnames';
import { uniqueId } from 'lodash';
import * as React from 'react';
import {
  Dispatch,
  MouseEventHandler,
  SetStateAction,
  useCallback,
  useEffect,
  useState,
} from 'react';
import { CircularProgressbar, buildStyles } from 'react-circular-progressbar';
import Dropzone from 'react-dropzone';
import { MdClose, MdDelete } from 'react-icons/md';
import { Button } from 'reactstrap';

import { StagedFile, uploadToCampusPack2 } from '../importer/htmlTransferService';

type Uploading = { guid: string; progress?: number };

export type FeedbackAttachment = StagedFile | Uploading;
type FeedbackAttachments = Array<FeedbackAttachment>;

export const isStagedFile = (f: FeedbackAttachment): f is StagedFile =>
  typeof (f as any).size === 'number';

export const onDropFeedbackAttachments =
  (setAttachments: Dispatch<SetStateAction<FeedbackAttachments>>) => (files: File[]) => {
    const uploading = new Array<Uploading>();
    for (const file of files) {
      const guid = uniqueId();
      uploadToCampusPack2(file, progress =>
        setAttachments(files => files.map(u => (u.guid === guid ? { guid, progress } : u)))
      )
        .then(uploaded => setAttachments(files => files.map(u => (u.guid === guid ? uploaded : u))))
        .catch(() => setAttachments(file => file.filter(u => u.guid !== guid)));
      uploading.push({ guid });
    }
    setAttachments(files => [...files, ...uploading]);
  };

export const onPasteFeedbackAttachments = (
  setAttachments: Dispatch<SetStateAction<FeedbackAttachments>>
) => {
  const listener = (event: ClipboardEvent) => {
    if (event.clipboardData?.files.length) {
      const files = new Array<File>();
      for (let i = 0; i < event.clipboardData.files.length; ++i) {
        files.push(event.clipboardData.files[i]);
      }
      onDropFeedbackAttachments(setAttachments)(files);
      event.preventDefault();
      event.stopPropagation();
    }
  };
  document.addEventListener('paste', listener, true);
  return () => {
    document.removeEventListener('paste', listener, true);
  };
};

export const FeedbackAttachmentUpload: React.FC<{
  attachments: FeedbackAttachments;
  setAttachments: Dispatch<SetStateAction<FeedbackAttachments>>;
}> = ({ attachments, setAttachments }) => {
  const [expanded, setExpanded] = useState('');

  useEffect(() => {
    if (!attachments.some(a => a.guid === expanded)) setExpanded('');
  }, [attachments, expanded]);

  const onDrop = useCallback(onDropFeedbackAttachments(setAttachments), [setAttachments]);

  const unpropagated =
    (f: () => void): MouseEventHandler =>
    e => {
      e.stopPropagation();
      f();
    };
  return (
    <Dropzone
      accept={{ 'image/*': ['.jpg', '.jpeg', '.png', '.gif'] }}
      onDrop={onDrop}
    >
      {({ getRootProps, getInputProps, isDragAccept, isDragReject }) => (
        <div
          {...getRootProps()}
          className={classnames(
            'feedback-upload',
            'mt-2',
            isDragReject && 'reject',
            isDragAccept && 'accept'
          )}
        >
          <input
            {...getInputProps()}
            multiple={false}
            name="feedback-dropzone"
          />
          {!attachments.length ? (
            <div style={{ cursor: 'pointer' }}>Attach any screenshots here.</div>
          ) : (
            <div className="staged-files">
              {attachments.map(file =>
                isStagedFile(file) ? (
                  <div
                    key={file.guid}
                    className={classNames('staged-file', expanded === file.guid && 'expanded')}
                  >
                    <Button
                      color="dark"
                      outline
                      className="p-0 image-holder"
                      disabled={expanded === file.guid}
                      onClick={unpropagated(() => setExpanded(file.guid))}
                    >
                      <img
                        className="preview"
                        alt={file.fileName}
                        src={`/api/v2/uploads/${file.guid}`}
                      />
                    </Button>
                    {expanded === file.guid ? (
                      <Button
                        key="unexpand"
                        color="dark"
                        className="un-expand"
                        onClick={unpropagated(() => setExpanded(''))}
                      >
                        <MdClose style={{ fontSize: '2rem' }} />
                      </Button>
                    ) : (
                      <Button
                        key="delete"
                        color="dark"
                        className="close-circle trash"
                        onClick={unpropagated(() =>
                          setAttachments(files => files.filter(f => f !== file))
                        )}
                      >
                        <MdDelete style={{ fontSize: '.875rem' }} />
                      </Button>
                    )}
                  </div>
                ) : (
                  <CircularProgressbar
                    key={file.guid}
                    value={100 * (file.progress ?? 0)}
                    strokeWidth={50}
                    styles={buildStyles({
                      strokeLinecap: 'butt',
                    })}
                  />
                )
              )}
            </div>
          )}
        </div>
      )}
    </Dropzone>
  );
};
