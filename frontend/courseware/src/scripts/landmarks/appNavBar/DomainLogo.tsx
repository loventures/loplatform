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

import { createLink } from '../../utils/linkUtils';
import { Translate, withTranslation } from '../../i18n/translationContext';
import { customLogoUrl } from '../../utilities/preferences';
import { selectRouter } from '../../utilities/rootSelectors';
import qs from 'qs';
import React, { useState } from 'react';
import { connect } from 'react-redux';

import { interpolateCustomLink } from './utils/customLinkUtils';

export type DomainLogoProps = {
  translate: Translate;
  searchParams: qs.ParsedQs;
};

const DomainLogo: React.FC<DomainLogoProps> = ({ translate, searchParams }) => {
  const [brokenImage, setBrokenImage] = useState(false);

  const domainLogoSrc =
    (customLogoUrl && !brokenImage && interpolateCustomLink(customLogoUrl)) ||
    window.lo_platform.domain.logo?.url;

  const domainLogoSrc2 = window.lo_platform.domain.logo2?.url;

  const domainLogoLink = createLink(searchParams.contentItemRoot ? '#/' : '/');
  const domainLogoClass = domainLogoSrc ? 'domain-logo' : 'default-logo';
  return (
    <a
      className={'lo-domain-link base-logo ' + domainLogoClass}
      title={translate('DOMAIN_LOGO_LINK_NAME')}
      href={domainLogoLink.pathname}
    >
      {!domainLogoSrc ? (
        <span>{window.lo_platform.domain.name}</span>
      ) : (
        <img
          src={domainLogoSrc}
          alt={window.lo_platform.domain.shortName}
          onError={() => setBrokenImage(true)}
          className={domainLogoSrc2 && 'dark-only'}
        />
      )}
      {domainLogoSrc2 && (
        <img
          src={domainLogoSrc2}
          alt={window.lo_platform.domain.shortName}
          className="light-only"
        />
      )}
    </a>
  );
};

export default withTranslation(connect(selectRouter)(DomainLogo));
