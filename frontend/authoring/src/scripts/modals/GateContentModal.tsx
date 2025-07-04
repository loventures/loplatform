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

import * as React from 'react';
import { useMemo, useState } from 'react';
import { IoSearchOutline } from 'react-icons/io5';
import {
  Button,
  Input,
  InputGroup,
  InputGroupText,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from 'reactstrap';
import { useDebounce } from 'use-debounce';

import edgeRuleConstants from '../editor/EdgeRuleConstants';
import { setToggle } from '../gradebook/set';
import {
  ElementsOnly,
  getContentTree,
  getFilteredContentList,
  TreeAsset,
  TreeAssetWithParent,
  useAllEditedOutEdges,
  useEditedAsset,
  useGraphEdits,
} from '../graphEdit';
import { useHomeNodeName, useModal, usePolyglot } from '../hooks';
import { toMultiWordRegex } from '../story/questionUtil';
import { Onceler } from '../story/story';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { NewAsset, NodeName, TypeId } from '../types/asset';
import { EdgeGroup } from '../types/edge';
import AddContentRow from './narrative/AddContentRow';

export type GateContentModalData = {
  parent: NewAsset<any>;
  group: EdgeGroup;
  callback: (names: NodeName[]) => void | Promise<any>;
};

const GateContentModal = () => {
  const polyglot = usePolyglot();
  const {
    modalOpen,
    toggleModal,
    data: { callback, parent, group },
  } = useModal<GateContentModalData>();
  const [selected, setSelected] = useState(new Set<NodeName>());
  const [search, setSearch] = useState('');
  const [regex] = useDebounce(toMultiWordRegex(search), 300);
  const targetTypes = useMemo(
    () => new Set(edgeRuleConstants[parent.typeId][group] as TypeId[]),
    [parent]
  );
  const allEdges = useAllEditedOutEdges(parent.name);
  const prohibited = useMemo(
    () =>
      new Set([
        ...allEdges.filter(edge => edge.group === group).map(edge => edge.targetName),
        parent.name,
      ]), // pedantically any ancestor
    [parent, group, allEdges]
  );
  const [expanded, setExpanded] = useState(new Set<NodeName>());
  const [submitting, setSubmitting] = useState(false);

  const homeNodeName = useHomeNodeName();
  const home = useEditedAsset(homeNodeName);
  const projectGraph = useProjectGraph();
  const graphEdits = useGraphEdits();
  const [last, setLast] = useState<TreeAssetWithParent | undefined>();

  const groups = ElementsOnly;

  const contentTree = useMemo(() => {
    return getContentTree(home, [], groups, projectGraph, graphEdits);
  }, [projectGraph, groups]);

  const contentList = useMemo(() => {
    const predicate = (asset: TreeAsset) => {
      if (!regex.test(asset.data.title)) return false;
      // if (parent.typeId === 'course.1') return asset.typeId === 'module.1' ? '.' : false;
      return true;
    };
    return getFilteredContentList(contentTree, predicate);
  }, [contentTree, regex]);

  const submit = (selection: Set<NodeName>) => {
    // we iterate over the content list to preserve original order rather than click order.
    // should we preserve click order?
    const once = Onceler<NodeName>(); // once because content reuse shows up twice in content list
    const contents = getFilteredContentList(contentTree, undefined).filter(
      content =>
        selection.has(content.name) &&
        !content.context.some(ancestor => selection.has(ancestor.name)) &&
        once(content.name)
    );
    if (contents.length) {
      setSubmitting(true);
      Promise.resolve(callback(contents.map(c => c.name)))
        .then(() => toggleModal())
        .finally(() => setSubmitting(false));
    }
  };

  return (
    <Modal
      id="gate-content-modal"
      isOpen={modalOpen}
      toggle={toggleModal}
      size="xl"
      className="no-exit-edit narrative-editor not-100"
    >
      <ModalHeader>{group === 'testsOut' ? 'Test Content Out' : 'Gate Content'}</ModalHeader>

      <ModalBody className="p-0">
        <div
          className="content-list full-index non-a py-3 add-content-list"
          style={{ overflow: 'auto', height: 'calc(100vh - 13rem)' }}
        >
          {regex.ignoreCase || contentList.length ? (
            <div className="pt-2 pb-3 d-flex justify-content-center">
              <InputGroup className="search-bar">
                <Input
                  type="search"
                  value={search}
                  onChange={e => setSearch(e.target.value)}
                  placeholder="Filter by title..."
                  bsSize="sm"
                  size={48}
                />
                <InputGroupText className="search-icon form-control form-control-sm flex-grow-0 d-flex align-items-center justify-content-center p-0 pe-1">
                  <IoSearchOutline aria-hidden />
                </InputGroupText>
              </InputGroup>
            </div>
          ) : null}
          {!contentList.length ? (
            <div className="text-muted mt-5 depth-1 text-center">
              {regex.ignoreCase ? 'No content matches your search.' : 'No contents.'}
            </div>
          ) : null}
          {contentList
            .filter(
              c => c.context.length < 2 || regex.ignoreCase || expanded.has(c.context[1].name)
            )
            .map(content => (
              <AddContentRow
                key={content.edge?.name ?? '_root_'}
                content={content}
                targetTypes={targetTypes}
                prohibited={prohibited}
                regex={regex}
                last={last}
                setLast={setLast}
                selected={selected}
                setSelected={setSelected}
                submit={submit}
                collapsible={!regex.ignoreCase && content.typeId === 'module.1'}
                collapsed={!regex.ignoreCase && !expanded.has(content.name)}
                multiple={true}
                toggle={all =>
                  setExpanded(
                    !all
                      ? setToggle(expanded, content.name)
                      : expanded.has(content.name)
                        ? new Set()
                        : new Set(contentTree.children.map(a => a.name))
                  )
                }
              />
            ))}
        </div>
      </ModalBody>
      <ModalFooter>
        <Button
          id="modal-cancel"
          color="secondary"
          onClick={toggleModal}
          disabled={submitting}
        >
          {polyglot.t('CANCEL')}
        </Button>
        <Button
          id="modal-submit"
          color="primary"
          onClick={() => submit(selected)}
          disabled={submitting || !selected.size}
        >
          {group === 'testsOut' ? 'Test Out' : 'Gate'}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export default GateContentModal;
