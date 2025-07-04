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

import { formatContentDuration } from '../utils/duration.js';
import { withTranslation } from '../../i18n/translationContext.js';

const ContentSubscript = ({
  translate,
  content,
  className,
  durationHuman = content.duration && formatContentDuration(content.duration),
}) => (
  <small className={'text-nowrap ' + className}>
    {durationHuman && (
      <span title={translate('ASSET_TIME_EXPLANATION', { duration: durationHuman })}>
        {durationHuman}
      </span>
    )}
    &nbsp;<span>{translate('CONTENT_NAME_' + content.displayKey)}</span>
  </small>
);

export default withTranslation(ContentSubscript);
