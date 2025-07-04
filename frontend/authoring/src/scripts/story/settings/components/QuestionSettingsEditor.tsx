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
import React, { useCallback } from 'react';
import NumericInput from 'react-numeric-input2';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import { usePolyglot } from '../../../hooks';
import {
  ChoiceQuestionContent,
  MultipleSelectQuestionData,
  MultipleSelectScoringOption,
  NewAsset,
  QuestionData,
} from '../../../types/asset';
import {
  DistractorRandomization,
  PointsPossible,
  StoryQuestionTypes,
  isRandomizable,
} from '../../questionUtil';
import { plural, sentence, useEditSession } from '../../story';
import { useFocusedRemoteEditor, useIsEditable } from '../../storyHooks';

// TODO: realtime

type QuestionContentEditor = {
  question: NewAsset<StoryQuestionTypes>;
  editContent: (
    comment: string,
    key: string | undefined,
    update: Partial<ChoiceQuestionContent>,
    data?: Partial<QuestionData>
  ) => void;
};

export const QuestionSettingsEditor: React.FC<
  QuestionContentEditor & {
    content: ChoiceQuestionContent;
    readOnly: boolean;
  }
> = ({ question, content, editContent, readOnly }) => {
  const polyglot = usePolyglot();
  const editMode = useIsEditable(question.name, 'EditSettings') && !readOnly;

  const { typeId } = question;

  const points = parseInt(content[PointsPossible]);

  const randomizable = isRandomizable(typeId);
  const randomized = !!content[DistractorRandomization];

  const multiselect = typeId === 'multipleSelectQuestion.1';
  const matching = typeId === 'matchingQuestion.1';
  const scoringOption =
    multiselect || matching
      ? ((question.data as MultipleSelectQuestionData).scoringOption ??
        ((question.data as MultipleSelectQuestionData).allowPartialCredit
          ? 'allowPartialCredit'
          : 'allOrNothing'))
      : undefined;

  const caseSensitival = typeId === 'fillInTheBlankQuestion.1';
  const caseSensitive = !!content.caseSensitive;

  return editMode ? (
    <div className="px-3 pb-3 text-center d-flex justify-content-center form-inline gap-2">
      <QuestionPointsEditor
        question={question}
        points={points}
        editContent={editContent}
      />

      {scoringOption && (
        <QuestionScoringOptionEditor
          question={question}
          scoringOption={scoringOption}
          editContent={editContent}
        />
      )}
      {randomizable && (
        <RandomizedDistractorEditor
          question={question}
          randomized={randomized}
          editContent={editContent}
        />
      )}
      {caseSensitival && (
        <CaseSensitiveEditor
          question={question}
          caseSensitive={caseSensitive}
          editContent={editContent}
        />
      )}
    </div>
  ) : (
    <div className="px-3 pb-2 d-flex justify-content-center">
      <span className="input-padding text-muted text-center feedback-context">
        {sentence(
          plural(points, 'point'),
          scoringOption && polyglot.t(`STORY_multiselectScoringOption_${scoringOption}`),
          !randomizable ? '' : polyglot.t(`STORY_randomized_${randomized}`),
          !caseSensitival ? '' : polyglot.t(`STORY_caseSensitive_${caseSensitive}`)
        )}
      </span>
    </div>
  );
};

const QuestionPointsEditor: React.FC<QuestionContentEditor & { points: number }> = ({
  question,
  points,
  editContent,
}) => {
  // TODO: invalid on NaN
  const session = useEditSession();
  const editPoints = useCallback(
    (points: number) =>
      editContent('Edit question points', session, {
        [PointsPossible]: isNaN(points) ? '' : `${points}`,
      }),
    [editContent, session]
  );

  const { onFocus, onBlur, remoteEditor } = useFocusedRemoteEditor(question.name, 'pointsPossible');

  return (
    <div style={remoteEditor}>
      <NumericInput
        step={1}
        min={0}
        value={isNaN(points) ? '' : points}
        format={n => plural(parseInt(n), 'Point')}
        onChange={editPoints}
        className={classNames(
          'form-control secret-input point-editor',
          remoteEditor && 'remote-edit'
        )}
        onFocus={onFocus}
        onBlur={onBlur}
      />
    </div>
  );
};

const QuestionScoringOptionEditor: React.FC<
  QuestionContentEditor & {
    scoringOption?: 'allowPartialCredit' | 'allOrNothing';
  }
> = ({ question, scoringOption, editContent }) => {
  const polyglot = usePolyglot();

  const editScoringOption = useCallback(
    (so: MultipleSelectScoringOption) => {
      if (so !== scoringOption)
        editContent(
          'Edit question scoring',
          undefined,
          {},
          {
            scoringOption: so,
            allowPartialCredit: false,
          }
        );
    },
    [scoringOption, editContent]
  );

  const { onFocus, onBlur, remoteEditor } = useFocusedRemoteEditor(question.name, 'scoringOption');

  return (
    <UncontrolledDropdown
      className={classNames('secret-input scoring-option-editor', remoteEditor && 'remote-edit')}
      onFocus={onFocus}
      onBlur={onBlur}
      style={remoteEditor}
    >
      <DropdownToggle
        color="transparent"
        caret
        className="secret-toggle"
      >
        {polyglot.t(`STORY_multiselectScoringOption_${scoringOption}`)}
      </DropdownToggle>
      <DropdownMenu>
        {(['allOrNothing', 'allowPartialCredit', 'fullCreditForAnyCorrectChoice'] as const).map(
          scoringOption => (
            <DropdownItem
              key={scoringOption}
              onClick={() => editScoringOption(scoringOption)}
            >
              {polyglot.t(`STORY_multiselectScoringOption_${scoringOption}`)}
            </DropdownItem>
          )
        )}
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};

const RandomizedDistractorEditor: React.FC<QuestionContentEditor & { randomized: boolean }> = ({
  question,
  randomized,
  editContent,
}) => {
  const polyglot = usePolyglot();
  const editRandomized = useCallback(
    (rnd: boolean) => {
      if (rnd !== randomized)
        editContent('Edit question randomization', undefined, { [DistractorRandomization]: rnd });
    },
    [randomized, editContent]
  );

  const { onFocus, onBlur, remoteEditor } = useFocusedRemoteEditor(
    question.name,
    'distractorRandomization'
  );

  return (
    <UncontrolledDropdown
      className={classNames('secret-input randomization-editor', remoteEditor && 'remote-edit')}
      onFocus={onFocus}
      onBlur={onBlur}
      style={remoteEditor}
    >
      <DropdownToggle
        color="transparent"
        caret
        className="secret-toggle"
      >
        {polyglot.t(`STORY_randomized_${randomized}`)}
      </DropdownToggle>
      <DropdownMenu>
        {([true, false] as const).map(rnd => (
          <DropdownItem
            key={rnd.toString()}
            onClick={() => editRandomized(rnd)}
          >
            {polyglot.t(`STORY_randomized_${rnd}`)}
          </DropdownItem>
        ))}
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};

const CaseSensitiveEditor: React.FC<QuestionContentEditor & { caseSensitive: boolean }> = ({
  question,
  caseSensitive,
  editContent,
}) => {
  const polyglot = usePolyglot();
  const editCaseSensitive = useCallback(
    (cs: boolean) => {
      if (cs !== caseSensitive)
        editContent('Edit question case sensitivity', undefined, {
          caseSensitive: cs || undefined,
        });
    },
    [caseSensitive, editContent]
  );

  const { onFocus, onBlur, remoteEditor } = useFocusedRemoteEditor(
    question.name,
    'caseInsensitive'
  );

  return (
    <UncontrolledDropdown
      className={classNames('secret-input case-sensitive-editor', remoteEditor && 'remote-edit')}
      onFocus={onFocus}
      onBlur={onBlur}
      style={remoteEditor}
    >
      <DropdownToggle
        color="transparent"
        caret
        className="secret-toggle"
      >
        {polyglot.t(`STORY_caseSensitive_${caseSensitive}`)}
      </DropdownToggle>
      <DropdownMenu>
        {([false, true] as const).map(cs => (
          <DropdownItem
            key={cs.toString()}
            onClick={() => editCaseSensitive(cs)}
          >
            {polyglot.t(`STORY_caseSensitive_${cs}`)}
          </DropdownItem>
        ))}
      </DropdownMenu>
    </UncontrolledDropdown>
  );
};
