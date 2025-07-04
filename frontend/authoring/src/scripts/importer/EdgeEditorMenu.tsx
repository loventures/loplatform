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
import { useDispatch } from 'react-redux';
import { DropdownItem } from 'reactstrap';

import { trackOpenImportModalButton } from '../analytics/AnalyticsEvents';
import {
  confirmSaveProjectGraphEdits,
  confirmSaveProjectGraphEditsLink,
  useGraphEditSelector,
} from '../graphEdit';
import { useBranchId, usePolyglot } from '../hooks';
import { ModalIds } from '../modals/modalIds';
import { DropdownAItem } from '../story/components/DropdownAItem';
import { useContentAccess } from '../story/hooks/useContentAccess';
import { useRevisionCommit } from '../story/storyHooks';
import { NodeName, TypeId } from '../types/asset';
import { htmlDocExportUrl, htmlZipExportUrl } from './htmlTransferService';
import { QUIZ_TYPE_IDS } from './importConstants';

const structureExportUrl = (branchId: number, commit?: number): string =>
  commit
    ? `/api/v2/authoring/${branchId}/commits/${commit}/csvExport.csv`
    : `/api/v2/authoring/${branchId}/csvExport.csv`;

const assessmentsExportUrl = (branchId: number, commit?: number): string =>
  commit
    ? `/api/v2/authoring/${branchId}/commits/${commit}/assessmentExport.zip`
    : `/api/v2/authoring/${branchId}/assessmentExport.zip`;

const loqiExportUrl = (branchId: number, name: string, commit?: number): string =>
  commit
    ? `/api/v2/authoring/${branchId}/commits/${commit}/asset/${name}/export/loqi.docx`
    : `/api/v2/authoring/${branchId}/asset/${name}/export/loqi.docx`;

export const exporteousTypes = new Set<TypeId>([
  'course.1',
  'module.1',
  'lesson.1',
  ...QUIZ_TYPE_IDS,
]);

export const ImportExportItems: React.FC<{
  name: NodeName;
  typeId: TypeId;
  import?: boolean;
  export?: boolean;
  openImportModal?: (modalId: string) => void;
}> = ({ name, typeId, import: imprt, export: exprt, openImportModal }) => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();
  const branchId = useBranchId();
  const isCourse = typeId === 'course.1';
  const isModule = typeId === 'module.1';
  const isLesson = typeId === 'lesson.1';
  const isQuizLike = QUIZ_TYPE_IDS.includes(typeId);
  const commit = useRevisionCommit();
  const dirty = useGraphEditSelector(state => state.dirty);
  const contentAccess = useContentAccess(name);
  const exportDisabled = contentAccess.Export ? undefined : 'disabled';

  return (
    <>
      {exprt && isCourse && (
        <DropdownAItem
          id="export-template-button"
          target="_blank"
          href={structureExportUrl(branchId, commit)}
          onClick={e => confirmSaveProjectGraphEditsLink(e, dirty, dispatch)}
          className={exportDisabled}
        >
          {polyglot.t('EXPORT_TEMPLATE')}
        </DropdownAItem>
      )}
      {imprt && isCourse && (
        <DropdownItem
          id="import-template-button"
          onClick={() => {
            dispatch(
              confirmSaveProjectGraphEdits(() => {
                openImportModal?.(ModalIds.TemplateImport);
                trackOpenImportModalButton(typeId);
              })
            );
          }}
          disabled={!contentAccess.Import}
        >
          {polyglot.t('IMPORT_TEMPLATE')}
        </DropdownItem>
      )}
      {exprt && isQuizLike && (
        <DropdownAItem
          id="export-loqi-button"
          target="_blank"
          href={loqiExportUrl(branchId, name, commit)}
          onClick={e => confirmSaveProjectGraphEditsLink(e, dirty, dispatch)}
          className={exportDisabled}
        >
          {polyglot.t('EXPORT_LOQI')}
        </DropdownAItem>
      )}
      {imprt && isQuizLike && (
        <DropdownItem
          id="import-questions-button"
          onClick={() => {
            dispatch(
              confirmSaveProjectGraphEdits(() => {
                openImportModal?.(ModalIds.AssessmentImport);
                trackOpenImportModalButton(typeId);
              })
            );
          }}
          disabled={!contentAccess.Import}
        >
          {polyglot.t('IMPORT_QUESTIONS')}
        </DropdownItem>
      )}
      {exprt && (isModule || isLesson) && (
        <DropdownAItem
          id="export-html-doc-button"
          target="_blank"
          href={htmlDocExportUrl(branchId, name, commit)}
          onClick={e => confirmSaveProjectGraphEditsLink(e, dirty, dispatch)}
          className={exportDisabled}
        >
          {polyglot.t('EXPORT_HTML_DOC')}
        </DropdownAItem>
      )}
      {imprt && (isModule || isLesson) && (
        <DropdownItem
          id="import-html-doc-button"
          onClick={() => {
            dispatch(
              confirmSaveProjectGraphEdits(() => {
                openImportModal?.(ModalIds.HtmlDocImport);
                trackOpenImportModalButton(typeId);
              })
            );
          }}
          disabled={!contentAccess.Import}
        >
          {polyglot.t('IMPORT_HTML_DOC')}
        </DropdownItem>
      )}
      {exprt && (isCourse || isModule || isLesson) && (
        <DropdownAItem
          id="export-html-button"
          target="_blank"
          href={htmlZipExportUrl(branchId, name, commit)}
          onClick={e => confirmSaveProjectGraphEditsLink(e, dirty, dispatch)}
          className={exportDisabled}
        >
          {polyglot.t('EXPORT_HTML_ZIP')}
        </DropdownAItem>
      )}
      {imprt && (isCourse || isModule || isLesson) && (
        <DropdownItem
          id="import-html-button"
          onClick={() => {
            dispatch(
              confirmSaveProjectGraphEdits(() => {
                openImportModal?.(ModalIds.HtmlZipImport);
                trackOpenImportModalButton(typeId);
              })
            );
          }}
          disabled={!contentAccess.Import}
        >
          {polyglot.t('IMPORT_HTML_ZIP')}
        </DropdownItem>
      )}
      {exprt && isCourse && (
        <DropdownAItem
          id="export-template-button"
          target="_blank"
          href={assessmentsExportUrl(branchId, commit)}
          onClick={e => confirmSaveProjectGraphEditsLink(e, dirty, dispatch)}
          className={exportDisabled}
        >
          {polyglot.t('EXPORT_ASSESSMENTS')}
        </DropdownAItem>
      )}
    </>
  );
};
