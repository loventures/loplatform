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

import { useDocumentTitle } from '../hooks';
import { QuillMenu } from '../story/NarrativeActionBar/QuillMenu';
import FeedbackFilters from './FeedbackFilters';

const FeedbackIndexActionBar: React.FC = () => {
  useDocumentTitle('Feedback');
  return (
    <div className="d-flex align-items-center h-100 px-3 narrative-action-bar">
      <h6 className="m-0 flex-grow-1 d-flex align-items-center">
        <QuillMenu />
        Feedback
      </h6>
      <FeedbackFilters right />
    </div>
  );
};

export default FeedbackIndexActionBar;
