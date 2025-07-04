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

import { maxBy } from 'lodash';
import React, { useCallback, useMemo } from 'react';

import {
  DefinitionContent,
  MatchTuple,
  MatchingQuestionContent,
  NewAsset,
  TermContent,
} from '../../../types/asset';
import {
  DefinitionText,
  Definitions,
  StoryQuestionTypes,
  TermText,
  Terms,
} from '../../questionUtil';
import { useIsEditable } from '../../storyHooks';
import { MatchEditor } from './MatchEditor';

const NoChoices = [];

// DE only supports 1:1 terms to definitions and may or may not support out of order. QTI can import
// questions that don't match these constraints. The platform will not work for such questions.
export const QuestionMatchEditor: React.FC<{
  question: NewAsset<StoryQuestionTypes>;
  content: MatchingQuestionContent;
  editContent: (
    comment: string,
    key: string | undefined,
    update: Partial<MatchingQuestionContent>
  ) => void;
  readOnly: boolean;
}> = ({ question, content, editContent, readOnly }) => {
  const editMode = useIsEditable(question.name) && !readOnly;

  const terms: TermContent[] = content[Terms] ?? NoChoices;
  const definitions: DefinitionContent[] = content[Definitions] ?? NoChoices;

  const nextIndex = (maxBy(definitions, 'index')?.index ?? -1) + 1;

  const nextMatch = useMemo<MatchTuple>(
    () => ({
      term: {
        [TermText]: '',
        correctDefinitionIndex: nextIndex,
      },
      definition: {
        [DefinitionText]: '',
        index: nextIndex,
      },
    }),
    [nextIndex]
  );

  const matchesWithNext = useMemo(
    () => [
      ...terms.map(term => ({
        term,
        definition: definitions.find(d => d.index === term.correctDefinitionIndex) ?? {
          [DefinitionText]: '',
          index: term.correctDefinitionIndex,
        },
      })),
      ...(editMode ? [nextMatch] : []),
    ],
    [definitions, terms, nextMatch, editMode]
  );

  const editMatches = useCallback(
    (
      comment: string,
      key: string | undefined,
      terms: TermContent[],
      definitions: DefinitionContent[]
    ) => {
      editContent(comment, key, { [Terms]: terms, [Definitions]: definitions });
    },
    [content, editContent]
  );

  const editMatch = useCallback(
    (match: MatchTuple, comment: string, key: string | undefined): void => {
      const tindex = terms.findIndex(
        t => t.correctDefinitionIndex === match.term.correctDefinitionIndex
      );
      const dindex = definitions.findIndex(d => d.index === match.definition.index);
      const trms = [...terms];
      const dfns = [...definitions];
      trms[tindex < 0 ? trms.length : tindex] = match.term;
      dfns[dindex < 0 ? dfns.length : dindex] = match.definition;
      editMatches(comment, key, trms, dfns);
    },
    [definitions, terms, editMatches]
  );

  const deleteMatch = useCallback(
    (match: MatchTuple) => {
      const tindex = terms.findIndex(
        t => t.correctDefinitionIndex === match.term.correctDefinitionIndex
      );
      const dindex = definitions.findIndex(d => d.index === match.definition.index);
      const trms = [...terms];
      const dfns = [...definitions];
      trms.splice(tindex, 1);
      dfns.splice(dindex, 1);
      editMatches('Delete match', undefined, trms, dfns);
    },
    [definitions, terms, editMatches]
  );

  return (
    <div className="mt-2">
      {matchesWithNext.map(match => (
        <MatchEditor
          key={match.definition.index}
          question={question}
          match={match}
          editMatch={editMatch}
          deleteMatch={deleteMatch}
          isAdd={match.definition.index === nextIndex}
          readOnly={readOnly}
        />
      ))}
    </div>
  );
};
