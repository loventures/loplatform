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

import { Translate } from '../../../../i18n/translationContext.tsx';
import React from 'react';
// @ts-ignore
import { VideoActionsProps } from 'react-video-recorder';
// @ts-ignore
import Countdown from 'react-video-recorder/lib/defaults/countdown';
// @ts-ignore
import RecordButton from 'react-video-recorder/lib/defaults/record-button';
// @ts-ignore
import StopButton from 'react-video-recorder/lib/defaults/stop-button';
// @ts-ignore
import Timer from 'react-video-recorder/lib/defaults/timer';
import { Button } from 'reactstrap';

/**
 * NOTE: This file is adapted from 'react-video-recorder' source and modified for stylistic changes.
 *        We have to copy this because 'styled-components' makes it impossible to make style changes
 *        using CSS alone.
 * */

const VideoActions: React.FC<VideoActionsProps & { translate: Translate }> = ({
  translate,
  isVideoInputSupported,
  isInlineRecordingSupported,
  thereWasAnError,
  isRecording,
  isCameraOn,
  streamIsReady,
  isConnecting,
  isRunningCountdown,
  isReplayingVideo,
  countdownTime,
  timeLimit,
  // showReplayControls,
  // replayVideoAutoplayAndLoopOff,
  useVideoInput,

  onTurnOnCamera,
  // onTurnOffCamera,
  onOpenVideoInput,
  onStartRecording,
  onStopRecording,
  // onPauseRecording,
  // onResumeRecording,
  onStopReplaying,
  // onConfirm,
}) => {
  const renderContent = () => {
    const shouldUseVideoInput = !isInlineRecordingSupported && isVideoInputSupported;

    if (
      (!isInlineRecordingSupported && !isVideoInputSupported) ||
      thereWasAnError ||
      isConnecting ||
      isRunningCountdown
    ) {
      return null;
    }

    if (isReplayingVideo) {
      return (
        <Button
          type="button"
          onClick={onStopReplaying}
          data-qa="start-replaying"
          className="abs-top"
          color="danger"
        >
          {translate('Discard and Record Again')}
        </Button>
      );
    }

    if (isRecording) {
      return (
        <div className="abs-bottom">
          <StopButton
            type="button"
            onClick={onStopRecording}
            data-qa="stop-recording"
            title={translate('Stop Recording')}
            aria-label={translate('Stop Recording')}
          />
        </div>
      );
    }

    if (isCameraOn && streamIsReady) {
      return (
        <div className="abs-bottom">
          <RecordButton
            t={translate}
            type="button"
            onClick={onStartRecording}
            data-qa="start-recording"
            title={translate('Start Recording')}
            aria-label={translate('Start Recording')}
          />
        </div>
      );
    }

    if (useVideoInput) {
      return (
        <Button
          type="button"
          onClick={onOpenVideoInput}
          data-qa="open-input"
        >
          {translate('Upload a video')}
        </Button>
      );
    }

    return shouldUseVideoInput ? (
      <Button
        type="button"
        onClick={onOpenVideoInput}
        data-qa="open-input"
      >
        {translate('Record a video')}
      </Button>
    ) : (
      <Button
        type="button"
        onClick={onTurnOnCamera}
        data-qa="turn-on-camera"
      >
        {translate('Turn my camera ON')}
      </Button>
    );
  };

  return (
    <div>
      {isRecording && <Timer timeLimit={timeLimit} />}
      {isRunningCountdown && <Countdown countdownTime={countdownTime} />}
      <div className="d-flex align-items-center justify-content-center">{renderContent()}</div>
    </div>
  );
};

export default VideoActions;
