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

import { ErrorMessageFilter } from '../../../../ng';
import Tutorial from '../../../../tutorial/Tutorial.tsx';
import dayjs from 'dayjs';
import advanced from 'dayjs/plugin/advancedFormat';
import localized from 'dayjs/plugin/localizedFormat';
import timezone from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';
import { QuizInfoBar } from '../../quiz/views/QuizInfoBar.tsx';
import SubmissionAutosave from '../parts/SubmissionAutosave.tsx';
import SubmissionFiles from '../parts/SubmissionFiles.tsx';
import {
  saveSubmissionAttemptActionCreator,
  submitSubmissionAttemptActionCreator,
} from '../redux/submissionActivityActions.ts';
import { selectSubmissionActivityEditComponent } from '../redux/submissionActivitySelectors.ts';
import {
  submissionAttemptEditTextActionCreator,
  submissionAttemptRemoveAttachmentsActionCreator,
  submissionAttemptRemoveUploadActionCreator,
  submissionAttemptStageFileActionCreator,
} from '../redux/submissionInEditActions.ts';
import { SubmissionInEditState } from '../redux/submissionInEditReducer.ts';
import VideoModal from './VideoModal.tsx';
import { SubmissionActivity } from '../submissionActivity';
import FileUploadBox from '../../../../directives/FileUploadBox';
import { RichTextEditor } from '../../../../contentEditor/directives/richTextEditor';
import {
  ContentWithNebulousDetails,
  ViewingAs,
} from '../../../../courseContentModule/selectors/contentEntry';
import { FormatDayjsSetFormats } from '../../../../filters/formatDayjs';
import { useTranslation } from '../../../../i18n/translationContext.tsx';
import { NGNavBlockerService } from '../../../../services/NavBlockerService';
import { LoadingState } from '../../../../utilities/loadingStateUtils.ts';
import { enableVideoRecording } from '../../../../utilities/preferences.ts';
import React, { useCallback, useEffect, useState } from 'react';
import { IoVideocamOutline } from 'react-icons/io5';
import { ConnectedProps, connect } from 'react-redux';
import { Button } from 'reactstrap';
import { lojector } from '../../../../loject.ts';

dayjs.extend(utc);
dayjs.extend(timezone);
dayjs.extend(localized);
dayjs.extend(advanced);

export const canSubmit = ({ essay, attachments, uploads, staging }: SubmissionInEditState) =>
  ((essay && essay !== '') ||
    (attachments && attachments.length > 0) ||
    (uploads && uploads.length > 0)) &&
  (!staging || staging.unstagedFiles.length === 0);

export const hasError = (saveState: LoadingState, submitState: LoadingState) =>
  (!saveState.loading && saveState.error) || (!submitState.loading && submitState.error);

export const getError = (saveState: LoadingState, submitState: LoadingState) => {
  const error = saveState.error || submitState.error;
  return (lojector.get('errorMessageFilter') as ErrorMessageFilter)(error);
};

const connector = connect(selectSubmissionActivityEditComponent, {
  changeText: submissionAttemptEditTextActionCreator,
  stageFile: submissionAttemptStageFileActionCreator,
  removeAttachment: submissionAttemptRemoveAttachmentsActionCreator,
  removeUpload: submissionAttemptRemoveUploadActionCreator,
  saveAttempt: saveSubmissionAttemptActionCreator,
  submitAttempt: submitSubmissionAttemptActionCreator,
});
type PropsFromRedux = ConnectedProps<typeof connector>;

interface StickySubmissionEditorProps {
  submissionActivity: SubmissionActivity;
  content: ContentWithNebulousDetails;
  viewingAs: ViewingAs;
  closeEditor: () => void;
}

const SubmissionEditor: React.FC<StickySubmissionEditorProps & PropsFromRedux> = ({
  content,
  viewingAs,
  closeEditor,

  attempt,
  inEdit,
  saveState,
  submitState,

  changeText,
  stageFile,
  removeAttachment,
  removeUpload,
  saveAttempt,
  submitAttempt,

  submissionActivity: { assessment },
}) => {
  const translate = useTranslation();
  const [videoModalOpen, setVideoModalOpen] = useState<boolean>(false);

  const submit = () => {
    submitAttempt(content, attempt, inEdit);
    closeEditor();
  };

  const minimize = () => {
    saveAttempt(content, attempt, inEdit);
    closeEditor();
  };

  const addUpload = useCallback(
    file => stageFile(file, content.id, viewingAs.id),
    [content.id, viewingAs.id]
  );

  // we could be more subtle than just the video modal open but why bother.
  useEffect(() => {
    const NavBlockerService: NGNavBlockerService = lojector.get('NavBlockerService');
    return NavBlockerService.register(
      () => !!inEdit.lastUpdated || videoModalOpen,
      'QUIZ_CONFIRM_MOVE_UNSAVED_CHANGES'
    );
  }, [inEdit.lastUpdated, videoModalOpen]);

  /**
   * Magic Numbers:
   */
  const smallScreen = window.innerWidth < 569;

  return (
    <>
      <QuizInfoBar
        instructions={assessment.instructions}
        rubric={assessment.rubric}
      />

      <div className="d-flex flex-column pt-3 sticky-submission-editor">
        <RichTextEditor
          content={inEdit.essay}
          onChange={(text: string) => changeText(content.id, viewingAs.id, text)}
          isMinimal={smallScreen}
        />
        <SubmissionFiles
          attachments={inEdit.attachments}
          removeAttachment={attachment =>
            removeAttachment(content.id, viewingAs.id, attachment.id, inEdit.attachments)
          }
          uploads={inEdit.uploads}
          removeUpload={upload => removeUpload(content.id, viewingAs.id, upload)}
          filesPendingStaging={inEdit.staging}
        />
        <div className="d-flex gap-2 mt-2">
          {enableVideoRecording ? (
            <Button
              color="primary"
              outline
              className="d-flex align-items-center justify-content-center p-3"
              onClick={() => {
                setVideoModalOpen(true);
              }}
              title={translate('RECORD_VIDEO')}
            >
              <IoVideocamOutline size="1.5rem" />
            </Button>
          ) : null}
          <FileUploadBox addUpload={addUpload} />
        </div>
        <div className="d-flex mt-3 flex-column-reverse flex-md-row justify-content-end align-items-ennd align-items-md-center">
          {!hasError(saveState, submitState) && attempt && (
            <>
              {attempt.responseTime && (
                <div className="text-right text-md-left text-muted mt-2 mt-md-0 me-0 me-md-3">
                  {translate('ASSIGNMENT_LAST_SAVED')}{' '}
                  {dayjs(attempt.responseTime).format(FormatDayjsSetFormats.time)}
                </div>
              )}
              <SubmissionAutosave
                lastUpdated={inEdit.lastUpdated}
                lastSaved={attempt.responseTime}
                autosaveAction={() => inEdit.lastUpdated && saveAttempt(content, attempt, inEdit)}
              />
            </>
          )}
          <div className="d-flex justify-content-end">
            <Button
              color="success"
              outline
              onClick={minimize}
              id="assignment-save"
            >
              {translate('ASSIGNMENT_SAVE_BUTTON')}
            </Button>
            <Button
              color="success"
              className="ms-2"
              disabled={!canSubmit(inEdit)}
              onClick={submit}
              id="assignment-submit"
            >
              {translate('ASSIGNMENT_SUBMIT_BTN')}
            </Button>
          </div>
        </div>
        {hasError(saveState, submitState) && (
          <div className="alert alert-danger my-2">
            <span>{getError(saveState, submitState)}</span>
          </div>
        )}
      </div>
      <Tutorial name="assignment-attempt-editor" />
      {enableVideoRecording ? (
        <VideoModal
          stageVideo={addUpload}
          modalOpen={videoModalOpen}
          toggleModal={() => setVideoModalOpen(!videoModalOpen)}
        />
      ) : null}
    </>
  );
};

// some type fuckup
export default connector(SubmissionEditor) as unknown as React.FC<StickySubmissionEditorProps>;
