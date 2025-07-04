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
import { BsChevronRight } from 'react-icons/bs';
import { IoEyeOutline } from 'react-icons/io5';
import { Button } from 'reactstrap';

import { setAdd, setToggle } from '../../gradebook/set';
import { TreeAssetWithParent } from '../../graphEdit';
import { usePolyglot } from '../../hooks';
import { getIcon, isContainer } from '../../story/AddAsset';
import { Stornado } from '../../story/badges/Stornado';
import { isQuestion } from '../../story/questionUtil';
import { storyTypeName, truncateAssetName } from '../../story/story';
import { NodeName, TypeId } from '../../types/asset';

const AddContentRow: React.FC<{
  content: TreeAssetWithParent;
  regex: RegExp;
  targetTypes: Set<TypeId>;
  prohibited: Set<NodeName>;
  selected: Set<NodeName>;
  setSelected: (selected: Set<NodeName>) => void;
  onPreview?: () => void;
  last: TreeAssetWithParent | undefined;
  setLast: (last: TreeAssetWithParent) => void;
  multiple: boolean;
  submit: (selected: Set<NodeName>) => void;
  collapsible: boolean;
  collapsed: boolean;
  showContext?: boolean;
  toggle: (all: boolean) => void;
}> = ({
  content,
  regex,
  targetTypes,
  prohibited,
  last,
  setLast,
  selected,
  setSelected,
  onPreview,
  submit,
  collapsible,
  collapsed,
  toggle,
  multiple,
  showContext,
}) => {
  const container = isContainer(content.typeId);
  const Icon = container ? undefined : getIcon(content.typeId);
  const polyglot = usePolyglot();
  const isSelected = selected.has(content.name);
  const ancestorSelected = content.context.some(node => selected.has(node.name));
  const disabled = prohibited.has(content.name) || !targetTypes.has(content.typeId);

  const getRange = (): NodeName[] | undefined => {
    if (!last || last.parent !== content.parent) return;
    const min = Math.min(content.index, last.index);
    const max = Math.max(content.index, last.index);
    const range = new Array<NodeName>();
    for (let i = min; i <= max; ++i) {
      const el = content.parent.children[i];
      if (!prohibited.has(el.name) && targetTypes.has(el.typeId)) range.push(el.name);
    }
    return range;
  };

  const lastContext = content.context[content.context.length - 1];

  return (
    <div
      className={classNames(
        'story-index-item',
        'd-flex',
        'align-items-center',
        'gap-2',
        'mx-3',
        `story-nav-${content.typeId.replace(/\..*/, '')}`,
        `depth-${content.depth}`,
        regex.ignoreCase && regex.test(content.data.title) && 'hit',
        (isSelected || ancestorSelected) && 'selected',
        disabled && !ancestorSelected && 'unenabled',
        collapsible && 'collapsible',
        (collapsible || !disabled) && 'pointer',
        disabled && !collapsible && 'cursor-disabled'
      )}
      onClick={e => {
        if (disabled && collapsible) toggle(false);
        if (disabled) return;
        e.preventDefault();
        const range = e.shiftKey ? getRange() : undefined;
        const mod = e.shiftKey || e.ctrlKey || e.metaKey;
        const result =
          multiple && mod ? setToggle(selected, range ?? content.name) : new Set([content.name]);
        setLast(content);
        setSelected(result);
      }}
      onDoubleClick={e => {
        if (disabled) return;
        e.preventDefault();
        const range = e.shiftKey ? getRange() : undefined;
        const mod = e.shiftKey || e.ctrlKey || e.metaKey;
        const result =
          multiple && mod ? setAdd(selected, range ?? content.name) : new Set([content.name]);
        setLast(content);
        setSelected(result);
        submit(result);
      }}
    >
      {collapsible && (
        <Button
          id={content.edge.name}
          size="small"
          color="transparent"
          className={classNames(
            'mini-button p-1 d-inline-flex align-items-center justify-content-center module-toggle',
            !collapsed && 'expanded'
          )}
          style={{ lineHeight: 1 }}
          onClick={e => {
            e.stopPropagation();
            toggle(e.shiftKey || e.metaKey);
          }}
          onDoubleClick={e => e.stopPropagation()}
        >
          <BsChevronRight size="1rem" />
        </Button>
      )}
      {Icon && (
        <Icon
          className={classNames('flex-shrink-0', !isSelected && 'text-muted')}
          title={storyTypeName(polyglot, content.typeId)}
        />
      )}
      <div className="overflow-hidden">
        {showContext && (
          <div className="context-summary text-muted minw-0">
            {content.context
              .filter(
                c => c.typeId !== 'course.1' && c.typeId !== 'module.1' && !isQuestion(c.typeId)
              )
              .map(truncateAssetName)
              .join(' – ')}
          </div>
        )}
        <div className="a text-truncate flex-shrink-1">
          {content.edge.group === 'questions' ? (
            <>
              {`Question ${content.index + 1} – `}
              <span className="unhover-muted">{content.data.title || 'Untitled'}</span>
            </>
          ) : content.typeId === 'rubric.1' ? (
            'Rubric for ' +
            (isQuestion(lastContext.typeId)
              ? storyTypeName(polyglot, lastContext.typeId, true) + ' – '
              : '') +
            lastContext.data.title
          ) : (
            content.data.title
          )}
        </div>
      </div>
      <Stornado
        name={content.name}
        size="md-noml"
      />
      {!disabled && !container && !!onPreview && (
        <Button
          size="sm"
          className="d-flex p-1 flex-shrink-0 border-0 br-50 ms-auto unhover-bg-transparent"
          title="Preview Content"
          onClick={e => {
            e.stopPropagation();
            onPreview();
          }}
        >
          <IoEyeOutline size="1rem" />
        </Button>
      )}
    </div>
  );
};

export default AddContentRow;
