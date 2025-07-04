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

import { openModal } from '../modals/modalActions';
import { ModalIds } from '../modals/modalIds';
import { NodeName, TypeId } from '../types/asset';
import { QUIZ_TYPE_IDS, importExtensions } from './importConstants';

export const openImportModal = (assetType: TypeId, data: { file: File; name?: NodeName }) => {
  return dispatch => {
    const isCourse = assetType === 'course.1';
    const isModule = assetType === 'module.1';
    const isLesson = assetType === 'lesson.1';
    const extension = data.file.name.replace(/^.*\./, '.').toLowerCase();
    if (isCourse && importExtensions.csv.includes(extension)) {
      dispatch(openModal(ModalIds.TemplateImport, data));
    } else if ((isCourse || isModule || isLesson) && importExtensions.zip.includes(extension)) {
      dispatch(openModal(ModalIds.HtmlZipImport, data));
    } else if ((isModule || isLesson) && importExtensions.html.includes(extension)) {
      dispatch(openModal(ModalIds.HtmlDocImport, data));
    } else if (
      QUIZ_TYPE_IDS.includes(assetType) &&
      (importExtensions.qti.includes(extension) || importExtensions.loqi.includes(extension))
    ) {
      dispatch(openModal(ModalIds.AssessmentImport, data));
    }
  };
};
