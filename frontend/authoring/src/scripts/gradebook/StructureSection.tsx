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

import classNames from 'classnames';
import { max, sumBy } from 'lodash';
import React from 'react';

import edgeRuleConstants from '../editor/EdgeRuleConstants';
import { useEditedTargets } from '../graphEdit';
import { usePolyglot } from '../hooks';
import { NewAsset } from '../types/asset';
import {
  formatPercent,
  nanZero,
  useEditedFlatAssignments,
  useEditedGradebookCategories,
} from './gradebook';

const StructureSection: React.FC = () => {
  const polyglot = usePolyglot();
  const categories = useEditedGradebookCategories();
  const flatContents = useEditedFlatAssignments();

  const totalWeight = sumBy(categories, cat => nanZero(cat.data.weight));

  return (
    <>
      <h3 className="mt-5 mb-4">{polyglot.t('GRADEBOOK_STRUCTURE_TITLE')}</h3>
      {[...categories, undefined].map(category => {
        const categoryContents = flatContents.filter(
          ([, categoryName]) => categoryName === category?.name
        );
        const gradedContents = categoryContents.filter(([asset]) => asset.data.isForCredit);
        const totalPoints = sumBy(gradedContents, ([asset]) => asset.data.pointsPossible);
        return (
          <div
            key={category?.name ?? 'uncategory'}
            className="gradebook-structure-category"
          >
            <div className="d-flex align-items-baseline">
              <h4>{category?.data.title ?? polyglot.t('GRADEBOOK_UNCATEGORIZED')}</h4>
              {category && (
                <div className="ms-2 text-muted">
                  {[
                    polyglot.t('GRADEBOOK_STRUCTURE_POINTS', { points: category.data.weight }),
                    polyglot.t('GRADEBOOK_STRUCTURE_PERCENT_OF_OVERALL', {
                      percent: formatPercent(category.data.weight / totalWeight),
                    }),
                  ].join(', ')}
                </div>
              )}
            </div>
            {!gradedContents.length && (
              <div
                className={classNames(
                  'ms-4',
                  category ? 'mb-3' : 'mb-2',
                  'text-muted',
                  'gb-no-assignments'
                )}
              >
                {polyglot.t('GRADEBOOK_STRUCTURE_NO_ASSIGNMENTS')}
              </div>
            )}
            {gradedContents.map(([asset, , , edgeName]) => (
              <AssignmentRow
                key={edgeName}
                asset={asset}
                totalPoints={totalPoints}
                category={category}
              />
            ))}
          </div>
        );
      })}
    </>
  );
};

const AssignmentRow: React.FC<{
  asset: NewAsset<any>;
  totalPoints: number;
  category?: NewAsset<'gradebookCategory.1'>;
}> = ({ asset, totalPoints, category }) => {
  const polyglot = usePolyglot();
  const points = asset.data.pointsPossible;

  const rubric = useEditedTargets(asset.name, 'cblRubric', 'rubric.1')[0];
  const criteria = useEditedTargets(rubric?.name ?? '', 'criteria', 'rubricCriterion.1');
  const rubricPoints = sumBy(criteria, criterion =>
    max(criterion.data.levels.map(level => level.points))
  );
  const rubricError =
    rubric &&
    (!criteria.length ||
      criteria.some(
        criterion =>
          !criterion.data.title ||
          criterion.data.title === 'Untitled' ||
          !criterion.data.levels.length ||
          criterion.data.levels.some(level => !level.name || !level.description)
      ));

  return (
    <div className={classNames('ms-4', category ? 'mb-3' : 'mb-2', 'gb-has-assignment')}>
      {asset.data.title}
      {category && (
        <div className="text-muted">
          {[
            polyglot.t('GRADEBOOK_STRUCTURE_POINTS', { points }),
            !!rubricPoints && polyglot.t('GRADEBOOK_STRUCTURE_RUBRIC_POINTS', { rubricPoints }),
            polyglot.t('GRADEBOOK_STRUCTURE_PERCENT_OF_CATEGORY', {
              percent: formatPercent(points / totalPoints),
            }),
          ]
            .filter(s => !!s)
            .join(', ')}
          {rubricError && <span className="text-danger"> (rubric incomplete)</span>}
        </div>
      )}
    </div>
  );
};

export default StructureSection;
