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

import { groupBy, mapValues, sumBy } from 'lodash';
import React, { useMemo, useState } from 'react';
import { GiCheckMark } from 'react-icons/gi';
import { useDispatch } from 'react-redux';
import { Link } from 'react-router-dom';
import {
  Col,
  Input,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  Row,
  UncontrolledDropdown,
  FormGroup,
} from 'reactstrap';

import {
  addProjectGraphEdge,
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  computeEditedOutEdges,
  deleteProjectGraphEdge,
  editProjectGraphNodeData,
  useEditedAsset,
  useGraphEdits,
} from '../graphEdit';
import { usePolyglot } from '../hooks';
import { Stornado } from '../story/badges/Stornado';
import { editorUrl } from '../story/story';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { NewAsset, NodeName } from '../types/asset';
import { NewAssetWithEdge, NewEdge } from '../types/edge';
import {
  blurEnter,
  nanZero,
  useEditedFlatAssignments,
  useEditedGradebookCategories,
} from './gradebook';

// TODO: A spinner while the structure loads.. it should be a resource

// TODO: Do attempts and grading policy belong here?

const pathTitles = (path: NewAsset<any>[]) =>
  path.map(asset => asset.data.title.replace(/:.*/, ''));

const AssignmentsSection: React.FC<{
  userCanEdit: boolean;
}> = ({ userCanEdit }) => {
  const polyglot = usePolyglot();

  const categories = useEditedGradebookCategories();

  const flatContents = useEditedFlatAssignments();

  const categoryNames = useMemo(
    () =>
      mapValues(
        groupBy(categories, category => category.name),
        assets => assets[0].data.title
      ),
    [categories]
  );

  const [creditType, setCreditType] = useState<'ANY_CREDIT' | 'FOR_CREDIT' | 'NO_CREDIT'>(
    'ANY_CREDIT'
  );

  const [category, setCategory] = useState<'ANY_CATEGORY' | 'UNCATEGORIZED' | NodeName>(
    'ANY_CATEGORY'
  );

  const [filter, setFilter] = useState('');

  const filteredContents = useMemo(
    () =>
      flatContents.filter(
        ([asset, cat]) =>
          (creditType === 'ANY_CREDIT' ||
            (creditType === 'FOR_CREDIT') === asset.data.isForCredit) &&
          (category === 'ANY_CATEGORY' ||
            (category === 'UNCATEGORIZED' ? !cat : cat === category)) &&
          (!filter || asset.data.title.toLowerCase().includes(filter.toLowerCase()))
      ),
    [flatContents, creditType, category, filter]
  );

  const total = useMemo(
    () =>
      sumBy(flatContents, ([asset]) => (asset.data.isForCredit && asset.data.pointsPossible) || 0),
    [flatContents]
  );

  const hasCategories = !!categories.length;

  return (
    <>
      <div className="d-flex align-items-baseline justify-content-between">
        <h3 className="my-4">{polyglot.t('GRADEBOOK_ASSIGNMENTS_TITLE')}</h3>
        <div
          className="d-flex"
          style={{ gap: '.25rem' }}
        >
          <Input
            type="search"
            id="name-filter"
            value={filter}
            onChange={e => setFilter(e.target.value)}
            bsSize="sm"
            style={{
              borderRadius: 'calc(0.75em + 0.375rem + 1px)',
              paddingLeft: '0.75rem',
              width: '12rem',
            }}
            placeholder={polyglot.t('GRADEBOOK_FILTER_NAME')}
          />
          <UncontrolledDropdown className="credit-dropdown">
            <DropdownToggle
              id="credit-filter"
              color="light"
              size="sm"
              caret
            >
              {polyglot.t(
                creditType === 'ANY_CREDIT'
                  ? 'GRADEBOOK_FILTER_CREDIT_TYPE'
                  : `GRADEBOOK_FILTER_${creditType}`
              )}
            </DropdownToggle>
            <DropdownMenu end>
              {(['ANY_CREDIT', 'FOR_CREDIT', 'NO_CREDIT'] as const).map(s => (
                <DropdownItem
                  key={s}
                  onClick={() => setCreditType(s)}
                  disabled={s === creditType}
                >
                  {polyglot.t(`GRADEBOOK_FILTER_${s}`)}
                </DropdownItem>
              ))}
            </DropdownMenu>
          </UncontrolledDropdown>
          {hasCategories && (
            <UncontrolledDropdown className="category-dropdown">
              <DropdownToggle
                id="category-filter"
                color="light"
                size="sm"
                caret
              >
                {polyglot.t(
                  category === 'ANY_CATEGORY'
                    ? 'GRADEBOOK_FILTER_CATEGORY_TYPE'
                    : category === 'UNCATEGORIZED'
                      ? `GRADEBOOK_FILTER_${category}`
                      : categoryNames[category]
                )}
              </DropdownToggle>
              <DropdownMenu end>
                {(['ANY_CATEGORY', 'UNCATEGORIZED'] as const).map(s => (
                  <DropdownItem
                    key={s}
                    onClick={() => setCategory(s)}
                    disabled={s === category}
                  >
                    {polyglot.t(`GRADEBOOK_FILTER_${s}`)}
                  </DropdownItem>
                ))}
                {categories.map(c => (
                  <DropdownItem
                    key={c.name}
                    onClick={() => setCategory(c.name)}
                    disabled={c.name === category}
                  >
                    {c.data.title}
                  </DropdownItem>
                ))}
              </DropdownMenu>
            </UncontrolledDropdown>
          )}
        </div>
      </div>
      <Row
        className="fw-bold"
        style={{ borderBottom: '1px solid #ccc' }}
      >
        <Col sm={hasCategories ? 7 : 10}>
          <span className="input-padding">{polyglot.t('GRADEBOOK_ASSIGNMENT_TITLE')}</span>
        </Col>
        <Col sm={1}>
          <span className="input-padding">{polyglot.t('GRADEBOOK_ASSIGNMENT_FOR_CREDIT')}</span>
        </Col>
        <Col sm={1}>
          <span className="input-padding">{polyglot.t('GRADEBOOK_ASSIGNMENT_POINTS')}</span>
        </Col>
        {hasCategories && (
          <Col sm={3}>
            <span className="input-padding">{polyglot.t('GRADEBOOK_ASSIGNMENT_CATEGORY')}</span>
          </Col>
        )}
      </Row>
      {filteredContents.map(([asset, categoryName, path, edgeName]) => (
        <AssignmentRow
          key={edgeName}
          asset={asset}
          path={path}
          category={categoryName}
          categories={categories}
          userCanEdit={userCanEdit}
        />
      ))}
      {!hasCategories && (
        <Row className="gradebook-assignment-total py-2">
          <Col sm={{ size: 1, offset: 11 }}>
            <div className="input-padding fw-bold">{total}</div>
          </Col>
        </Row>
      )}
    </>
  );
};

const AssignmentRow: React.FC<{
  asset: NewAsset<any>;
  path: NewAsset<any>[];
  category: NodeName | undefined;
  categories: NewAssetWithEdge<'gradebookCategory.1'>[];
  userCanEdit: boolean;
}> = ({ asset, path, category, categories, userCanEdit }) => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();
  const hasCategories = !!categories.length;
  const projectGraph = useProjectGraph();
  const graphEdits = useGraphEdits();
  const { branchId } = projectGraph;
  const course = useEditedAsset(projectGraph.homeNodeName);

  return (
    <Row
      style={{ borderBottom: '1px solid #dddd' }}
      className="gradebook-assignment"
    >
      <Col sm={hasCategories ? 7 : 10}>
        <div className="input-padding mw-100">
          <div className="text-muted small text-truncate">
            {[...pathTitles(path), polyglot.t(asset.typeId)].join(' / ')}
          </div>
          <div className="d-flex align-items-center">
            <Link
              className="text-truncate"
              to={editorUrl('story', branchId, asset, [course, ...path])}
            >
              {asset.data.title}
            </Link>
            <Stornado
              name={asset.name}
              size="sm"
            />
          </div>
        </div>
      </Col>
      <Col
        sm={1}
        className="d-flex align-items-center"
      >
        <FormGroup
          switch
          className="input-padding"
        >
          {userCanEdit ? (
            <Input
              id={`credit-${asset.name}`}
              type="switch"
              checked={asset.data.isForCredit}
              className="assignment-credit-switch ms-0"
              onChange={e => {
                dispatch(beginProjectGraphEdit('Change assignment credit', asset.name));
                dispatch(
                  editProjectGraphNodeData(asset.name, {
                    isForCredit: e.target.checked,
                  })
                );
                dispatch(autoSaveProjectGraphEdits());
              }}
            />
          ) : asset.data.isForCredit ? (
            <GiCheckMark className="ms-3" />
          ) : null}
        </FormGroup>
      </Col>
      <Col
        sm={1}
        className="d-flex align-items-center"
      >
        {userCanEdit ? (
          <Input
            className="secret-input assignment-points-editor"
            type="number"
            value={asset.data.pointsPossible}
            onChange={e => {
              dispatch(beginProjectGraphEdit('Change assignment points', asset.name));
              dispatch(
                editProjectGraphNodeData(asset.name, {
                  pointsPossible: nanZero(parseFloat(e.target.value)),
                })
              );
            }}
            size={4}
            style={{ width: '5rem' }}
            invalid={isNaN(asset.data.pointsPossible)}
            onKeyDown={blurEnter}
            onBlur={() => {
              // morally this ought to have the usual escape/enter behaviour
              dispatch(autoSaveProjectGraphEdits());
            }}
          />
        ) : (
          <div className="input-padding">{asset.data.pointsPossible}</div>
        )}
      </Col>
      {hasCategories && (
        <Col
          sm={3}
          className="d-flex align-items-center"
        >
          <Input
            className="secret-input assignment-category-select"
            type="select"
            value={category ?? ''}
            onChange={e => {
              dispatch(beginProjectGraphEdit('Change assignment category', asset.name));
              const newCategory = categories.find(cat => cat.name === e.target.value);
              for (const catEdge of computeEditedOutEdges(
                asset.name,
                'gradebookCategory',
                projectGraph,
                graphEdits
              )) {
                dispatch(deleteProjectGraphEdge(catEdge));
              }
              if (newCategory) {
                const newEdge: NewEdge = {
                  name: crypto.randomUUID(),
                  group: 'gradebookCategory',
                  sourceName: asset.name,
                  targetName: newCategory.name,
                  data: {},
                  traverse: false,
                  newPosition: 'end',
                };
                dispatch(addProjectGraphEdge(newEdge));
              }
              dispatch(autoSaveProjectGraphEdits());
            }}
            invalid={asset.data.isForCredit && !category}
            disabled={!userCanEdit}
          >
            <option value="">{polyglot.t('GRADEBOOK_UNCATEGORIZED')}</option>
            {categories.map(cat => (
              <option
                key={cat.name}
                value={cat.name}
              >
                {cat.data.title}
              </option>
            ))}
          </Input>
        </Col>
      )}
    </Row>
  );
};

export default AssignmentsSection;
