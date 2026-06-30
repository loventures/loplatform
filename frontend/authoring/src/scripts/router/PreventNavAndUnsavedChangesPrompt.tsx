/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { Location } from 'history';
import * as React from 'react';
import { connect } from 'react-redux';
import { Dispatch } from 'redux';

import { trackAuthoringEvent } from '../analytics';
import { history } from '../dcmStore';
import { safeSaveProjectGraphEdits } from '../graphEdit';
import { openModal } from '../modals/modalActions';
import { ModalIds } from '../modals/modalIds';
import { narrativeSaveAndContinue } from '../story/storyHooks';

interface PreventNavAndUnsavedChangesPromptProps {
  pathname: string;
  projectGraphDirty: boolean;
  realTime: boolean;
  dispatch: Dispatch<any>;
}

/*
 * react-router v6 removed <Prompt> and history v5 removed `getUserConfirmation`, so the unsaved-work
 * guard is reimplemented with `history.block`. The original Prompt's `message` callback (which
 * returned a Symbol key resolved by getUserConfirmation to a save-modal trigger) is inlined here as
 * `confirmNav`. We stay a class so `this.props` is always current inside the long-lived blocker (a
 * hook + useEffect would capture stale props). On confirm we unblock, retry the navigation, and
 * reinstall the block so it keeps guarding subsequent transitions.
 */
class PreventNavAndUnsavedChangesPrompt extends React.Component<PreventNavAndUnsavedChangesPromptProps> {
  private unblock?: () => void;

  componentDidMount() {
    this.install();
  }

  componentWillUnmount() {
    this.unblock?.();
  }

  // Decide whether a pending navigation may proceed; calls `allow(true)` to continue (possibly after
  // saving) or `allow(false)` to cancel. Mirrors the v5 <Prompt message> + getUserConfirmation flow.
  private confirmNav(location: Location, allow: (ok: boolean) => void) {
    const { dispatch, projectGraphDirty, realTime } = this.props;
    if (location.pathname.includes('/story/') && this.props.pathname.includes('/story/')) {
      // Navigation within the narrative editor: low-key save-and-continue. Skip the transient
      // 'Untitled' add-next route (which passes &confirm=false).
      if (projectGraphDirty && !realTime && !location.search.includes('&confirm=false')) {
        dispatch(narrativeSaveAndContinue(() => allow(true)));
      } else {
        allow(true);
      }
    } else if (projectGraphDirty) {
      if (realTime) {
        trackAuthoringEvent('Narrative Editor - Save', 'Autosave');
        dispatch(safeSaveProjectGraphEdits(() => allow(true)));
      } else {
        dispatch(
          openModal(ModalIds.StorySaveCancel, {
            callback: (save: boolean) => {
              if (save) {
                trackAuthoringEvent('Narrative Editor - Save', 'Continue');
                trackAuthoringEvent('Project Graph - Dirty', 'Save');
                dispatch(safeSaveProjectGraphEdits(() => allow(true)));
              } else {
                allow(false);
              }
            },
          })
        );
      }
    } else {
      allow(true);
    }
  }

  private install() {
    this.unblock = (history as any).block((tx: { location: Location; retry: () => void }) => {
      this.confirmNav(tx.location, ok => {
        if (ok) {
          this.unblock?.();
          tx.retry();
          this.install();
        }
      });
    });
  }

  render() {
    return null;
  }
}

const mapStateToProps = state => ({
  pathname: state.router.location.pathname,
  // Graph edit Routes
  projectGraphDirty: state.graphEdits.dirty,
  realTime: state.configuration.realTime,
});

export default connect(mapStateToProps)(PreventNavAndUnsavedChangesPrompt);
