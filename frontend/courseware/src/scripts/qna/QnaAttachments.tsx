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

import Previewer from '../components/fileViews/Previewer';
import { QnaQuestionDto } from './qnaApi';
import { MdClose, MdDelete } from 'react-icons/md';

const QnaAttachments: React.FC<{
  question?: QnaQuestionDto;
  attachments: IoFile[];
  setAttachments: Dispatch<SetStateAction<IoFile[]>>;
}> = ({ attachments, setAttachments }) => {
  const translate = useTranslation();
  const [expandedAttachment, setExpandedAttachment] = useState<string | undefined>(undefined); // which image is expanded

  const onDrop = useCallback(useFileUploader(setAttachments), [setAttachments]);
  // Listen for the user pasting files into our UI, upload them as attachments
  usePasteListener(onDrop);

  return (
    <Dropzone
      onDrop={onDrop}
      useFsAccessApi={window.top === window.self}
    >
      {({ getRootProps, getInputProps, isDragAccept, isDragReject }) => (
        <div
          {...getRootProps()}
          className={classNames('feedback-upload p-3', {
            accept: isDragAccept,
            reject: isDragReject,
          })}
        >
          {!attachments.length ? (
            <div style={{ cursor: 'pointer' }}>{translate('QNA_ATTACH_FILES')}</div>
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
                      className="p-1 border-0 image-holder"
                      disabled={expandedAttachment === file.guid}
                      onClick={unpropagated(() => setExpandedAttachment(file.guid))}
                      title={translate('QNA_EXPAND_FILE')}
                      aria-label={translate('QNA_EXPAND_FILE')}
                    >
                      <div className="d-flex align-items-center qna-attachment-preview">
                        <Previewer
                          name={file.fileName}
                          viewUrl={`/api/v2/uploads/${file.guid}`}
                        />
                        <span className="text-start overflow-hidden">{file.fileName}</span>
                      </div>
                    </Button>
                    {expandedAttachment === file.guid ? (
                      <Button
                        color="dark"
                        className="un-expand"
                        onClick={unpropagated(() => setExpandedAttachment(''))}
                        title={translate('QNA_SHRINK_FILE')}
                        aria-label={translate('QNA_SHRINK_FILE')}
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
                        title={translate('QNA_REMOVE_FILE')}
                        aria-label={translate('QNA_REMOVE_FILE')}
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
            </div>
          )}
          <input
            id="qna-attachments"
            {...getInputProps()}
          />
        </div>
      )}
    </Dropzone>
  );
};

export default QnaAttachments;
