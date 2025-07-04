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
import { useRef, useState } from 'react';
import { CiMenuKebab } from 'react-icons/ci';
import { IoCheckmarkOutline } from 'react-icons/io5';
import { DropdownItem, DropdownMenu, DropdownToggle, UncontrolledDropdown } from 'reactstrap';

import Loading from '../authoringUi/Loading';
import { useIsScrolled } from '../feedback/feedback';
import { useEditedCurrentAsset } from '../graphEdit';
import { RevisionHistory } from './RevisionHistory';

const RevisionPanel: React.FC = () => {
  const asset = useEditedCurrentAsset();

  const divRef = useRef<HTMLDivElement>();
  const scrolled = useIsScrolled(divRef.current);
  const [detail, setDetail] = useState(false);

  return (
    <div className="grid-feedback narrative revision">
      <div
        className="inner"
        ref={divRef}
      >
        <div
          className={classNames(
            'feedback-header p-2 d-flex align-items-center justify-content-between flex-shrink-0',
            { scrolled }
          )}
        >
          <div style={{ width: 'calc(2rem + 2px)' }}></div>
          <span style={{ fontSize: '1.1rem' }}>Revisions</span>
          <UncontrolledDropdown>
            <DropdownToggle
              id="revisions-toggle"
              color="transparent"
              className="d-flex align-items-center p-1 me-2"
            >
              <CiMenuKebab />
            </DropdownToggle>
            <DropdownMenu
              right
              id="revisions-menu"
            >
              <DropdownItem onClick={() => setDetail(d => !d)}>
                <div className="check-spacer">{detail && <IoCheckmarkOutline />}</div>
                Detailed View
              </DropdownItem>
            </DropdownMenu>
          </UncontrolledDropdown>
        </div>
        <div className="panel-body">
          {asset ? (
            <RevisionHistory
              scrollRef={divRef}
              asset={asset}
              detail={detail}
            />
          ) : (
            <Loading />
          )}
        </div>
      </div>
    </div>
  );
};

export default RevisionPanel;
