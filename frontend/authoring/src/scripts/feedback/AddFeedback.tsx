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
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Button, FormGroup, Input, Label } from 'reactstrap';

import { trackFeedbackPanelAdd } from '../analytics/AnalyticsEvents';
import CodeEditor from '../code/CodeEditor';
import edgeRules from '../editor/EdgeRuleConstants';
import { getEditedAsset, useGraphEdits } from '../graphEdit';
import { useBranchId, useDcmSelector } from '../hooks';
import { dropTrailingParagraphs, isBlankHtml } from '../story/editorUtils';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { openToast } from '../toast/actions';
import { feedbackAdded, refreshFeedback, setAddFeedbackForAsset } from './feedbackActions';
import { MaxFeedbackLength, postFeedback } from './FeedbackApi';
import {
  FeedbackAttachment,
  FeedbackAttachmentUpload,
  isStagedFile,
  onDropFeedbackAttachments,
  onPasteFeedbackAttachments,
} from './FeedbackAttachmentUpload';
import { useAddFeedback, useFeedbackAssignees } from './feedbackHooks';

export const isContentType = (typeId: string | undefined) =>
  edgeRules['lesson.1'].elements.includes(typeId as any);

const AddFeedback: React.FC = () => {
  const dispatch = useDispatch();
  const [feedback, setFeedback] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [attachments, setAttachments] = useState(new Array<FeedbackAttachment>());
  const [assignee, setAssignee] = useState<number | undefined>(undefined);
  const assignees = useFeedbackAssignees();

  const { project } = useDcmSelector(state => state.layout);
  const branchId = useBranchId();
  const addFeedback = useAddFeedback();
  const projectGraph = useProjectGraph();
  const graphEdits = useGraphEdits();
  const nodes = useMemo(
    () =>
      addFeedback?.path
        .map(name => getEditedAsset(name, projectGraph, graphEdits))
        .filter(node => !!node) ?? [],
    [addFeedback, projectGraph, graphEdits]
  );
  const asset = nodes[nodes.length - 1];
  const quote = addFeedback?.quote;
  const id = addFeedback?.id;

  const submitFeedback = useCallback(() => {
    if (!asset) return;
    setSubmitting(true);
    const lesson = nodes.find(node => node.typeId === 'lesson.1');
    const module = nodes.find(node => node.typeId === 'module.1');
    const unit = nodes.find(node => node.typeId === 'unit.1');
    const content = isContentType(asset.typeId)
      ? asset
      : nodes.find(node => isContentType(node.typeId));
    postFeedback({
      project: project!.id,
      branch: branchId!,
      assetName: asset.name,
      contentName: content?.name ?? null,
      lessonName: lesson?.name ?? null,
      moduleName: module?.name ?? null,
      unitName: unit?.name ?? null,
      identifier: id ?? null,
      assignee,
      quote,
      feedback: dropTrailingParagraphs(feedback),
      attachments,
    })
      .then(dto => {
        dispatch(feedbackAdded(dto.id));
        dispatch(setAddFeedbackForAsset(undefined));
        dispatch(refreshFeedback());
        dispatch(openToast('Feedback added.', 'success'));
        trackFeedbackPanelAdd();
        setTimeout(() => {
          dispatch(feedbackAdded(undefined));
        }, 500);
      })
      .finally(() => setSubmitting(false));
  }, [asset, nodes, project, branchId, id, assignee, quote, feedback, attachments]);

  const [focused, setFocused] = useState(false);
  const onFocus = useCallback(() => setFocused(true), []);
  const onBlur = useCallback(() => setFocused(false), []);
  const onDrop = useCallback(onDropFeedbackAttachments(setAttachments), []);

  useEffect(() => (focused ? onPasteFeedbackAttachments(setAttachments) : undefined), [focused]);

  // summernote fails to autofocus correctly if mounted inline
  const [delayEditor, setDelayEditor] = useState(true);
  useEffect(() => {
    setTimeout(() => setDelayEditor(false), 0);
  }, []);

  useEffect(() => {
    // So it actually proves impossible to clear summernote. If we just unmount and remount it,
    // it comes back with the original content. If we run a summernote command to reset the content
    // then it adds back all the default toolbars.
    setFeedback('');
    setAttachments([]);
  }, [asset?.name]);

  const isUploading = attachments.some(f => !isStagedFile(f));
  const invalid = feedback.length > MaxFeedbackLength;
  const isBlankFeedback = useMemo(() => isBlankHtml(feedback), [feedback]);

  const imageLike = !!quote?.match(/^http.*\.(?:jpg|jpeg|gif|png|webp|svg)$/i);
  const cannotSubmit = isUploading || isBlankFeedback || submitting || invalid;

  const doneEditing = () => {
    if (!cannotSubmit) submitFeedback();
  };

  // I can't !asset ? null because then it won't slide out nicely
  return (
    <div className="add-feedback">
      <h3 className="h5 text-truncate p-3 add-title">Feedback on {asset?.data.title}</h3>
      {quote ? (
        <FormGroup className="m-3">
          <Label>Quote</Label>
          {imageLike ? (
            <div>
              <a
                className="content-img-link btn btn-primary p-0"
                href={quote}
                target="_blank"
                rel="noreferrer noopener"
              >
                <img
                  src={quote}
                  alt="Quoted Image"
                />
              </a>
            </div>
          ) : (
            <div className="text-muted">{quote}</div>
          )}
        </FormGroup>
      ) : null}
      <FormGroup className="m-3">
        <Label for="feedback-input">Comments</Label>
        {delayEditor || !asset ? (
          <div className="editor-placeholder" />
        ) : (
          <CodeEditor
            id={`feedback-${asset?.name}`}
            mode="htmlmixed"
            size="inline"
            value={feedback}
            onChange={setFeedback}
            onFocus={onFocus}
            onBlur={onBlur}
            onImageUpload={onDrop}
            lineWrapping
            placeholder="Enter your feedback on this content."
            toolbar="simple"
            doneEditing={doneEditing}
          />
        )}
        {invalid && (
          <div className="text-danger small mt-3">This feedback is too large to submit.</div>
        )}
      </FormGroup>
      <FormGroup className="m-3">
        <Label>Attachments</Label>
        <FeedbackAttachmentUpload
          attachments={attachments}
          setAttachments={setAttachments}
        />
      </FormGroup>
      <FormGroup className="m-3">
        <Label for="feedback-assignee">Assignee</Label>
        <Input
          id="feedback-assignee"
          value={assignee?.toString() ?? ''}
          onChange={e => setAssignee(e.target.value ? parseInt(e.target.value) : undefined)}
          type="select"
        >
          <option value="">Unassigned</option>
          {assignees.map(a => (
            <option
              key={a.id}
              value={a.id}
            >
              {a.fullName}
            </option>
          ))}
        </Input>
      </FormGroup>
      <div
        className="d-flex justify-content-end m-3"
        style={{ gap: '.75rem' }}
      >
        <Button
          color="primary"
          outline
          onClick={() => dispatch(setAddFeedbackForAsset(undefined))}
        >
          Cancel
        </Button>
        <Button
          color="primary"
          onClick={submitFeedback}
          disabled={cannotSubmit}
        >
          Submit
        </Button>
      </div>
    </div>
  );
};

export default AddFeedback;
