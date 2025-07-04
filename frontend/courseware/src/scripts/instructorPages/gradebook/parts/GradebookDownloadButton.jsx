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

import { withTranslation } from '../../../i18n/translationContext';
import { lojector } from '../../../loject';

const GradebookDownloadButton = ({ translate }) => (
  <a
    className="btn btn-primary download-grades"
    target="_blank"
    title={translate('GRADEBOOK_DOWNLOAD')}
    href={lojector.get('GradebookAPI').downloadGrades()}
  >
    {translate('GRADEBOOK_DOWNLOAD')}
  </a>
);

export default withTranslation(GradebookDownloadButton);
