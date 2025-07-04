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

import { useCourseSelector } from '../../../../loRedux';
import dayjs from 'dayjs';
import VideoActions from './VideoActions.tsx';
import { useTranslation, withTranslation } from '../../../../i18n/translationContext.tsx';
import { humanFileSize } from '../../../../utilities/fileStagingUtils.ts';
import { selectCurrentUser } from '../../../../utilities/rootSelectors.ts';
import React, { useEffect, useState } from 'react';
// @ts-ignore
import VideoRecorder from 'react-video-recorder';
import {
  Button,
  Card,
  CardBody,
  CardFooter,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from 'reactstrap';

type VideoModalProps = {
  stageVideo: (file: File) => void;
  modalOpen: boolean;
  toggleModal: () => void;
};

const getMimeType = (typ: string) => MIME_TYPES[typ] ?? '.mp4';

const MIME_TYPES: { [key: string]: string } = {
  'video/webm;codecs=vp8,opus': '.webm',
  'video/webm;codecs=h264': '.webm',
  'video/webm;codecs=vp9': '.webm',
  'video/webm': '.webm',
  'video/mp4': '.mp4',
};
const VideoModal: React.FC<VideoModalProps> = ({ stageVideo, modalOpen, toggleModal }) => {
  const translate = useTranslation();
  const [video, setVideo] = useState<File | undefined>(undefined);
  const [downloadUrl, setDownloadUrl] = useState<string | undefined>(undefined);
  const viewingAs = useCourseSelector(selectCurrentUser);
  const [errorMessage, setErrorMessage] = useState<string | undefined>(undefined);
  const [recordingInProgress, setRecordingInProgress] = useState<boolean>(false);
  const [confirm, setConfirm] = useState<boolean>(false);

  const possibleToggle = () => {
    if (Boolean(video) || recordingInProgress) {
      setConfirm(true);
    } else {
      toggleModal();
    }
  };

  const acceptVideo = (file: File) => {
    setRecordingInProgress(false);
    if (file) {
      const extension = getMimeType(file.type);
      // TODO better dayjs format
      const title = `Recording-${viewingAs.fullName}-${dayjs()}${extension}`;
      const newFile = new File([file], title, {
        type: file.type,
        lastModified: file.lastModified,
      });
      setVideo(newFile);
    }
  };

  const showError = (error: Error) => {
    console.error(error.message);
    setErrorMessage(error.message);
  };

  const stageAndClose = () => {
    if (video) {
      stageVideo(video);
      setVideo(undefined);
      toggleModal();
    } else {
      setErrorMessage('Error: No video was recorded for submission.');
    }
  };

  const downloadFile = () => {
    const element = document.createElement('a');
    element.href = downloadUrl || '';
    element.download = video?.name || '';
    document.body.appendChild(element);
    element.click();
    element.parentNode?.removeChild(element);
  };

  useEffect(() => {
    if (downloadUrl) {
      URL.revokeObjectURL(downloadUrl);
    }
    if (video) {
      setDownloadUrl(URL.createObjectURL(video));
    }
  }, [video]);

  return modalOpen ? (
    <Modal
      id="video-recording-modal"
      isOpen={modalOpen}
      toggle={possibleToggle}
      size="xl"
    >
      <ModalHeader toggle={possibleToggle}>{translate('Video Recording')}</ModalHeader>
      <ModalBody>
        <VideoRecorder
          chunkSize={250}
          constraints={{
            audio: true,
            video: true,
          }}
          countdownTime={3000}
          dataAvailableTimeout={500}
          isOnInitially
          showReplayControls
          onRecordingComplete={acceptVideo}
          onError={showError}
          onStartRecording={() => setRecordingInProgress(true)}
          onStopReplaying={() => {
            setVideo(undefined);
            setRecordingInProgress(false);
          }}
          renderActions={withTranslation(VideoActions)}
          wrapperClassName="full-height-video"
          t={translate}
        />
        {confirm ? (
          <div className="confirm-position">
            <Card>
              <CardBody>
                <span>{translate('VIDEO_MODAL_EXIT_FOR_SURE')}</span>
              </CardBody>
              <CardFooter>
                <Button
                  className="me-2"
                  color="primary"
                  outline
                  onClick={() => setConfirm(false)}
                >
                  {translate('CANCEL')}
                </Button>
                <Button onClick={toggleModal}>{translate('VIDEO_MODAL_DISCARD_EXIT')}</Button>
              </CardFooter>
            </Card>
          </div>
        ) : null}
      </ModalBody>
      <ModalFooter>
        <div className="d-flex justify-content-between m-2 align-items-center">
          {errorMessage ? <span className="text-danger me-2">{errorMessage}</span> : null}
          <Button
            color="primary"
            outline={true}
            className="me-2"
            onClick={downloadFile}
            disabled={!video}
          >
            {video
              ? translate('DOWNLOAD_VIDEO_FILE_WITH_SIZE', { size: humanFileSize(video.size) })
              : translate('DOWNLOAD_VIDEO_FILE')}
          </Button>
          <Button
            onClick={stageAndClose}
            disabled={!video}
          >
            {translate('UPLOAD_VIDEO_FILE')}
          </Button>
        </div>
      </ModalFooter>
    </Modal>
  ) : null;
};

export default VideoModal;
