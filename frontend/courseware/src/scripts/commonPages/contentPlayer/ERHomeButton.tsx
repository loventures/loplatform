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
import { FiHome } from 'react-icons/fi';

const ERHomeButton: React.FC<{ dropdown?: boolean }> = ({ dropdown }) => {
  const translate = useTranslation();
  const title = dropdown ? undefined : translate('ER_HOME_TITLE');
  return (
    <LoLink
      id="home-button"
      key={'home'}
      className={dropdown ? 'dropdown-item' : 'btn btn-outline-primary border-white px-2'}
      style={dropdown ? { textDecoration: undefined } : {}}
      aria-label={title}
      title={title}
      to={CourseDashboardLink.toLink()}
    >
      {dropdown ? (
        translate('ER_HOME_TITLE')
      ) : (
        <FiHome
          size="2rem"
          strokeWidth={1.1}
          aria-hidden={true}
        />
      )}
    </LoLink>
  );
};

export default ERHomeButton;
