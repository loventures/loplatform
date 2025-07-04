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
import React, { useCallback, useMemo } from 'react';
import { BsClock } from 'react-icons/bs';
import NumericInput from 'react-numeric-input2';
import { useDispatch } from 'react-redux';
import { UncontrolledTooltip } from 'reactstrap';
import { useDebounce } from 'use-debounce';

import { beginProjectGraphEdit, editProjectGraphNodeData } from '../../graphEdit';
import { NewAsset } from '../../types/asset';
import { plural } from '../story';
import { useFocusedRemoteEditor, useIsEditable } from '../storyHooks';
import { durationStr } from './util';

export const WordCount: React.FC<{
  html: NewAsset<'html.1'>;
  content: string;
}> = ({ html, content }) => {
  const dispatch = useDispatch();
  const duration = html.data.duration;
  const [debouncedContent] = useDebounce(content, 500);
  const editMode = useIsEditable(html.name, 'EditSettings');

  const wordCount = useMemo(() => {
    if (!debouncedContent) return 0;
    const el = document.createElement('div');
    el.innerHTML = debouncedContent.replace(/<img/g, '<span'); // else images are downloaded
    const plain = el.innerText;
    return (plain.match(/\b\S+\b/g) || []).length;
  }, [debouncedContent]);

  const { onFocus, onBlur, remoteEditor, session } = useFocusedRemoteEditor(html.name, 'duration');

  const editDuration = useCallback(
    (dur: number) => {
      if (!dur ? !duration : dur === duration) return;
      dispatch(beginProjectGraphEdit('Edit duration', session));
      dispatch(editProjectGraphNodeData(html.name, { duration: dur || null }));
    },
    [duration, session]
  );

  // https://psyarxiv.com/xynwg/ says 238wpm, let's assume otherwise

  return (
    <div
      className="d-flex justify-content-between mt-3 px-2 hide-nudge-arrows"
      style={remoteEditor}
    >
      {editMode ? (
        <NumericInput
          step={1}
          min={0}
          value={!duration || isNaN(duration) ? 0 : duration}
          onChange={editDuration}
          format={n => (!parseInt(n) ? 'No Duration Set' : plural(parseInt(n), 'Minute'))}
          className={classNames(
            'form-control secret-input duration-editor bigly',
            remoteEditor && 'remote-edit'
          )}
          placeholder="No Duration Set"
          onFocus={onFocus}
          onBlur={onBlur}
        />
      ) : (
        <span className="text-muted input-padding">
          {!duration ? 'No Duration Set' : plural(duration, 'Minute')}
        </span>
      )}

      {!!wordCount && (
        <>
          <div className="text-muted input-padding d-flex align-items-center gap-2">
            <BsClock id={`wc-${html.name}`} className="br-50" />
            {`${plural(wordCount, 'word')}.`}
          </div>
          <UncontrolledTooltip
            target={`wc-${html.name}`}
            className="html-duration"
          >
            <div>Estimated Reading Time</div>
            <div className="text-nowrap">
              <strong>Level 1: </strong> {durationStr(wordCount, 200, 166)}
            </div>
            <div className="text-nowrap">
              <strong>Level 2: </strong> {durationStr(wordCount, 133, 133)}
            </div>
            <div className="text-nowrap">
              <strong>Level 3: </strong> {durationStr(wordCount, 100, 66)}
            </div>
          </UncontrolledTooltip>
        </>
      )}
    </div>
  );
};
