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

import LoLink from '../../components/links/LoLink';
import { CourseDashboardLink } from '../../utils/pageLinks';
import { useTranslation } from '../../i18n/translationContext';
import React from 'react';

const ERReturnHome = () => {
  const translate = useTranslation();
  const link = CourseDashboardLink.toLink();
  return (
    <div className="next-up flex-column-center pt-3">
      <LoLink
        to={link}
        className="btn btn-primary btn-lg"
        style={{ textDecoration: 'none' }}
      >
        {translate('RETURN_TO_DASHBOARD')}
      </LoLink>
    </div>
  );
};

export default ERReturnHome;
