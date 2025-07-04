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

import { minimalise } from '../../commonPages/contentPlayer/ERContentTitle';
import { ContentWithAncestors } from '../../resources/LearningPathResource';
import { flatten, take } from 'lodash';
import ContentLink from '../../contentPlayerComponents/parts/ContentLink';
import { ViewingAs } from '../../courseContentModule/selectors/contentEntry';
import React from 'react';

import { SearchHighlights } from './search';

export const SearchResult: React.FC<{
  content: ContentWithAncestors;
  highlights: SearchHighlights;
  viewingAs: ViewingAs;
}> = ({ content, highlights, viewingAs }) => {
  const path = minimalise(content.module?.name, content.lesson?.name).join(' / ');
  return (
    <li className="search-hit">
      <div className="small text-muted">{path}</div>
      <div className="search-title">
        <ContentLink
          content={content}
          viewingAs={viewingAs}
          disableSummary
        >
          {content.name}
        </ContentLink>
      </div>
      <Highlights className="mt-2 search-highlights">{highlights}</Highlights>
    </li>
  );
};

const MAX_HIGHLIGHTS = 5;

const Highlights: React.FC<{
  children: SearchHighlights;
  className?: string;
}> = ({ children, className }) => {
  const matches = take(flatten(Object.values(children as SearchHighlights)), MAX_HIGHLIGHTS);
  const highlight = (match: string) => {
    const re = /\{\{\{([^}]*)}}}/g;
    const result = [];
    let index = 0;
    let hi;
    while ((hi = re.exec(match)) !== null) {
      result.push(
        <React.Fragment key={index}>
          {match.substring(index, hi.index)}
          <span className="highlighter">{hi[1]}</span>
        </React.Fragment>
      );
      index = re.lastIndex;
    }
    result.push(<span key={index}>{match.substring(index)}</span>);
    return result;
  };
  return (
    <div className={className}>
      {matches.map((match, index) => (
        <React.Fragment key={index}>
          {index > 0 && <Sep>&hellip;</Sep>}
          {highlight(match)}
        </React.Fragment>
      ))}
    </div>
  );
};

export const Sep: React.FC<{ children: string }> = ({ children }) => (
  <span className="text-muted"> {children} </span>
);
