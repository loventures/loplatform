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

import {
  createSubmissionAttemptActionCreator,
  loadSubmissionActivityActionCreator,
} from './redux/submissionActivityActions.js';
import { selectSubmissionOpenAttemptLoaderComponent } from './redux/submissionActivitySelectors.js';
import { selectContentActivityLoaderComponent } from '../../../courseActivityModule/selectors/activitySelectors.js';
import { createLoaderComponent } from '../../../utilities/withLoader.js';

export const SubmissionActivityLoader = createLoaderComponent(
  selectContentActivityLoaderComponent,
  ({ content, viewingAs }) => loadSubmissionActivityActionCreator(content, viewingAs),
  'SubmissionActivity'
);

export const RefreshingSubmissionActivityLoader = createLoaderComponent(
  selectContentActivityLoaderComponent,
  ({ content, viewingAs }) => loadSubmissionActivityActionCreator(content, viewingAs),
  'SubmissionActivity',
  true
);

export const SubmissionOpenAttemptLoader = createLoaderComponent(
  selectSubmissionOpenAttemptLoaderComponent,
  ({ content, viewingAsId }) => createSubmissionAttemptActionCreator(content, viewingAsId),
  'SubmissionOpenAttempt'
);
