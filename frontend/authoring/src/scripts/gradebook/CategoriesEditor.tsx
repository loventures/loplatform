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
import { sumBy, uniqueId } from 'lodash';
import React, { useCallback, useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Button, Col, Form, Input, Row } from 'reactstrap';

import {
  addProjectGraphEdge,
  addProjectGraphNode,
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  deleteProjectGraphEdge,
  discardProjectGraphEdit,
  editProjectGraphNodeData,
  setProjectGraphEdgeOrder,
  useAllEditedOutEdges,
} from '../graphEdit';
import { useDcmSelector, useHomeNodeName, usePolyglot } from '../hooks';
import { useEscapeOrEnterToStopEditing } from '../story/story';
import { EdgeName, NewAsset, NodeName } from '../types/asset';
import { GradebookCategory } from '../types/typeIds';
import { nanZero, useEditedFlatAssignments, useEditedGradebookCategories } from './gradebook';

type Flash = [EdgeName, 'del' | 'up' | 'down' | 'add'];

const CategoriesEditor: React.FC = () => {
  const dispatch = useDispatch();
  const polyglot = usePolyglot();
  const homeNodeName = useHomeNodeName();
  const { userCanEdit } = useDcmSelector(state => state.layout);

  const [added, setAdded] = useState(false); // has any been added
  const [flash, setFlash] = useState<Flash | undefined>(undefined); // used to visually indicate a nudge or remove
  const [name, setName] = useState(uniqueId()); // next category name

  const flatContents = useEditedFlatAssignments();
  const allEdges = useAllEditedOutEdges(homeNodeName);
  const categoryEdges = allEdges.filter(edge => edge.group === 'gradebookCategories');
  const categories = useEditedGradebookCategories();
  const categoriesInUse = useMemo(
    () => new Set(flatContents.map(([, category]) => category)),
    [flatContents]
  );

  const doFlash = (name: string, btn: 'del' | 'up' | 'down' | 'add') => {
    setFlash([name, btn]);
    setTimeout(() => setFlash(undefined), 300);
  };

  const addCategory = (title: string, weight: number) => {
    if (!title || isNaN(weight)) return;
    dispatch(beginProjectGraphEdit('Add category'));
    dispatch(
      addProjectGraphNode({
        name,
        typeId: GradebookCategory,
        data: {
          title,
          weight,
          archived: false,
        },
      })
    );
    dispatch(
      addProjectGraphEdge({
        name: crypto.randomUUID(),
        sourceName: homeNodeName,
        targetName: name,
        group: 'gradebookCategories',
        data: {},
        traverse: true,
        newPosition: 'end',
      })
    );
    dispatch(autoSaveProjectGraphEdits());

    setAdded(true);
    doFlash(name, 'add');
    setName(uniqueId());
  };

  const nudgeCategory = (from: number, to: number | -1) => {
    const edges = [...categoryEdges];
    const edge = edges.splice(from, 1)[0];
    const after = to < 0 ? [] : edges.splice(to);
    const newOrder = to < 0 ? edges : [...edges, edge, ...after];
    dispatch(
      beginProjectGraphEdit(to < 0 ? 'Remove category' : 'Reorder category', edge.targetName)
    );
    // TODO: use relative set position rather than set edge order
    if (to < 0) dispatch(deleteProjectGraphEdge(edge));
    else
      dispatch(
        setProjectGraphEdgeOrder(
          // TODO: need a SetEdgePosition write-op
          homeNodeName,
          'gradebookCategories',
          newOrder.map(edge => edge.name)
        )
      );
    dispatch(autoSaveProjectGraphEdits());
    doFlash(edge.targetName, to < 0 ? 'del' : to < from ? 'up' : 'down');
  };

  const total = useMemo(
    () => sumBy(categories, category => nanZero(category.data.weight)),
    [categories]
  );

  return (
    <>
      <Row
        className="fw-bold mb-1"
        style={categories.length ? { borderBottom: '1px solid #ccc' } : undefined}
      >
        <Col sm={8}>
          <span className="input-padding">{polyglot.t('GRADEBOOK_CATEGORY_TITLE')}</span>
        </Col>
        <Col sm={2}>
          <span className="input-padding">{polyglot.t('GRADEBOOK_CATEGORY_WEIGHT')}</span>
        </Col>
      </Row>
      {categories.map((category, index) => (
        <GradebookCategoryRow
          key={category.name}
          index={index}
          count={categories.length}
          flash={flash}
          category={category}
          userCanEdit={userCanEdit}
          nudgeCategory={nudgeCategory}
          categoryInUse={s => categoriesInUse.has(s)}
        />
      ))}
      {categories.length > 0 && (
        <Row
          className="fw-bold mb-3"
          style={{ borderTop: '1px solid #ccc' }}
        >
          <Col sm={{ size: 2, offset: 8 }}>
            <span className="input-padding category-total">{total}</span>
          </Col>
        </Row>
      )}
      <AddGradebookCategoryRow
        key={name}
        userCanEdit={userCanEdit}
        autoFocus={added || !categories.length}
        addCategory={addCategory}
      />
    </>
  );
};

const GradebookCategoryRow: React.FC<{
  index: number;
  count: number;
  category: NewAsset<GradebookCategory>;
  flash?: Flash;
  userCanEdit: boolean;
  nudgeCategory: (from: number, to: number | -1) => void;
  categoryInUse: (category: NodeName) => boolean;
}> = ({ index, count, category, flash, userCanEdit, nudgeCategory, categoryInUse }) => {
  const dispatch = useDispatch();
  const flashOff = (btn: string) => !!flash && (flash[0] !== category.name || flash[1] !== btn);

  const endEditing = useCallback((e?: React.SyntheticEvent) => {
    (e?.target as HTMLElement)?.blur();
    dispatch(autoSaveProjectGraphEdits());
  }, []);

  const finishEditing = useCallback(
    (enter: boolean, e: React.KeyboardEvent) => {
      if (enter) {
        endEditing(e);
      } else {
        dispatch(discardProjectGraphEdit(category.name));
        (e?.target as HTMLElement)?.blur();
      }
    },
    [endEditing, category.name]
  );

  const keyHandler = useEscapeOrEnterToStopEditing(finishEditing);

  return (
    <Row
      className={classNames(
        'mb-1 flashy-box-shadow gradebook-edit-category',
        flash?.[0] === category.name && 'flash'
      )}
    >
      <Col
        sm={8}
        className="d-flex align-items-baseline"
      >
        <Input
          type="text"
          value={category.data.title}
          onChange={e => {
            dispatch(beginProjectGraphEdit('Edit category name', category.name));
            dispatch(editProjectGraphNodeData(category.name, { title: e.target.value }));
          }}
          onBlur={endEditing}
          onKeyDown={keyHandler}
          className="secret-input category-title-edit"
          invalid={!category.data.title}
          disabled={!userCanEdit}
        />
      </Col>
      <Col sm={2}>
        <Input
          type="number"
          value={category.data.weight}
          onChange={e => {
            dispatch(beginProjectGraphEdit('Edit category weight', category.name));
            dispatch(editProjectGraphNodeData(category.name, { weight: parseInt(e.target.value) }));
          }}
          onBlur={endEditing}
          onKeyDown={keyHandler}
          className="secret-input category-weight-edit"
          invalid={isNaN(category.data.weight)}
          disabled={!userCanEdit}
        />
      </Col>
      <Col
        sm={2}
        className="d-flex align-items-center"
      >
        <Button
          size="sm"
          color="primary"
          outline
          className="material-icons md-18 p-1 mini-button nudge-up"
          disabled={!userCanEdit || !index || flashOff('up')}
          onClick={() => nudgeCategory(index, index - 1)}
        >
          <span className="material-icons md-18">arrow_upward</span>
        </Button>
        <Button
          size="sm"
          color="primary"
          outline
          className="material-icons md-18 p-1 ms-2 mini-button nudge-down"
          disabled={!userCanEdit || index === count - 1 || flashOff('down')}
          onClick={() => nudgeCategory(index, index + 1)}
        >
          <span className="material-icons md-18">arrow_downward</span>
        </Button>
        <Button
          size="sm"
          color="danger"
          outline
          className="material-icons md-18 p-1 ms-2 mini-button nudge-off"
          disabled={!userCanEdit || flashOff('del') || categoryInUse(category.name)}
          onClick={() => nudgeCategory(index, -1)}
        >
          <span className="material-icons md-18">close</span>
        </Button>
      </Col>
    </Row>
  );
};

const AddGradebookCategoryRow: React.FC<{
  userCanEdit: boolean;
  autoFocus: boolean;
  addCategory: (title: string, weight: number) => void;
}> = ({ userCanEdit, autoFocus, addCategory }) => {
  const polyglot = usePolyglot();
  const [title, setTitle] = useState('');
  const [weight, setWeight] = useState(0);
  return (
    <Form
      className="row mb-3 gradebook-add-category"
      onSubmit={e => {
        e.preventDefault();
        addCategory(title, weight);
      }}
    >
      <Col sm={8}>
        <Input
          type="text"
          value={title}
          disabled={!userCanEdit}
          autoFocus={autoFocus}
          placeholder={polyglot.t('GRADEBOOK_CATEGORY_ADD_PLACEHOLDER')}
          className="gradebook-title-edit"
          onChange={e => setTitle(e.target.value)}
        />
      </Col>
      <Col sm={2}>
        <Input
          type="number"
          value={weight}
          disabled={!userCanEdit}
          className="gradebook-weight-edit"
          onChange={e => setWeight(parseInt(e.target.value))}
        />
      </Col>
      <Col
        sm={2}
        className="d-flex align-items-center"
      >
        <Button
          type="submit"
          size="sm"
          color="success"
          outline
          className="material-icons md-18 p-1 mini-button gradebook-category-add"
          disabled={!userCanEdit || !title || isNaN(weight)}
        >
          <span className="material-icons md-18">add</span>
        </Button>
      </Col>
    </Form>
  );
};

export default CategoriesEditor;
