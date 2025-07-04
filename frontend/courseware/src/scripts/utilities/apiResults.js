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

/*
  At some point in history people decided to adopt a strange format
  for a list of things from an api call:
    an array with 3 custom fields on them,
    count, filterCount, totalCount
  This means that if we get results in a different format
  or if we want to operate on the results before we return it
  from the api services, we need to make sure the end result
  is still in that format.
*/

/*
  make the new results format:
    { total, partialResults: [...data] }
  look like the format we are used to.
*/
export const forPartialResults = ({ total, partialResults }) => {
  const results = partialResults.slice();
  results.count = partialResults.length;
  results.filterCount = total;
  results.totalCount = total;
  return results;
};

/*
  wrap operations over the list of results and keep all the counts
  e.g. instead of
    callAPI().then(results => map(results, someFn))
  do
    callAPI().then(withTransform(results => map(results, someFn)))
*/
export const withTransform = transformFn => results => {
  const newResults = transformFn(results);
  newResults.count = results.count;
  newResults.filterCount = results.filterCount;
  newResults.totalCount = results.totalCount;
  return newResults;
};
