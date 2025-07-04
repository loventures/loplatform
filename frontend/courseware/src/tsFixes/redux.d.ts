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

import { ThunkAction } from 'redux-thunk';
import { ReactElement } from 'react';

// https://github.com/reduxjs/redux-thunk/issues/231#issuecomment-474105114
declare module 'redux' {
  export interface Dispatch<A extends Action = AnyAction> {
    <R, S, E>(asyncAction: ThunkAction<R, S, E, A>): R;
  }
}

declare module '*.csv' {
  const content: string;
  export = content;
}

declare module 'react-video-recorder' {
  export type VideoRecorderMimeTypes =
    | 'video/webm;codecs="vp8|opus"'
    | 'video/webm;codecs=h264'
    | 'video/webm;codecs=vp9'
    | 'video/webm'
    | 'video/mp4';

  export interface VideoActionsProps {
    isVideoInputSupported: boolean;
    isInlineRecordingSupported: boolean;
    thereWasAnError: boolean;
    isRecording: boolean;
    isCameraOn: boolean;
    streamIsReady: boolean;
    isConnecting: boolean;
    isRunningCountdown: boolean;
    countdownTime: number;
    timeLimit: number;
    showReplayControls: boolean;
    replayVideoAutoplayAndLoopOff: boolean;
    isReplayingVideo: boolean;
    useVideoInput: boolean;

    onTurnOnCamera?: () => any;
    onTurnOffCamera?: () => any;
    onOpenVideoInput?: () => any;
    onStartRecording?: () => any;
    onStopRecording?: () => any;
    onPauseRecording?: () => any;
    onResumeRecording?: () => any;
    onStopReplaying?: () => any;
    onConfirm?: () => any;
  }

  export interface ReactVideoRecorderProps {
    /** Whether or not to start the camera initially */
    isOnInitially?: boolean;
    /** Whether or not to display the video flipped (makes sense for user facing camera) */
    isFlipped?: boolean;
    /** Pass this if you want to force a specific mime-type for the video */
    mimeType?: VideoRecorderMimeTypes;
    /** How much time to wait until it starts recording (in ms) */
    countdownTime?: number;
    /** Use this if you want to set a time limit for the video (in ms) */
    timeLimit?: number;
    /** Use this if you want to show play/pause/etc. controls on the replay video */
    showReplayControls?: boolean;
    /** Use this to turn off autoplay and looping of the replay video. It is recommended to also showReplayControls in order to play */
    replayVideoAutoplayAndLoopOff?: boolean;
    /** Use this if you want to customize the constraints passed to getUserMedia() */
    constraints?: {
      audio: any;
      video: any;
    };
    chunkSize?: number;
    dataAvailableTimeout?: number;
    useVideoInput?: boolean;

    renderDisconnectedView?: (props: any) => ReactElement;
    renderLoadingView?: (props: any) => ReactElement;
    renderVideoInputView?: (props: any) => ReactElement;
    renderUnsupportedView?: (props: any) => ReactElement;
    renderErrorView?: (props: any) => ReactElement;
    renderActions?: (props: VideoActionsProps) => ReactElement;

    cameraViewClassName?: string;
    videoClassName?: string;
    wrapperClassName?: string;

    /** Use this to localize the texts */
    t?: (key: string) => string;

    onCameraOn?: () => any;
    onTurnOnCamera?: () => any;
    onTurnOffCamera?: () => any;
    onStartRecording?: () => any;
    onStopRecording?: () => any;
    onPauseRecording?: () => any;
    onRecordingComplete?: (videoBlob: Blob) => void;

    onResumeRecording?: () => any;
    onOpenVideoInput?: () => any;
    onStopReplaying?: () => any;
    onError?: (error: Error) => any;
  }

  const VideoRecorder: React.FunctionComponent<ReactVideoRecorderProps>;
  export default VideoRecorder;
}

export default {};
