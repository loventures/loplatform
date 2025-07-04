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

import { useEffect, useMemo } from 'react';

const useDocumentTitle = (title: string | string[]) => {
  const str = useMemo(
    () => (Array.isArray(title) ? title.filter(s => !!s).join(' / ') : title),
    [title]
  );
  useEffect(() => {
    document.title = str;
    return () => {
      document.title = 'Learning Objects'; // really, that's what the Crumb bar does
    };
  }, [str]);
};

export default useDocumentTitle;
