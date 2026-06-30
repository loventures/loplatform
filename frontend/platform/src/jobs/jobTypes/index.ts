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

import Polyglot from 'node-polyglot';
import React from 'react';

import AssessmentStatistics from './AssessmentStatistics';
import AssetExportStorage from './AssetExportStorage';
import GarbageCollectProjects from './GarbageCollectProjects';
import LtiUsageStatistics from './LtiUsageStatistics';
import InstructorMetricsReport from './InstructorMetricsReport';
import NodeAttachmentBlobRefUpgrade from './NodeAttachmentBlobRefUpgrade';
import QnaReportJob from './QnaReportJob';
import ScriptedReport from './ScriptedReport';
import StudentCompletionJob from './StudentCompletionJob';

// the job entity is a free-form, schema-dependent record
export type JobRow = Record<string, any>;
export type ValidationErrors = Record<string, string>;

export interface JobTypeProps {
  T: Polyglot;
  row: JobRow;
  validationErrors: ValidationErrors;
}

export interface ValidatorResult {
  validationErrors?: ValidationErrors;
  data?: Record<string, any>;
}

export type JobValidator = (form: JobRow, T: Polyglot) => ValidatorResult;

export interface JobTypeEntry {
  component: React.ComponentType<JobTypeProps>;
  validator: JobValidator;
}

const jobTypes: Record<string, JobTypeEntry> = {
  [AssessmentStatistics.id]: {
    component: AssessmentStatistics.component,
    validator: AssessmentStatistics.validator,
  },
  [NodeAttachmentBlobRefUpgrade.id]: {
    component: NodeAttachmentBlobRefUpgrade.component,
    validator: NodeAttachmentBlobRefUpgrade.validator,
  },
  [AssetExportStorage.id]: {
    component: AssetExportStorage.component,
    validator: AssetExportStorage.validator,
  },
  [ScriptedReport.id]: {
    component: ScriptedReport.component,
    validator: ScriptedReport.validator,
  },
  [StudentCompletionJob.id]: {
    component: StudentCompletionJob.component,
    validator: StudentCompletionJob.validator,
  },
  [GarbageCollectProjects.id]: {
    component: GarbageCollectProjects.component,
    validator: GarbageCollectProjects.validator,
  },
  [LtiUsageStatistics.id]: {
    component: LtiUsageStatistics.component,
    validator: LtiUsageStatistics.validator,
  },
  [InstructorMetricsReport.id]: {
    component: InstructorMetricsReport.component,
    validator: InstructorMetricsReport.validator,
  },
  [QnaReportJob.id]: {
    component: QnaReportJob.component,
    validator: QnaReportJob.validator,
  },
};

export default jobTypes;
