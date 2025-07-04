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

import quizLoaders from '../contentPlayerDirectives/quizLoaders/index.js';
import competencyMasterySummary from '../contentPlayerDirectives/quizResultsCompetencySummary/competencyMasterySummary.js';
import discussion from '../discussion/index.js';
import feedback from '../assignmentFeedback/index.js';
import richContent from '../richContent/index.js';
import rubricGrid from '../assignmentGrade/directives/rubricGrid/rubricGrid.jsx';
import graderJumpButton from '../assignmentGrader/jumper/graderJumpButtonLight.js';
import courseActivity from '../courseActivityModule/ng.js';
import courseContent from '../courseContentModule/ng.js';
import LTI from '../lti/ng.js';

export default angular.module('lof.contentPlayerComponents', [
  courseActivity.name,
  courseContent.name,
  LTI.name,
  feedback.name,
  rubricGrid.name,
  quizLoaders.name,
  richContent.name,
  competencyMasterySummary.name,
  discussion.name,
  graderJumpButton.name,
]);
