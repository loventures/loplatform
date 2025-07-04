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

import LoLink from '../../../components/links/LoLink';
import { SendMessagePageLink } from '../../../utils/pageLinks';
import { Translate, withTranslation } from '../../../i18n/translationContext';
import React from 'react';
import { IoMailOutline } from 'react-icons/io5';

const PageHeaderSendMessageLink: React.FC<{ translate: Translate }> = ({ translate }) => (
  <LoLink
    className="nav-item btn btn-outline-primary border-white d-none d-md-flex"
    title={translate('PAGE_HEADER_SEND_MESSAGE')}
    to={SendMessagePageLink.toLink({})}
  >
    <IoMailOutline
      size="1.5rem"
      className="thin-mail-icon"
      aria-hidden={true}
    />

    <span className="sr-only">{translate('PAGE_HEADER_SEND_MESSAGE')}</span>
  </LoLink>
);

export default withTranslation(PageHeaderSendMessageLink);
