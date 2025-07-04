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
import { Route, Switch } from 'react-router-dom';

import DropboxActionBar from '../dropbox/DropboxActionBar';
import FeedbackDetailActionBar from '../feedback/FeedbackDetailActionBar';
import FeedbackIndexActionBar from '../feedback/FeedbackIndexActionBar';
import { ProjectsActionBar } from '../projects/ProjectsActionBar';
import RevisionActionBar from '../revision/RevisionActionBar';
import {
  contentSearchPath,
  dropboxPath,
  feedbackItemPath,
  feedbackPath,
  revisionPath,
  rootPath,
  storyPath,
} from '../router/routes';
import { NarrativeActionBar } from '../story/NarrativeActionBar';

const ActionBar: React.FC<{ stuck: boolean }> = ({ stuck }) => (
  <div
    id="action-bar"
    className="grid-actionbar border-bottom"
  >
    <Switch>
      <Route
        path={rootPath}
        exact
      >
        <ProjectsActionBar />
      </Route>
      <Route path={contentSearchPath}>
        <ProjectsActionBar label="Content Search" />
      </Route>
      <Route path={storyPath}>
        <NarrativeActionBar stuck={stuck} />
      </Route>
      <Route
        path={revisionPath}
        component={RevisionActionBar}
      />
      <Route
        path={feedbackPath}
        exact
      >
        <FeedbackIndexActionBar />
      </Route>
      <Route
        path={feedbackItemPath}
        exact
      >
        <FeedbackDetailActionBar />
      </Route>
      <Route path={dropboxPath}>
        <DropboxActionBar />
      </Route>
    </Switch>
  </div>
);

export default ActionBar;
