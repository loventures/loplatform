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
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { GiTv, GiVibratingBall } from 'react-icons/gi';
import { TfiClose } from 'react-icons/tfi';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';
import { useDebouncedCallback } from 'use-debounce';

import { useGraphEditSelector } from '../graphEdit';
import { deleteFromCampusPack, uploadToCampusPack2 } from '../importer/htmlTransferService';
import PresenceService from '../presence/services/PresenceService';
import { Html } from '../types/typeIds';
import { useAuthoringPreferences } from '../user/userActions';
import { useDefaultStyles } from './HtmlEditor/hooks';
import { HtmlPreview } from './HtmlEditor/HtmlPreview';
import { HtmlResources } from './HtmlEditor/HtmlResources';
import { HtmlSourceEditor } from './HtmlEditor/HtmlSourceEditor';
import { HtmlSummernoteEditor } from './HtmlEditor/HtmlSummernoteEditor';
import { HtmlViewer } from './HtmlEditor/HtmlViewer';
import { isStagedBlob, NarrativeEditor } from './story';
import { setNarrativeAssetState } from './storyActions';
import { useIsStoryEditMode, useNarrativeAssetState, useStorySelector } from './storyHooks';
import { useExitEditingOnSave } from './editorUtils.ts';

export const HtmlEditor: NarrativeEditor<Html> = props => {
  const [editing0, setEditing] = useState(false);
  const [viewSource0, setViewSource] = useState(false);
  const saving = useGraphEditSelector(state => state.saving);
  const { autoPreview } = useAuthoringPreferences();
  const dispatch = useDispatch();
  const html = props.asset;
  const source = html.data.source;
  const isUnset = !source;
  const editMode = useIsStoryEditMode() && !props.readOnly;
  const omegaEdit = useStorySelector(s => s.omegaEdit);
  const { previewing } = useNarrativeAssetState(html);
  const exitEditing = useCallback(() => {
    setEditing(false);
    setViewSource(false);
  }, []);

  useEffect(() => {
    if (!editMode) exitEditing();
  }, [editMode]);

  const viewSource = viewSource0 || omegaEdit;
  const editing = editing0 || (omegaEdit && editMode);

  // Stage edits on the server for rendering: A tuple [html?, guid?]
  // It would be nicer to put this in the store, but shrug.
  const [staged, setStaged] = useState(new Array<string>());

  const value = isStagedBlob(source) ? source.get() : undefined;

  const unstaged = value != null && (staged[0] !== value || !staged[1]);

  const doStage = useCallback(() => {
    if (value != null && staged[0] !== value) {
      const prior = staged[1];
      setStaged([value]);
      const blob = new Blob([value], { type: 'text/html' });
      uploadToCampusPack2(blob, () => void 0).then(({ guid }) => {
        setStaged(staged => (staged[0] === value ? [value, guid] : staged));
        if (prior) return deleteFromCampusPack(prior);
      });
    }
  }, [value, staged]);

  const debouncedStage = useDebouncedCallback(doStage, 5000);

  // The callbacks change every time the value changes, so this callback runs on every edit.
  useEffect(() => {
    if (!isUnset && !editing && !saving) {
      debouncedStage.cancel();
      doStage();
    } else {
      debouncedStage.cancel();
    }
  }, [editing, saving, isUnset, doStage, debouncedStage]);

  // This delay serves two purposes, it lets us keep the preview in the DOM while the panel
  // slides out, it also lets us keep the panel at z-index -1 while it slides in, before
  // moving it back to z-index 0 so it is interactible.
  const [delay, setDelay] = useState(false);

  useEffect(() => {
    const timeout = setTimeout(() => {
      setDelay(previewing);
    }, 200);
    return () => {
      clearTimeout(timeout);
    };
  }, [previewing]);

  useEffect(() => {
    if (previewing && !editing)
      dispatch(setNarrativeAssetState(html.name, { previewing: undefined }));
  }, [previewing, editing]);

  useEffect(() => {
    if (editing && autoPreview) dispatch(setNarrativeAssetState(html.name, { previewing: true }));
  }, [editing, autoPreview]);

  const [minHeight, setMinHeight] = useState(0);

  const elementRef = useRef<HTMLDivElement | null>(null);
  useEffect(() => {
    const el = elementRef.current;
    if (!el) {
      setMinHeight(0);
      return;
    }
    const resizeObserver = new ResizeObserver(() => {
      setMinHeight(el.offsetHeight);
    });
    setMinHeight(el.offsetHeight);
    resizeObserver.observe(el);
    return () => resizeObserver.disconnect(); // clean up
  }, [elementRef.current]);

  const doPreview = useCallback(
    () => dispatch(setNarrativeAssetState(html.name, { previewing: true })),
    [html.name]
  );

  useEffect(() => {
    if (editing) {
      const listener = (e: KeyboardEvent) => {
        if ((e.ctrlKey || e.metaKey) && !e.shiftKey && e.key === 'r') {
          e.preventDefault();
          if (!previewing) doPreview();
          doStage();
        }
      };
      window.addEventListener('keydown', listener);
      return () => {
        window.removeEventListener('keydown', listener);
      };
    }
  }, [editing, previewing, doStage, doPreview]);

  useEffect(() => {
    PresenceService.onAssetField(`${html.name}:html`, editing);
  }, [html.name, editing]);

  useDefaultStyles(props.projectGraph.branchId);

  useExitEditingOnSave(exitEditing);

  return (
    <div style={{ minHeight: `calc(${minHeight}px - 2rem)` }}>
      {editing && !previewing && (
        <Button
          className="html-preview-icon no-exit-edit"
          onClick={doPreview}
          title="Page Preview"
        >
          <GiTv size="1.5rem" />
        </Button>
      )}
      {!props.readOnly && (
        <div
          className="story-element preview-pane"
          ref={elementRef}
          style={previewing && delay ? undefined : { zIndex: -1 }}
        >
          {previewing && (
            <>
              <Button
                color="transparent"
                className={classNames(
                  'border-0 d-flex align-content-center position-absolute no-exit-edit previewing',
                  unstaged && 'unstaged'
                )}
                style={{ padding: '.4rem', top: '.75rem', left: '.75rem' }}
                title={unstaged ? 'Unstaged' : undefined}
                onClick={doStage}
              >
                <GiVibratingBall size="1rem" />
              </Button>
              <Button
                color="transparent"
                className="border-0 close-button d-flex align-content-center position-absolute no-exit-edit"
                style={{ padding: '.4rem', top: '.75rem', right: '.75rem' }}
                onClick={() =>
                  dispatch(setNarrativeAssetState(html.name, { previewing: undefined }))
                }
                title="Close"
              >
                <TfiClose
                  aria-hidden={true}
                  size="1rem"
                />
              </Button>
              <HtmlPreview {...{ ...props, stagedGuid: staged[1] }} />
            </>
          )}
        </div>
      )}
      {!editing && !viewSource ? (
        <HtmlViewer {...{ ...props, setEditing, setViewSource, stagedGuid: staged[1] }} />
      ) : viewSource ? (
        <HtmlSourceEditor {...{ ...props, exitEditing, editing }} />
      ) : (
        <HtmlSummernoteEditor {...{ ...props, exitEditing }} />
      )}
      <HtmlResources {...props} />
    </div>
  );
};
