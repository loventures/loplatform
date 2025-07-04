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
import React, { Dispatch, SetStateAction, useCallback, useRef, useState } from 'react';
import { AiOutlinePlus } from 'react-icons/ai';
import { BsList, BsListNested } from 'react-icons/bs';
import { IoTrashOutline } from 'react-icons/io5';
import { useDispatch } from 'react-redux';
import { useClickAway } from 'react-use';
import { Button, Popover } from 'reactstrap';

import {
  autoSaveProjectGraphEdits,
  beginProjectGraphEdit,
  discardProjectGraphEdit,
  editProjectGraphNodeData,
  useEditedAssetTitle,
} from '../graphEdit';
import { usePolyglot } from '../hooks';
import { openModal } from '../modals/modalActions';
import { ModalIds } from '../modals/modalIds';
import { Stornado } from '../story/badges/Stornado';
import { plural, useEditSession, useEscapeOrEnterToStopEditing } from '../story/story';
import { NewAssetWithEdge } from '../types/edge';
import { Level1Competency, Level2Competency, Level3Competency } from '../types/typeIds';
import { addCompetencyAction, removeCompetencyAction } from './competencyEditorActions';

const MAGIC_INPUT_LINE_HEIGHT = 30;

type CompetencyRowProps = {
  competency: NewAssetWithEdge<any>;
  autoFocus: boolean;
  setAddedItem: Dispatch<SetStateAction<string>>;
  userCanEdit: boolean;
  alignments: number;
  resetCompetencyTree: () => void;
  resetAlignments: () => void;
};

const CompetencyRow: React.FC<CompetencyRowProps> = ({
  competency,
  autoFocus,
  setAddedItem,
  userCanEdit,
  alignments,
  resetCompetencyTree,
  resetAlignments,
}) => {
  const polyglot = usePolyglot();
  const dispatch = useDispatch();
  const [active, setActive] = useState(autoFocus ?? false);
  const buttonRef = useRef<HTMLButtonElement>();
  const [addOpen, setAddOpen] = useState(false);
  const titleRef = useRef<HTMLSpanElement>(null);
  const popoverRef = useRef(null);
  const title = useEditedAssetTitle(competency.name);

  useClickAway(popoverRef, () => {
    setAddOpen(false);
  });

  const session = useEditSession();
  const updateTitle = (value: string) => {
    const title = value.trim() || 'Untitled';
    dispatch(beginProjectGraphEdit('Edit Competency Title', session));
    dispatch(editProjectGraphNodeData(competency.name, { title }));
  };

  const onRemoveCompetency = () => {
    dispatch(removeCompetencyAction(competency, resetCompetencyTree));
  };

  const addItem = (sibling: boolean) => {
    setAddOpen(false);
    dispatch(addCompetencyAction(competency, sibling, setAddedItem, resetCompetencyTree));
  };

  const [rowSize, setRowSize] = useState(1);
  const makeActive = () => {
    if (titleRef.current) {
      // TODO: support both sizes of font 30px seems to be good for the large font. Something
      //       smaller for the sub comps, 24 perhaps?
      setRowSize(titleRef.current.offsetHeight / MAGIC_INPUT_LINE_HEIGHT);
    }
    setActive(true);
  };

  const endEditing = useCallback(() => {
    setActive(false);
    // TODO: these should all be dispatch(endProjectGraphEdit(undoaction);
    dispatch(autoSaveProjectGraphEdits());
  }, []);

  const finishEditing = useCallback(
    (enter: boolean) => {
      if (enter) {
        endEditing();
      } else {
        dispatch(discardProjectGraphEdit(session));
        setActive(false);
      }
    },
    [endEditing, session]
  );

  const keyHandler = useEscapeOrEnterToStopEditing(finishEditing);

  const indentLvl =
    competency.typeId === Level3Competency
      ? 'lvl-3'
      : competency.typeId === Level2Competency
        ? 'lvl-2'
        : 'lvl-1';
  return (
    <div
      className={classNames(
        indentLvl,
        !userCanEdit && 'bg-transparent',
        'd-flex',
        'justify-content-between',
        'competencies-row',
        'align-items-center'
      )}
    >
      <div className="d-flex align-items-center flex-grow-1">
        {active ? (
          <textarea
            ref={e => e?.setSelectionRange(e.value.length, e.value.length)}
            className="competency-title border-0"
            autoFocus={true}
            defaultValue={title === 'Untitled' ? '' : title}
            placeholder={polyglot.t('UNTITLED_COMPETENCY')}
            onBlur={endEditing}
            onChange={e => updateTitle(e.target.value)}
            onKeyDown={keyHandler}
            rows={rowSize}
          />
        ) : (
          <span
            ref={titleRef}
            className={classNames('competency-title', userCanEdit && 'editable')}
            tabIndex={userCanEdit ? 0 : undefined}
            onFocus={userCanEdit ? () => makeActive() : undefined}
          >
            {title}
            <Stornado
              name={competency.name}
              size={competency.typeId === Level1Competency ? 'md' : 'sm'}
              className="align-baseline"
            />
          </span>
        )}
      </div>
      <div className="controls d-flex ms-1 gap-1">
        {userCanEdit ? (
          <>
            <Button
              size="sm"
              color="primary"
              outline
              className="border-0 d-inline-flex p-2 add-competency"
              innerRef={buttonRef}
              onClick={() => setAddOpen(!addOpen)}
            >
              <AiOutlinePlus />
            </Button>
            <Button
              size="sm"
              color="danger"
              outline
              className="border-0 d-inline-flex p-2"
              onClick={() => onRemoveCompetency()}
            >
              <IoTrashOutline />
            </Button>
            <Button
              size="sm"
              outline
              className={classNames('text-nowrap aligner', !alignments && 'unaligned')}
              onClick={() => {
                dispatch(
                  openModal(ModalIds.CompetencyAlignment, {
                    competencyToAlign: competency.name,
                    resetAlignments,
                  })
                );
              }}
            >
              {alignments ? plural(alignments, 'Alignment') : 'Unaligned'}
            </Button>
          </>
        ) : (
          <Button
            size="sm"
            outline
            className={classNames('text-nowrap aligner', !alignments && 'unaligned')}
            onClick={() => {
              dispatch(
                openModal(ModalIds.CompetencyAlignment, {
                  competencyToAlign: competency.name,
                  resetAlignments,
                })
              );
            }}
          >
            {alignments ? plural(alignments, 'Alignment') : 'Unaligned'}
          </Button>
        )}
        {buttonRef.current && addOpen ? (
          <Popover
            isOpen
            target={buttonRef.current}
            placement="bottom"
            className="add-popover"
            innerClassName="add-palette"
            innerRef={popoverRef}
            onClick={e => e.stopPropagation()}
          >
            <Button
              color="primary"
              outline
              onClick={() => addItem(true)}
            >
              <BsList size="1.75rem" />
              <div className="small">{polyglot.t('ADD_SIBLING_COMPETENCY')}</div>
            </Button>
            <Button
              color="primary"
              outline
              onClick={() => addItem(false)}
              disabled={competency.typeId === Level3Competency}
            >
              <BsListNested size="1.75rem" />
              <div className="small">{polyglot.t('ADD_CHILD_COMPETENCY')}</div>
            </Button>
          </Popover>
        ) : null}
      </div>
    </div>
  );
};

export default CompetencyRow;
