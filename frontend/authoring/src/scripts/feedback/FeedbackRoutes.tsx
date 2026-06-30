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
import { Route, Routes, useParams } from 'react-router-dom';

import FeedbackDetail from './FeedbackDetail';
import FeedbackIndex from './FeedbackIndex';

// Descendant routes (mounted under DcmApp's `feedback/*`), so paths are relative to feedbackPath.
const FeedbackDetailRoute: React.FC = () => {
  const { feedback } = useParams<{ feedback: string }>();
  return <FeedbackDetail id={parseInt(feedback!)} />;
};

const FeedbackRoutes: React.FC = () => {
  return (
    <Routes>
      <Route
        index
        element={<FeedbackIndex />}
      />
      <Route
        path=":feedback"
        element={<FeedbackDetailRoute />}
      />
    </Routes>
  );
};

export default FeedbackRoutes;
