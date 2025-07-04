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

import * as React from 'react';
import { useSelector } from 'react-redux';

import CompetencyAlignmentModal from '../competency/CompetencyAlignmentModal';
import CompetencySetImportModal from '../competency/CompetencySetImportModal';
import ContentAlignmentImportModal from '../competency/ContentAlignmentImportModal';
import AssessmentImportModal from '../importer/AssessmentImportModal';
import HtmlDocImportModal from '../importer/HtmlDocImportModal';
import HtmlZipImportModal from '../importer/HtmlZipImportModal';
import TemplateImportModal from '../importer/template/TemplateImportModal';
import { ResetContentStatusModal } from '../story/ActionMenu/ResetContentStatusModal';
import MergeUpdatesModal from '../story/modals/MergeUpdatesModal';
import OverwriteChangesModal from '../story/modals/OverwriteChangesModal';
import StorySaveCancelModal from '../story/modals/StorySaveCancelModal';
import StorySaveModal from '../story/modals/StorySaveModal';
import { DcmState } from '../types/dcmState';
import BranchLinkModal from './BranchLinkModal';
import ConfirmModal from './ConfirmModal';
import ContentHyperlinkModal from './ContentHyperlinkModal';
import DiscardChangesModal from './DiscardChangesModal';
import EBookEmbedModal from './EBookEmbedModal';
import FileAddModal from './FileAddModal';
import FindContentModal from './FindContentModal';
import GateContentModal from './GateContentModal';
import { ModalIds } from './modalIds';
import SessionTimeoutModal from './SessionTimeoutModal';

const ModalContainer = () => {
  const openModalId = useSelector((state: DcmState) => state.modal.openModalId);

  switch (openModalId) {
    case ModalIds.AssessmentImport:
      return <AssessmentImportModal />;
    case ModalIds.TemplateImport:
      return <TemplateImportModal />;
    case ModalIds.HtmlZipImport:
      return <HtmlZipImportModal />;
    case ModalIds.HtmlDocImport:
      return <HtmlDocImportModal />;
    case ModalIds.CompetencySetImport:
      return <CompetencySetImportModal />;
    case ModalIds.ContentAlignmentImport:
      return <ContentAlignmentImportModal />;
    case ModalIds.Confirm:
      return <ConfirmModal />;
    case ModalIds.StorySave:
      return <StorySaveModal />;
    case ModalIds.StorySaveCancel:
      return <StorySaveCancelModal />;
    case ModalIds.OverwriteChanges:
      return <OverwriteChangesModal />;
    case ModalIds.DiscardChanges:
      return <DiscardChangesModal />;
    case ModalIds.MergeUpdates:
      return <MergeUpdatesModal />;
    case ModalIds.SessionTimeout:
      return <SessionTimeoutModal />;
    case ModalIds.FileAdd:
      return <FileAddModal />;
    case ModalIds.EBookEmbedModal:
      return <EBookEmbedModal />;
    case ModalIds.CompetencyAlignment:
      return <CompetencyAlignmentModal />;
    case ModalIds.ContentHyperlinkModal:
      return <ContentHyperlinkModal />;
    case ModalIds.GateContentModal:
      return <GateContentModal />;
    case ModalIds.FindContentModal:
      return <FindContentModal />;
    case ModalIds.BranchLinkModal:
      return <BranchLinkModal />;
    case ModalIds.ResetContentStatus:
      return <ResetContentStatusModal />;
    default:
      return null;
  }
};

export default ModalContainer;
