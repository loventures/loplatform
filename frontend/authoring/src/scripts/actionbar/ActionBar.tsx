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

import React from 'react';
import { Route, Routes } from 'react-router-dom';

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
    <Routes>
      <Route
        path={rootPath}
        element={<ProjectsActionBar />}
      />
      <Route
        path={contentSearchPath}
        element={<ProjectsActionBar label="Content Search" />}
      />
      <Route
        path={`${storyPath}/*`}
        element={<NarrativeActionBar stuck={stuck} />}
      />
      <Route
        path={`${revisionPath}/*`}
        element={<RevisionActionBar />}
      />
      <Route
        path={feedbackPath}
        element={<FeedbackIndexActionBar />}
      />
      <Route
        path={feedbackItemPath}
        element={<FeedbackDetailActionBar />}
      />
      <Route
        path={`${dropboxPath}/*`}
        element={<DropboxActionBar />}
      />
    </Routes>
  </div>
);

export default ActionBar;
