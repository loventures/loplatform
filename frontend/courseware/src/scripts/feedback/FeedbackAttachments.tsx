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
import { IoFile, isStagedFile, useFileUploader } from '../api/fileUploadApi';
import { unpropagated, usePasteListener } from '../feedback/feedback';
import { useTranslation } from '../i18n/translationContext';
import React, { Dispatch, SetStateAction, useCallback, useState } from 'react';
import { CircularProgressbar, buildStyles } from 'react-circular-progressbar';
import Dropzone from 'react-dropzone';
import { Button } from 'reactstrap';
import { MdClose, MdDelete } from 'react-icons/md';

const FeedbackAttachments: React.FC<{
  attachments: IoFile[];
  setAttachments: Dispatch<SetStateAction<IoFile[]>>;
}> = ({ attachments, setAttachments }) => {
  const translate = useTranslation();
  const [expandedAttachment, setExpandedAttachment] = useState<string | undefined>(undefined); // which image is expanded

  const onDrop = useCallback(useFileUploader(setAttachments), [setAttachments]);
  // Listen for the user pasting images into our UI, upload them as attachments
  usePasteListener(onDrop);

  return (
    <Dropzone
      accept={{ 'image/*': ['.jpg', '.jpeg', '.png', '.gif'] }}
      onDrop={onDrop}
    >
      {({ getRootProps, getInputProps, isDragAccept, isDragReject }) => (
        <div
          {...getRootProps()}
          className={classNames('feedback-upload', { accept: isDragAccept, reject: isDragReject })}
        >
          {!attachments.length ? (
            <div style={{ cursor: 'pointer' }}>{translate('FEEDBACK_ATTACH_IMAGES')}</div>
          ) : (
            <div className="staged-files">
              {attachments.map(file =>
                isStagedFile(file) ? (
                  <div
                    key={file.guid}
                    className={classNames(
                      'staged-file',
                      expandedAttachment === file.guid && 'expanded'
                    )}
                  >
                    <Button
                      color="dark"
                      className="p-0 border-0 image-holder"
                      disabled={expandedAttachment === file.guid}
                      onClick={unpropagated(() => setExpandedAttachment(file.guid))}
                      title={translate('FEEDBACK_EXPAND_IMAGE')}
                      aria-label={translate('FEEDBACK_EXPAND_IMAGE')}
                    >
                      <img
                        className="preview"
                        alt={file.fileName}
                        src={`/api/v2/uploads/${file.guid}`}
                      />
                    </Button>
                    {expandedAttachment === file.guid ? (
                      <Button
                        color="dark"
                        className="un-expand"
                        onClick={unpropagated(() => setExpandedAttachment(''))}
                        title={translate('FEEDBACK_SHRINK_IMAGE')}
                        aria-label={translate('FEEDBACK_SHRINK_IMAGE')}
                      >
                        <MdClose
                          aria-hidden={true}
                          style={{ fontSize: '2rem' }}
                        />
                      </Button>
                    ) : (
                      <Button
                        color="dark"
                        className="close-circle trash"
                        onClick={unpropagated(() =>
                          setAttachments(files => files.filter(f => f !== file))
                        )}
                        title={translate('FEEDBACK_REMOVE_IMAGE')}
                        aria-label={translate('FEEDBACK_REMOVE_IMAGE')}
                      >
                        <MdDelete
                          aria-hidden={true}
                          style={{ fontSize: '.875rem' }}
                        />
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
              <input
                id="feedback-attachments"
                {...getInputProps()}
              />
            </div>
          )}
        </div>
      )}
    </Dropzone>
  );
};

export default FeedbackAttachments;
