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

import { sumBy } from 'lodash';
import React, { useMemo } from 'react';
import { Col, Row } from 'reactstrap';

import { usePolyglot } from '../hooks';
import { Stornado } from '../story/badges/Stornado';
import { NewAsset, NodeName } from '../types/asset';
import {
  formatPercent,
  nanZero,
  useEditedFlatAssignments,
  useEditedGradebookCategories,
} from './gradebook';

const CategoriesSection: React.FC = () => {
  const categories = useEditedGradebookCategories();
  const assignments = useEditedFlatAssignments();

  const categoryCounts = useMemo(() => {
    const acc: Record<NodeName | '' | 'warn', number> = {};
    for (const [asset, category] of assignments) {
      acc[category ?? ''] = (acc[category ?? ''] ?? 0) + 1;
      if (asset.data.isForCredit && !category) {
        acc.warn = (acc.warn ?? 0) + 1;
      }
    }
    return acc;
  }, [assignments]);

  const polyglot = usePolyglot();
  const totalWeight = sumBy(categories, cat => nanZero(cat.data.weight));
  return (
    <>
      <Row
        className="fw-bold mb-1"
        style={{ borderBottom: '1px solid #ccc' }}
      >
        <Col sm={8}>
          <span className="input-padding">{polyglot.t('GRADEBOOK_CATEGORY_TITLE')}</span>
        </Col>
        <Col sm={1}>
          <span className="input-padding">{polyglot.t('GRADEBOOK_CATEGORY_WEIGHT')}</span>
        </Col>
        <Col sm={3}>
          <span className="input-padding">{polyglot.t('GRADEBOOK_CATEGORY_ASSIGNMENTS')}</span>
        </Col>
      </Row>
      {categories.map(cat => (
        <CategoryRow
          key={cat.name}
          category={cat}
          assigmentCount={categoryCounts[cat.name] ?? 0}
          totalWeight={totalWeight}
        />
      ))}
      <Row className="gradebook-uncategory">
        <Col sm={8}>
          <span className="input-padding text-muted">{polyglot.t('GRADEBOOK_UNCATEGORIZED')}</span>
        </Col>
        <Col sm={{ size: 3, offset: 1 }}>
          <span className="input-padding">
            {categoryCounts[''] ?? 0}
            {categoryCounts.warn > 0 && (
              <span className="text-danger ms-2">
                {polyglot.t('GRADEBOOK_CATEGORY_WARNING', { count: categoryCounts.warn })}
              </span>
            )}
          </span>
        </Col>
      </Row>
    </>
  );
};

const CategoryRow: React.FC<{
  category: NewAsset<'gradebookCategory.1'>;
  assigmentCount: number;
  totalWeight: number;
}> = ({ category, assigmentCount, totalWeight }) => (
  <Row className="gradebook-category">
    <Col sm={8}>
      <div className="input-padding mw-100 d-flex align-items-center">
        <div className="text-truncate">{category.data.title}</div>
        <Stornado
          name={category.name}
          size="sm"
        />
      </div>
    </Col>
    <Col sm={1}>
      <span className="input-padding">
        {formatPercent(!totalWeight ? undefined : category.data.weight / totalWeight)}
      </span>
    </Col>
    <Col sm={3}>
      <span className="input-padding">{assigmentCount}</span>
    </Col>
  </Row>
);

export default CategoriesSection;
