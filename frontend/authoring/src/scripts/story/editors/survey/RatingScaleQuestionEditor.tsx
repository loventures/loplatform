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

import React from 'react';
import NumericInput from 'react-numeric-input2';
import { useDispatch } from 'react-redux';
import Textarea from '../../../react-textarea-autosize';
import { Col, Input, Label, Row } from 'reactstrap';

import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  editProjectGraphNodeData,
} from '../../../graphEdit';
import { NarrativeEditor, useEditSession } from '../../story';
import { useIsEditable, useNarrativeAssetState } from '../../storyHooks';

const deUntitled = (s: string) => (s === 'Untitled' ? '' : s);

export const RatingScaleQuestionEditor: NarrativeEditor<'ratingScaleQuestion.1'> = ({
  asset: question,
  readOnly,
}) => {
  const dispatch = useDispatch();
  const editMode = useIsEditable(question.name) && !readOnly;

  const { created } = useNarrativeAssetState(question);

  const session = useEditSession();
  const editPrompt = (title: string) => {
    dispatch(beginProjectGraphEdit('Question prompt', `${session}-prompt`));
    dispatch(
      editProjectGraphNodeData(question.name, {
        title: title || 'Untitled',
      })
    );
    dispatch(autoSaveProjectGraphEdits()); // TODO: BAD BLUR
  };
  const editMax = (m: number) => {
    dispatch(beginProjectGraphEdit('Max rating', `${session}-max`));
    dispatch(
      editProjectGraphNodeData(question.name, {
        max: m || 2,
      })
    );
    dispatch(autoSaveProjectGraphEdits()); // TODO: BAD BLUR
  };
  const editHighRatingText = (hr: string) => {
    dispatch(beginProjectGraphEdit('High rating text', `${session}-hi`));
    dispatch(
      editProjectGraphNodeData(question.name, {
        highRatingText: hr || 'Untitled',
      })
    );
    dispatch(autoSaveProjectGraphEdits()); // TODO: BAD BLUR
  };
  const editLowRatingText = (lr: string) => {
    dispatch(beginProjectGraphEdit('Low rating text', `${session}-lo`));
    dispatch(
      editProjectGraphNodeData(question.name, {
        lowRatingText: lr || 'Untitled',
      })
    );
    dispatch(autoSaveProjectGraphEdits()); // TODO: BAD BLUR
  };

  return (
    <>
      <div className="mx-2">
        {!editMode ? (
          <div className="input-padding">{question.data.title}</div>
        ) : (
          <Textarea
            value={deUntitled(question.data.title)}
            autoFocus={created}
            onChange={e => editPrompt(e.target.value)}
            className="form-control secret-input mb-3 py-2 px-3"
            style={{ resize: 'none' }}
            placeholder="Rating prompt"
            maxLength={255}
          />
        )}
      </div>
      <div
        className="my-3"
        style={{ marginLeft: 'calc(1.5rem - 1px)', marginRight: '.5rem' }}
      >
        <Row>
          <Label md={2}>Max Rating</Label>
          <Col
            md={10}
            className="d-flex align-items-center"
          >
            {!editMode ? (
              <div className="input-padding">{question.data.max}</div>
            ) : (
              <NumericInput
                step={1}
                min={2}
                value={question.data.max}
                onChange={editMax}
                className="form-control max-rating"
              />
            )}
          </Col>
        </Row>
        <Row className="mt-2">
          <Label md={2}>High Rating Text</Label>
          <Col
            md={10}
            className="d-flex align-items-center"
          >
            {!editMode ? (
              <div className="input-padding">{question.data.highRatingText}</div>
            ) : (
              <Input
                type="text"
                className="high-rating-text"
                value={deUntitled(question.data.highRatingText)}
                onChange={e => editHighRatingText(e.target.value)}
              />
            )}
          </Col>
        </Row>
        <Row className="mt-2">
          <Label md={2}>Low Rating Text</Label>
          <Col
            md={10}
            className="d-flex align-items-center"
          >
            {!editMode ? (
              <div className="input-padding">{question.data.lowRatingText}</div>
            ) : (
              <Input
                type="text"
                className="low-rating-text"
                value={deUntitled(question.data.lowRatingText)}
                onChange={e => editLowRatingText(e.target.value)}
              />
            )}
          </Col>
        </Row>
      </div>
    </>
  );
};
