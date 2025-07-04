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

import { useTranslation } from '../../i18n/translationContext';
import React from 'react';

export const SearchInstructions: React.FC = () => {
  const translate = useTranslation();
  return (
    <div className="mx-md-5 pt-md-4 text-muted search-instructions">
      <div className="mx-3 mb-2">Search Instructions</div>
      <dl className="mx-3 small">
        <dt>{translate('apple orange')}</dt>
        <dd>{translate('Search for documents containing any term.')}</dd>
        <dt>{translate('"orange apple"')}</dt>
        <dd>{translate('Use quotes to search for exact phrases.')}</dd>
        <dt>{translate('"extra terrestrial" + life')}</dt>
        <dd>{translate('Use plus to search for documents containing all terms.')}</dd>
        <dt>{translate('flash + -photography')}</dt>
        <dd>
          {translate(
            'Use plus and minus to search for documents containing some terms but not others.'
          )}
        </dd>
        <dt>{translate('flash + -(photography dance)')}</dt>
        <dd>{translate('Use parentheses to group terms.')}</dd>
      </dl>
    </div>
  );
};
