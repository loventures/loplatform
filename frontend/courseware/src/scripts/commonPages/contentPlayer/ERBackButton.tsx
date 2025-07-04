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
import { usePreviousFromContent } from '../../resources/useNextUp';
import { ContentPlayerPageLink, CourseDashboardLink } from '../../utils/pageLinks';
import { useTranslation } from '../../i18n/translationContext';
import React from 'react';
import { IoIosArrowRoundBack } from 'react-icons/io';

const ERBackButton: React.FC<{ dropdown?: boolean }> = ({ dropdown }) => {
  const translate = useTranslation();
  const previous = usePreviousFromContent();

  const title = dropdown ? undefined : translate('NAVIGATE_PREVIOUS', {
    previous: previous?.name ?? translate('ER_HOME_TITLE'),
  });
  return (
    <LoLink
      id="back-button"
      key={previous?.id ?? 'home'}
      className={dropdown ? 'dropdown-item' : 'btn btn-outline-primary border-white px-2'}
      style={dropdown ? { textDecoration: undefined } : {}}
      aria-label={title}
      title={title}
      to={
        previous
          ? ContentPlayerPageLink.toLink({ content: previous })
          : CourseDashboardLink.toLink()
      }
    >
      {dropdown ? (
        translate(previous ? 'NAVIGATE_PREVIOUS_CONTENT' : 'ER_HOME_TITLE')
      ) : (
        <IoIosArrowRoundBack
          size="2rem"
          strokeWidth={1.1}
          aria-hidden={true}
        />
      )}
    </LoLink>
  );
};

export default ERBackButton;
