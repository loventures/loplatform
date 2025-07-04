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

import { connect } from 'react-redux';
import { withTranslation } from '../../i18n/translationContext';

import { selectStudentPreviewBannerComponent } from './selectors';

import { openLearnerPreviewPickerActionCreator, exitPreviewActionCreator } from './actions';

const LearnerPreviewBanner = ({ translate, isPreviewing, viewingAs, openPicker, exitPreview }) =>
  isPreviewing && (
    <div className="alert alert-danger mb-0 w-10 student-preview-alert">
      <div className="flex-row-content">
        <button
          className="alert-pick-user btn-link border-0 flex-col-fluid"
          onClick={openPicker}
        >
          <span>
            {translate('STUDENT_PREVIEW_LABEL', {
              userName: viewingAs.fullName,
            })}
          </span>
          <i className="icon icon-chevron-down"></i>
        </button>
        <button
          className="icon-btn close icon-cancel-circle text-primary"
          onClick={exitPreview}
          title={translate('EXIT_STUDENT_PREVIEW')}
        ></button>
      </div>
    </div>
  );

export default connect(selectStudentPreviewBannerComponent, {
  openPicker: openLearnerPreviewPickerActionCreator,
  exitPreview: exitPreviewActionCreator,
})(withTranslation(LearnerPreviewBanner));
