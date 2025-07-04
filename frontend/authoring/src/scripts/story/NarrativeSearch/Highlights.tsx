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

import { flatten, take } from 'lodash';
import * as React from 'react';

export type SearchHighlights = Record<string, string[]>;

type SepProps = {
  children: string;
};

const Sep = ({ children }: SepProps) => <span className="text-muted"> {children} </span>;

const MAX_HIGHLIGHTS = 5;

type HighlightsProps = {
  children: SearchHighlights;
  className?: string;
};

export const Highlights: React.FC<HighlightsProps> = ({ children, className }) => {
  const matches = take(flatten(Object.values(children)), MAX_HIGHLIGHTS);
  const highlight = match => {
    const re = /\{\{\{([^}]*)\}\}\}/g;
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
