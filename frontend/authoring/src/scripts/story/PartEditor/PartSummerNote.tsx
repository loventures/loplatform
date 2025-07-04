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

import React, { useEffect, useMemo, useRef } from 'react';
import CodeEditor from '../../code/CodeEditor';
import edgeRules from '../../editor/EdgeRuleConstants';
import { useDcmSelector } from '../../hooks';
import { BlockPart, HtmlPart, NewAsset } from '../../types/asset';
import {
  derenderHtmlAction,
  edgeTargetMap,
  isHtmlPart,
  normalizeBlankHtml,
  renderLoEdges,
  useCmsInsert,
  useCmsUpload,
} from '../editorUtils';
import { autoSaveProjectGraphEdits, useAllEditedOutEdges } from '../../graphEdit';
import { useDispatch } from 'react-redux';
import { useProjectGraphSelector } from '../../structurePanel/projectGraphHooks.ts';

export const PartSummerNote: React.FC<{
  id: string;
  asset: NewAsset<any>;
  part: BlockPart | HtmlPart | undefined;
  placeholder: string;
  compact: boolean;
  fillInTheBlank: boolean;
  setFocused: (f: boolean) => void;
  exitEditing: () => void;
  editHtml: (html: string) => void;
}> = ({
  id,
  asset,
  part,
  placeholder,
  compact,
  fillInTheBlank,
  setFocused,
  exitEditing,
  editHtml,
}) => {
  const dispatch = useDispatch();
  const cmsUpload = useCmsUpload(asset.name);
  const cmsInsert = useCmsInsert(asset.name);
  const hyperlinkable = edgeRules[asset.typeId]?.hyperlinks;
  const { eBookSupportEnabled } = useDcmSelector(s => s.configuration);

  const branchId = useProjectGraphSelector(state => state.branchId);
  const edges = useAllEditedOutEdges(asset.name);

  const original = useRef(part);

  // This is all to allow us to avoid doing rendering and derendering on every keystroke

  const initialValue = useMemo(() => {
    const part = original.current;
    const html = isHtmlPart(part)
      ? part.html.trim()
      : (part?.parts
          .map(part => part.html)
          .join('')
          .trim() ?? '');
    return (
      renderLoEdges(normalizeBlankHtml(html), branchId, edgeTargetMap(edges)) + '\n<p><br></p>'
    );
  }, [original.current, branchId, edges]);

  const value =
    part === original.current
      ? initialValue
      : isHtmlPart(part)
        ? part.html
        : (part?.parts[0]?.html ?? '');


  // derender and save edits on end editing or nav away
  const edited = useRef<[string, (html: string) => void]>(['', editHtml]);
  if (part !== original.current) edited.current = [value, editHtml];
  useEffect(
    () => () => {
      const [html, editHtml] = edited.current;
      if (html) dispatch(derenderHtmlAction(asset.name, html, editHtml));
      dispatch(autoSaveProjectGraphEdits());
    },
    [asset.name]
  );

  return (
    <CodeEditor
      id={`${asset.name}-${id}`}
      mode="htmlmixed"
      size="inline"
      value={value}
      onChange={editHtml}
      lineWrapping
      placeholder={placeholder}
      toolbar={compact ? 'simple' : undefined}
      noCustomTags
      noCustomFiles
      noRareStyles
      findReplacePlugin
      cleanerPlugin
      resizable={!compact}
      contentLink={hyperlinkable ? asset.name : undefined}
      fillInTheBlank={fillInTheBlank}
      eBookLink={eBookSupportEnabled}
      onImageLink={cmsInsert}
      onImageUpload={cmsUpload}
      doneEditing={exitEditing}
      onFocus={() => setFocused(true)}
      onBlur={() => setFocused(false)}
    />
  );
};
