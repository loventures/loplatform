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

import Polyglot from 'node-polyglot';
import React from 'react';
import { Button, Col, Input } from 'reactstrap';
import { Pagination, PaginationItem, PaginationLink } from 'reactstrap';

import { SrsCollectionInfo } from '../../srs';

const PaginationEllipsis = () => {
  return (
    <PaginationItem disabled={true}>
      <PaginationLink className="px-2">&hellip;</PaginationLink>
    </PaginationItem>
  );
};

const PaginationPrev = ({ pageIndex, pageAction }: any) => {
  return (
    <PaginationItem disabled={pageIndex === 0}>
      <PaginationLink
        disabled={pageIndex === 0}
        onClick={() => pageAction(pageIndex - 1)}
      >
        &lt;
      </PaginationLink>
    </PaginationItem>
  );
};

const PaginationNext = ({ pageIndex, lastPageIndex, pageAction }: any) => {
  return (
    <PaginationItem disabled={pageIndex === lastPageIndex}>
      <PaginationLink
        disabled={pageIndex >= lastPageIndex}
        onClick={() => pageAction(pageIndex + 1)}
      >
        &gt;
      </PaginationLink>
    </PaginationItem>
  );
};

class PaginationPanel extends React.Component<Props> {
  render(): React.ReactNode {
    const { pageSize, csvUrl, stats, setPageSize, onPageChange, T } = this.props;
    const statsMsg = this.statsMessage();

    const sideSpan = 3;
    const numPages = Math.floor((stats.filterCount ?? 0) / pageSize) + 1;
    const pageIndex = Math.floor((stats.offset ?? 0) / pageSize);
    const lastPageIndex = numPages - 1;
    const allFits = numPages <= sideSpan * 2 + 1;
    const hasLeftEllipsis = !allFits && pageIndex - sideSpan > 0;
    const leftSpan = hasLeftEllipsis ? sideSpan - 2 : sideSpan;
    const hasRightEllipsis = !allFits && pageIndex + sideSpan < lastPageIndex;
    const rightSpan = hasRightEllipsis ? sideSpan - 2 : sideSpan;
    const startIndex = !hasLeftEllipsis
      ? 0
      : !hasRightEllipsis
        ? lastPageIndex - rightSpan - leftSpan
        : pageIndex - leftSpan;
    const endIndex = !hasRightEllipsis
      ? lastPageIndex
      : !hasLeftEllipsis
        ? rightSpan + leftSpan
        : pageIndex + rightSpan;
    const pages = Array.from(new Array(endIndex + 1 - startIndex), (_, i) => i + startIndex);

    const pageAction = (index: number) => onPageChange?.(1 + index, pageSize);

    return (
      <React.Fragment>
        <Col
          md={4}
          className="d-flex align-items-start"
        >
          {statsMsg && <span id="paging-stats">{statsMsg}</span>}
          <Button
            id="table-csv"
            className="glyphButton sneaky ms-2 mt-1"
            color="light"
            href={csvUrl}
            download
            title={T.t('crudTable.downloadTable')}
          >
            <i
              className="material-icons md-18"
              aria-hidden="true"
            >
              arrow_downward
            </i>
          </Button>
        </Col>
        <Col
          md={8}
          id="page-size-col"
        >
          {setPageSize && (
            <Input
              id="page-size-select"
              className="ms-2 mb-3"
              type="select"
              onChange={evt => setPageSize(parseInt(evt.target.value, 10))}
              defaultValue={pageSize}
            >
              <option value="0">{T.t('crudTable.pageSize.auto')}</option>
              {[16, 32, 64].map((v, i) => (
                <option key={i}>{v}</option>
              ))}
            </Input>
          )}
          <Pagination>
            <PaginationPrev
              pageIndex={pageIndex}
              pageAction={pageAction}
            />
            {hasLeftEllipsis && (
              <>
                <PaginationItem>
                  <PaginationLink onClick={() => pageAction(0)}>1</PaginationLink>
                </PaginationItem>
                <PaginationEllipsis />
              </>
            )}
            {pages.map(index => (
              <PaginationItem
                key={index}
                active={pageIndex === index}
              >
                <PaginationLink onClick={() => pageAction(index)}>{index + 1}</PaginationLink>
              </PaginationItem>
            ))}
            {hasRightEllipsis && (
              <>
                <PaginationEllipsis />
                <PaginationItem>
                  <PaginationLink onClick={() => pageAction(lastPageIndex)}>
                    {lastPageIndex + 1}
                  </PaginationLink>
                </PaginationItem>
              </>
            )}
            <PaginationNext
              pageIndex={pageIndex}
              lastPageIndex={lastPageIndex}
              pageAction={pageAction}
            />
          </Pagination>
        </Col>
      </React.Fragment>
    );
  }

  statsMessage = (): string | null => {
    const {
      props: {
        T,
        stats: { offset, count, filterCount, totalCount },
      },
    } = this;
    if (typeof filterCount === 'number' && typeof totalCount === 'number') {
      return T.t(
        'crudTable.page.showing' +
          (!count ? 'Zero' : filterCount === totalCount ? 'Unfiltered' : 'Filtered'),
        {
          first: 1 + (offset || 0),
          last: (offset || 0) + count,
          filtered: filterCount,
          total: totalCount,
        }
      );
    } else {
      return null;
    }
  };
}

type Props = {
  entity: string;
  pageSize: number;
  dataSize: number;
  csvUrl: string;
  setPageSize?(pageSize: number): void;
  onPageChange?(page: number, pageSize: number): void;
  stats: SrsCollectionInfo;
  T: Polyglot;
};

export default PaginationPanel;
