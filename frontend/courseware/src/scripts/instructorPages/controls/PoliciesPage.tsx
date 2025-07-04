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

import AssessmentPolicies from './AssessmentPolicies';
import DueDateAccommodations from './DueDateAccommodations';
import DueDatePolicies from './DueDatePolicies';

const PoliciesPage = () => (
  <div className="d-block d-lg-flex flex-row-content align-items-start mt-3">
    <div className="flex-col-fluid">
      <DueDatePolicies />
      <DueDateAccommodations />
    </div>
    <div className="flex-col-fluid">
      <AssessmentPolicies />
    </div>
  </div>
);

export default PoliciesPage;
