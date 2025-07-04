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

import { map, range } from 'lodash';
import { TranslationContext } from '../i18n/translationContext';
import React, { useContext } from 'react';
import { Pagination, PaginationItem, PaginationLink } from 'reactstrap';

export const DEFAULT_SIDE_SPAN = 4;
export const DEFAULT_PAGE_SIZE = 10;
export const DEFAULT_PAGE_SIZE_OPTIONS = [10, 25, 50, 100];

const PaginationEllipsis = () => {
  return (
    <PaginationItem disabled={true}>
      <PaginationLink className="px-2">&hellip;</PaginationLink>
    </PaginationItem>
  );
};

const PaginationPrev = ({ pageIndex, pageAction }: any) => {
  const translate = useContext(TranslationContext);
  return (
    <PaginationItem disabled={pageIndex === 0}>
      <PaginationLink
        disabled={pageIndex === 0}
        onClick={() => pageAction(pageIndex - 1)}
      >
        {translate('PAGINATE_PREVIOUS')}
      </PaginationLink>
    </PaginationItem>
  );
};

const PaginationNext = ({ pageIndex, lastPageIndex, pageAction }: any) => {
  const translate = useContext(TranslationContext);
  return (
    <PaginationItem disabled={pageIndex === lastPageIndex}>
      <PaginationLink
        disabled={pageIndex >= lastPageIndex}
        onClick={() => pageAction(pageIndex + 1)}
      >
        {translate('PAGINATE_NEXT')}
      </PaginationLink>
    </PaginationItem>
  );
};

const PaginationWithMax: React.FC<{
  numPages: number;
  pageIndex: number;
  pageAction: (pn: number) => void;
  sideSpan?: number;
}> = ({ numPages, pageIndex, pageAction, sideSpan = DEFAULT_SIDE_SPAN }) => {
  const lastPageIndex = numPages - 1;
  const allFits = numPages <= sideSpan * 2 + 1;
  const hasLeftEllipsis = !allFits && pageIndex - sideSpan > 0;
  const leftSpan = hasLeftEllipsis ? sideSpan - 2 : sideSpan;
  const hasRightEllipsis = !allFits && pageIndex + sideSpan < lastPageIndex;
  const rightSpan = hasRightEllipsis ? sideSpan - 2 : sideSpan;

  const startIndex = (() => {
    if (!hasLeftEllipsis) {
      return 0;
    } else if (!hasRightEllipsis) {
      return lastPageIndex - rightSpan - leftSpan;
    } else {
      return pageIndex - leftSpan;
    }
  })();

  const endIndex = (() => {
    if (!hasRightEllipsis) {
      return lastPageIndex;
    } else if (!hasLeftEllipsis) {
      return rightSpan + leftSpan;
    } else {
      return pageIndex + rightSpan;
    }
  })();

  const pages = range(startIndex, endIndex + 1);
  return (
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
      {map(pages, index => (
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
  );
};

export default PaginationWithMax;
