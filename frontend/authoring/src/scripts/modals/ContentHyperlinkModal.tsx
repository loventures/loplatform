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
import * as React from 'react';
import { useState } from 'react';
import { IoSearchOutline } from 'react-icons/io5';
import { useDispatch, useSelector } from 'react-redux';
import {
  Button,
  FormGroup,
  Input,
  InputGroup,
  InputGroupText,
  Label,
  Modal,
  ModalBody,
  ModalFooter,
} from 'reactstrap';

import {
  addProjectGraphEdge,
  TreeAsset,
  useAllEditedOutEdges,
  useEditedAsset,
  useFilteredContentList,
} from '../graphEdit';
import { useModal, usePolyglot } from '../hooks';
import { getIcon } from '../story/AddAsset';
import { toMultiWordRegex } from '../story/questionUtil';
import { useProjectGraph } from '../structurePanel/projectGraphActions';
import { DcmState } from '../types/dcmState';

const ContentHyperlinkModal = () => {
  const polyglot = usePolyglot();
  const { callback, cancelback, name } = useSelector((state: DcmState) => state.modal.data);
  const { modalOpen, toggleModal } = useModal();
  const [selected, setSelected] = useState<TreeAsset>();
  const [search, setSearch] = useState('');
  const [newTab, setNewTab] = useState(false);
  const regex = toMultiWordRegex(search);

  const dispatch = useDispatch();
  const projectGraph = useProjectGraph();
  const allEdges = useAllEditedOutEdges(name);
  const hyperlinks = allEdges.filter(edge => edge.group === 'hyperlinks');
  const { homeNodeName } = projectGraph;
  const course = useEditedAsset(homeNodeName);
  const contentList = useFilteredContentList(course, [], false, search);

  const cancelModal = () => {
    toggleModal();
    cancelback();
  };

  const submit = (selected: TreeAsset | undefined) => {
    if (selected) {
      let edge = hyperlinks.find(edge => edge.targetName === selected.name);
      if (!edge) {
        edge = {
          name: crypto.randomUUID(),
          sourceName: name,
          targetName: selected.name,
          group: 'hyperlinks',
          data: {},
          traverse: false,
          edgeId: crypto.randomUUID(),
          newPosition: 'end',
        };
        dispatch(addProjectGraphEdge(edge));
      }
      const linkUrl = `javascript:lonav('${edge.edgeId}')`; // this is used by server-side link reports
      toggleModal();
      callback(selected.data.title, linkUrl, newTab, 'lonav(event)');
    }
  };

  return (
    <Modal
      id="content-link-modal"
      isOpen={modalOpen}
      toggle={cancelModal}
      size="lg"
      className="no-exit-edit narrative-editor"
    >
      <div className="modal-header align-items-center">
        <h5 className="modal-title">{polyglot.t('CONTENT_HYPERLINK_MODAL_TITLE')}</h5>
        <InputGroup className="search-bar">
          <Input
            type="search"
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search by title..."
            bsSize="sm"
            size={48}
          />
          <InputGroupText className="search-icon form-control form-control-sm flex-grow-0 d-flex align-items-center justify-content-center p-0 pe-1">
            <IoSearchOutline aria-hidden />
          </InputGroupText>
        </InputGroup>
      </div>
      <ModalBody>
        <div
          className="content-list full-index"
          style={{ overflow: 'auto', height: 'calc(100vh - 16rem)' }}
        >
          {contentList.map(content => (
            <ContentLinkRow
              key={content.edge?.name ?? '_root_'}
              content={content}
              regex={regex}
              selected={selected}
              select={() => setSelected(content)}
              insert={() => submit(content)}
            />
          ))}
        </div>
      </ModalBody>
      <ModalFooter>
        <FormGroup
          check
          className="me-auto"
        >
          <Label
            check
            className="ps-3"
          >
            <Input
              type="checkbox"
              checked={newTab}
              disabled={false}
              onChange={() => setNewTab(a => !a)}
            />
            {polyglot.t('STORY_newWindow_true')}
          </Label>
        </FormGroup>
        <Button
          id="modal-cancel"
          color="secondary"
          onClick={cancelModal}
        >
          {polyglot.t('CANCEL')}
        </Button>
        <Button
          id="modal-submit"
          color="primary"
          onClick={() => submit(selected)}
          disabled={!selected}
        >
          {polyglot.t('CONTENT_HYPERLINK_MODAL_INSERT')}
        </Button>
      </ModalFooter>
    </Modal>
  );
};

const ContentLinkRow: React.FC<{
  content: TreeAsset;
  regex: RegExp;
  selected?: TreeAsset;
  select: () => void;
  insert: () => void;
}> = ({ content, regex, selected, select, insert }) => {
  const Icon =
    content.typeId === 'module.1' || content.typeId === 'lesson.1'
      ? undefined
      : getIcon(content.typeId);
  const isSelected = content.name === selected?.name;
  return (
    <div
      key={content.edge?.name ?? '_root_'}
      className={classNames(
        'story-index-item',
        'd-flex',
        'align-items-center',
        'gap-2',
        `story-nav-${content.typeId.replace(/\..*/, '')}`,
        `depth-${content.depth}`,
        regex.ignoreCase && regex.test(content.data.title) && 'hit',
        isSelected && 'selected'
      )}
      style={{ cursor: 'pointer' }}
      onClick={select}
      onDoubleClick={insert}
    >
      {Icon && <Icon className={classNames('flex-shrink-0', !isSelected && 'text-muted')} />}
      <span className="a text-truncate">{content.data.title}</span>
    </div>
  );
};

export default ContentHyperlinkModal;
