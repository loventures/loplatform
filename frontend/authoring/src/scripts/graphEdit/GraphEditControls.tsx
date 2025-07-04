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

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Button } from 'reactstrap';

import { trackAuthoringEvent } from '../analytics';
import { useDcmSelector, usePolyglot } from '../hooks';
import { DiscardChangesModalData } from '../modals/DiscardChangesModal';
import { openModal } from '../modals/modalActions';
import { ModalIds } from '../modals/modalIds';
import PreventNavAndUnsavedChangesPrompt from '../router/PreventNavAndUnsavedChangesPrompt';
import { useStorySelector } from '../story/storyHooks';
import { suppressPromptForUnsavedGraphEdits } from './graphEdit';
import {
  autoSaveProjectGraphEdits,
  redoProjectGraphEdit,
  resetProjectGraphEdits,
  safeSaveProjectGraphEdits,
  undoProjectGraphEdit,
} from './graphEditActions';
import { useGraphEditSelector } from './graphEditHooks';
import { IoArrowRedoOutline, IoArrowUndoOutline } from 'react-icons/io5';

export const GraphEditControls: React.FC<{ appName: string }> = ({ appName }) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const [saved, setSaved] = useState(false);
  const initial = useGraphEditSelector(state => state.initial);
  const saving = useGraphEditSelector(state => state.saving);
  const dirty = useGraphEditSelector(state => state.dirty);
  const problem = useGraphEditSelector(state => state.problem);
  const savedUndo = useGraphEditSelector(state => state.undos[0]?.comment);
  const unsavedUndo = useGraphEditSelector(state => state.undo?.comment);
  const redo = useGraphEditSelector(state => state.redo?.comment);
  const { realTime } = useDcmSelector(state => state.configuration);
  const offline = useStorySelector(state => state.offline);
  const role = useDcmSelector(state => state.layout.role);
  const undo = unsavedUndo ?? savedUndo;

  const doSave = useCallback(
    (kind: string) => {
      trackAuthoringEvent(`${appName} - Save`, kind);
      //window.postMessage({ type: 'exitEdit' });
      dispatch(safeSaveProjectGraphEdits(() => setSaved(true)));
    },
    [appName]
  );

  const btnSave = useCallback(() => doSave('Button'), [doSave]);

  const cancel = useCallback(() => {
    dispatch(
      openModal<DiscardChangesModalData>(ModalIds.DiscardChanges, {
        discard: () => {
          trackAuthoringEvent(`${appName} - Reset`);
          dispatch(resetProjectGraphEdits());
        },
      })
    );
  }, [appName]);

  const suppressUndo = useRef(false);
  const doUndo = useCallback(() => {
    if (suppressUndo.current) {
      suppressUndo.current = false;
      return;
    }
    trackAuthoringEvent(`${appName} - Undo`);
    dispatch(undoProjectGraphEdit());
  }, [appName]);

  // If you edit a field and click the undo button, do the undo immediately
  // on-heap so no save goes down the wire. This has to be onMouseDownCapture
  // to occur before the blur triggers a save. We also need to suppress the
  // next onClick undo event, else if you do something, save, then type in
  // the field and click undo, the captured press will undo the type and the
  // subsequent click event will undo the save.
  const doCapturedUndo = realTime && !!unsavedUndo;
  const captureUndo = useCallback(() => {
    if (doCapturedUndo) {
      doUndo();
      suppressUndo.current = true;
    }
  }, [doCapturedUndo, doUndo]);

  // If you undo the last change, the button becomes disabled which means it
  // does not deliver the mouse click event so we have to explicitly clear
  // suppression here instead.
  useEffect(() => {
    if (!undo) suppressUndo.current = false;
  }, [!undo]);

  const doRedo = useCallback(() => {
    trackAuthoringEvent(`${appName} - Redo`);
    dispatch(redoProjectGraphEdit());
  }, [appName]);

  useEffect(() => {
    const listener = (e: KeyboardEvent) => {
      if (e.key === 's' && (e.ctrlKey || e.metaKey)) {
        e.preventDefault();
        if (dirty) doSave('Keyboard');
      }
    };
    window.addEventListener('keydown', listener);
    return () => {
      window.removeEventListener('keydown', listener);
    };
  }, [dirty, doSave]);

  useEffect(() => {
    if (!offline) dispatch(autoSaveProjectGraphEdits());
  }, [offline]);

  useEffect(() => {
    if (dirty) {
      const listener = (e: Event) => {
        if (!suppressPromptForUnsavedGraphEdits()) {
          e.preventDefault();
          (e as any).returnValue = ''; // wth?
        }
      };
      window.addEventListener('beforeunload', listener);
      return () => window.removeEventListener('beforeunload', listener);
    }
  }, [dirty]);

  return dirty || !initial ? (
    <div className="flex-grow-0 form-inline d-flex align-items-center flex-nowrap">
      <PreventNavAndUnsavedChangesPrompt />
      <Button
        id="graph-edit-undo"
        color="transparent"
        className="d-flex align-items-center mini-button p-1"
        size="sm"
        onMouseDownCapture={captureUndo}
        onClick={doUndo}
        title={undo ? `Undo: ${undo}` : ''}
        disabled={saving || problem || !undo}
      >
        <IoArrowUndoOutline size="1.2rem" />
      </Button>
      {!realTime /* too lazy to build the realtime redo queue */ && (
        <Button
          id="graph-edit-redo"
          color="transparent"
          className="d-flex align-items-center mini-button p-1"
          size="sm"
          onClick={doRedo}
          title={redo ? `Redo: ${redo}` : ''}
          disabled={saving || problem || !redo}
        >
          <IoArrowRedoOutline size="1.2rem" />
        </Button>
      )}
      {realTime && !problem ? (
        <Button
          size="sm"
          color="transparent"
          disabled={offline || saving || !dirty}
          id="graph-edit-status"
          className={
            offline && dirty
              ? 'status-offline'
              : saving
                ? 'status-saving'
                : dirty
                  ? 'status-unsaved'
                  : 'status-saved'
          }
          onClick={() => setTimeout(btnSave, 0)}
        >
          {offline && dirty ? 'Offline' : dirty ? 'Save' : 'Saved'}
        </Button>
      ) : (
        <>
          <Button
            id="graph-edit-reset"
            color="dark"
            outline
            size="sm"
            className="ms-2"
            onClick={cancel}
            disabled={saving || !dirty}
          >
            {polyglot.t('RESET')}
          </Button>
          <Button
            id="graph-edit-save"
            color="success"
            size="sm"
            className="ms-2"
            onClick={btnSave}
            disabled={saving || !dirty}
            style={{ width: '3.5rem' }}
          >
            {saving
              ? polyglot.t('SAVING')
              : saved && !dirty
                ? polyglot.t('SAVED')
                : polyglot.t('SAVE')}
          </Button>
        </>
      )}
    </div>
  ) : (
    <label
      id="graph-edit-status"
      className="btn-sm mb-0 status-viewing"
      htmlFor="narrative-you-toggle"
    >
      {role ?? 'Editing'}
    </label>
  );
};
