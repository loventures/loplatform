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

import { uniqueId } from 'lodash';
import * as React from 'react';
import { connect } from 'react-redux';
import { Prompt } from 'react-router-dom';
import { Dispatch } from 'redux';

import { trackAuthoringEvent } from '../analytics';
import { safeSaveProjectGraphEdits } from '../graphEdit';
import { openModal } from '../modals/modalActions';
import { ModalIds } from '../modals/modalIds';
import { narrativeSaveAndContinue } from '../story/storyHooks';

/* FC version:

 const SaveOnNavigate: React.FC<{ saves: Array<() => Promise<void>> }> = ({ saves }) => {
  const trigger = useMemo(() => Symbol.for(`__SaveOnNavigate_${uniqueId()}`), []);

  useEffect(() => {
    window[trigger] = allowTransitionCallback => {
      ...
    };
    return () => {
      delete window[trigger];
    };
  }, [trigger, saves]);

  return (
    <Prompt
      when={true}
      message={Symbol.keyFor(trigger)}
    />
  );
};

 */

interface PreventNavAndUnsavedChangesPromptProps {
  pathname: string;
  projectGraphDirty: boolean;
  realTime: boolean;
  dispatch: Dispatch<any>;
}

//https://kamranicus.com/posts/2018-07-26-react-router-custom-transition-ui
class PreventNavAndUnsavedChangesPrompt extends React.Component<PreventNavAndUnsavedChangesPromptProps> {
  private __trigger: symbol;
  private __trigger2: symbol;

  constructor(props) {
    super(props);
    this.__trigger = Symbol.for(`__PreventNavAndUnsavedChangesModal_${uniqueId()}`);
    this.__trigger2 = Symbol.for(`__StorySaveModal_${uniqueId()}`);
  }

  componentDidMount() {
    window[this.__trigger] = allowTransitionCallback => {
      const { dispatch, projectGraphDirty, realTime } = this.props;

      if (projectGraphDirty) {
        if (realTime) {
          trackAuthoringEvent('Narrative Editor - Save', 'Autosave');
          dispatch(safeSaveProjectGraphEdits(() => allowTransitionCallback(true)));
        } else {
          const unsavedChangesConfig = {
            callback: save => {
              if (save) {
                trackAuthoringEvent(`Narrative Editor - Save`, 'Continue');
                trackAuthoringEvent('Project Graph - Dirty', 'Save');
                dispatch(safeSaveProjectGraphEdits(() => allowTransitionCallback(true)));
              } else {
                allowTransitionCallback(false);
              }
            },
          };
          dispatch(openModal(ModalIds.StorySaveCancel, unsavedChangesConfig));
        }
      } else {
        allowTransitionCallback(true);
      }
    };
    window[this.__trigger2] = allowTransitionCallback => {
      const { dispatch } = this.props;
      dispatch(narrativeSaveAndContinue(() => allowTransitionCallback(true)));
    };
  }

  componentWillUnmount() {
    delete window[this.__trigger];
    delete window[this.__trigger2];
  }

  render() {
    return (
      <Prompt
        when={true}
        message={location => {
          if (location.pathname.includes('/story/') && this.props.pathname.includes('/story/')) {
            // Navigation within the narrative editor has a low-key save modal
            // Navigating through the add next route should not autosave the transient 'Untitled' page..
            return this.props.projectGraphDirty &&
              !this.props.realTime &&
              !location.search.includes('&confirm=false')
              ? Symbol.keyFor(this.__trigger2)
              : true;
          } else {
            return Symbol.keyFor(this.__trigger);
          }
        }}
      />
    );
  }
}

const mapStateToProps = state => ({
  pathname: state.router.location.pathname,
  // Graph edit Routes
  projectGraphDirty: state.graphEdits.dirty,
  realTime: state.configuration.realTime,
});

export default connect(mapStateToProps)(PreventNavAndUnsavedChangesPrompt);
