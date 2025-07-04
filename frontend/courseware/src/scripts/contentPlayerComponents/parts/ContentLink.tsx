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

import { Content } from '../../api/contentsApi.ts';
import LoLink from '../../components/links/LoLink.tsx';
import { ContentPlayerPageLink, LearnerCompetencyPlayerPageLink } from '../../utils/pageLinks.ts';
import { ViewingAs } from '../../courseContentModule/selectors/contentEntry';
import { selectPageCompetency } from '../../courseContentModule/selectors/contentEntrySelectors.ts';
import { useTranslation } from '../../i18n/translationContext.tsx';
import React, { CSSProperties, RefObject } from 'react';
import { useSelector } from 'react-redux';

import { summarizeToLabel } from '../utils/summarizeToLabel';

type ContentLinkProps = {
  content: Content;
  viewingAs: ViewingAs;
  linkRef?: RefObject<HTMLAnchorElement>;

  className?: string;
  style?: CSSProperties;
  hidden?: boolean;
  disabled?: boolean;
  disableSummary?: boolean;
  external?: boolean;
  nav?: string;
} & React.PropsWithChildren;

const ContentLink: React.FC<ContentLinkProps> = ({
  content,
  className,
  style,
  viewingAs,

  hidden,
  disabled,
  external,
  children,
  disableSummary,

  linkRef,
  nav,
}) => {
  const translate = useTranslation();
  const competency = useSelector(selectPageCompetency);
  // const competencyId = competency && competency.id;
  // const course = useSelector((state: CourseState) => state.course);
  return (
    <LoLink
      className={className}
      style={style}
      to={
        competency
          ? LearnerCompetencyPlayerPageLink.toLink({ content, competency })
          : ContentPlayerPageLink.toLink({ content, nav })
      }
      hidden={hidden}
      disabled={disabled}
      title={disableSummary ? void 0 : summarizeToLabel(translate, content, viewingAs)}
      aria-label={disableSummary ? void 0 : summarizeToLabel(translate, content, viewingAs)}
      target={external ? '_blank' : void 0}
      children={children}
      linkRef={linkRef}
    />
  );
};

export default ContentLink;
