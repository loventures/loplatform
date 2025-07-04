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
import { AttemptsOverview, fetchAttemptOverviews } from '../../api/assessmentApi';
import { Content } from '../../api/contentsApi';
import { Column } from '../../api/gradebookApi';
import Course from '../../bootstrap/course';
import LoLink from '../../components/links/LoLink';
import PaginationWithMax, { DEFAULT_PAGE_SIZE } from '../../components/PaginateWithMax';
import SearchBox from '../../components/SearchBox';
import dayjs from 'dayjs';
import advanced from 'dayjs/plugin/advancedFormat';
import localized from 'dayjs/plugin/localizedFormat';
import timezone from 'dayjs/plugin/timezone';
import utc from 'dayjs/plugin/utc';
import { find } from 'lodash';
import { thru } from 'lodash/fp';
import { LoCheckbox } from '../../directives/LoCheckbox';
import { isForCredit } from '../../utilities/creditTypes';
import { withTranslation } from '../../i18n/translationContext';
import { isGradableAssignment, isAssessment } from '../../utilities/contentTypes';
import React from 'react';
import Alert from 'reactstrap/lib/Alert';

import { PointsEditor } from './PointsEditor';

dayjs.extend(utc);
dayjs.extend(timezone);
dayjs.extend(localized);
dayjs.extend(advanced);

export type AttemptOverviewsProps = {
  columns: Column[];
  contents: Content[];
  enrollmentCount: number;
  translate: (key: string) => string;
};

type Filters = {
  filterText: string;
  showCreditOnly: boolean;
  page: number;
};

type AttemptOverviewsState = {
  filters: Filters;
  stagedFilters: Filters;
  loading: boolean;
  errored: boolean;
  filtersDirty: boolean;
  errorMessage?: string;
  lastRequested: number;
  attemptOverviews: AttemptsOverview[];
};

type AttemptOverviewRow = {
  content: Content;
  column: Column;
  attemptOverviews?: AttemptsOverview;
};

type OverviewCounts = {
  graded: number;
  completed: number;
  notStarted: number;
};
const overviewToCounts = (
  numberOfAssignees: number,
  overview?: AttemptsOverview
): OverviewCounts => {
  const attempts = overview ? overview.studentAttempts : {};
  const initial = { graded: 0, completed: 0 };
  const gac = Object.values(attempts).reduce(
    (acc, { Submitted, Finalized }) => ({
      graded: Finalized > 0 ? acc.graded + 1 : acc.graded,
      completed: Finalized > 0 || Submitted > 0 ? acc.completed + 1 : acc.completed,
      assigneesNotStarted: numberOfAssignees,
    }),
    initial
  );

  return {
    ...gac,
    notStarted: numberOfAssignees - gac.completed,
  };
};

class AttemptOverviewsInner extends React.Component<AttemptOverviewsProps, AttemptOverviewsState> {
  updateSearch: (text: string) => void;
  updateSearchText: (text: string) => void;

  constructor(props: AttemptOverviewsProps) {
    super(props);

    const initialFilters = {
      filterText: '',
      showCreditOnly: false,
      page: 1,
    };
    this.state = {
      filtersDirty: false,
      filters: initialFilters,
      stagedFilters: initialFilters,

      loading: true,
      errored: false,
      lastRequested: Date.now(),
      attemptOverviews: [],
    };
    this.fetchAttempts = this.fetchAttempts.bind(this);
    this.updateFilters = this.updateFilters.bind(this);
    this.filteredRows = this.filteredRows.bind(this);
    this.allRows = this.allRows.bind(this);
    this.updateSearchText = (text: string) => {
      this.updateFilters(filters => ({
        ...filters,
        filterText: text,
        page: 1,
      }));
    };
    this.updateSearch = (text: string) => {
      this.setState({
        filtersDirty: true,
        stagedFilters: { ...this.state.filters, filterText: text },
      });
      this.updateSearchText(text);
    };
  }

  componentDidMount() {
    // fetch the attempt overviews
    // todo: change global access to lo_platform to some injectable context solution
    this.fetchAttempts(this.state.filters);
  }

  allRows(): AttemptOverviewRow[] {
    return this.props.contents.flatMap(content => {
      const column = find(this.props.columns, col => col.id === content.id);
      // "columns" that are the same as the category they're in are modules, and shouldn't be shown
      // in this list. I wish we had a better way of expressing this
      if (column && column.id !== column.Category.id) {
        return [
          {
            content,
            column,
            attemptOverviews: find(
              this.state.attemptOverviews,
              a => a.edgePath.indexOf(content.id) !== -1
            ),
          },
        ];
      } else {
        return [];
      }
    });
  }

  filteredRows(filters: Filters): AttemptOverviewRow[] {
    return this.allRows()
      .filter(o => o.content.name.toLowerCase().indexOf(filters.filterText.toLowerCase()) !== -1)
      .filter(o => isAssessment(o.content))
      .filter(o => (filters.showCreditOnly ? isForCredit(o.column.credit) : true));
  }

  filteredAndSlicedRows(filters: Filters) {
    const begin = (filters.page - 1) * DEFAULT_PAGE_SIZE;
    const end = begin + DEFAULT_PAGE_SIZE;
    return this.filteredRows(filters).slice(begin, end);
  }

  fetchAttempts(filters: Filters) {
    const contents = this.filteredAndSlicedRows(filters).map(c => c.content.id);
    const nowRequested = Date.now();
    this.setState({
      filtersDirty: true,
      filters,
      lastRequested: nowRequested,
    });

    fetchAttemptOverviews(Course.id, contents)
      .then(overviews => {
        if (this.state.lastRequested === nowRequested) {
          this.setState({
            filtersDirty: false,
            loading: false,
            errored: false,
            attemptOverviews: overviews,
          });
        } else {
          console.log(
            'Not updating attempt overviews, since another request was made after this one'
          );
        }
      })
      .catch(error => {
        console.error('Failed to fetch attempt overviews: ', error);
        this.setState({ errored: true, errorMessage: JSON.stringify(error) });
      });
  }

  updateFilters(modify: (filters: Filters) => Partial<Filters>) {
    const newFilters = {
      ...this.state.filters,
      ...modify(this.state.filters),
    };
    this.fetchAttempts(newFilters);
  }

  render() {
    const { translate } = this.props;
    const rows = this.filteredAndSlicedRows(this.state.filters);
    return (
      <div className="card mt-3 assignments-table">
        <div className="card-header">
          <div className="flex-row-content flex-wrap">
            <LoCheckbox
              checkboxFor="credit-only"
              checkboxLabel={'GRADEBOOK_DISPLAY_CREDIT_ONLY'}
              onToggle={() => this.updateFilters(f => ({ showCreditOnly: !f.showCreditOnly }))}
              state={this.state.filters.showCreditOnly}
            />
            <SearchBox
              id="assignments-list-search"
              className="flex-col-fluid"
              searchString={this.state.stagedFilters.filterText}
              searchAction={s => this.updateSearch(s)}
              placeholder={translate('SEARCH_ASSIGNMENT_NAME')}
              ariaControls="instructor-assignments-table"
            />
          </div>
        </div>
        <div
          className="table-responsive"
          id="instructor-assignments-table"
        >
          {this.state.errored && (
            <div className="alert alert-danger">
              <span>Error: </span>
              <span>{this.state.errorMessage}</span>
            </div>
          )}
          <table className="table table-striped card-table">
            <caption className="sr-only">{translate('ASSIGNMENTS_TABLE_TABLE_CAPTION')}</caption>
            <thead className="thead-default">
              <tr>
                <th className="name">{translate('ASSIGNMENTS_TABLE_HEAD_NAME')}</th>
                <th>{translate('ASSIGNMENTS_TABLE_HEAD_POINTS')}</th>
                <th>{translate('ASSIGNMENTS_TABLE_HEAD_DUEDATE')}</th>
                <th>{translate('ASSIGNMENTS_TABLE_HEAD_SUBMITTED')}</th>
                <th>{translate('ASSIGNMENTS_TABLE_HEAD_NOT_SUBMITTED')}</th>
                <th>{translate('ASSIGNMENTS_TABLE_HEAD_GRADED')}</th>
              </tr>
            </thead>

            <tbody
              style={{ opacity: this.state.filtersDirty ? 0.5 : 1 }}
              className={classNames({ dirty: this.state.filtersDirty })}
            >
              {rows.map(row => (
                <tr key={row.content.id}>
                  <td className="title-cell">
                    {isGradableAssignment(row.content) ? ( // TODO: replace this with wes' react router stuff when it gets merged
                      <LoLink
                        to={`/instructor/assignments/${row.content.id}`}
                        target="_self"
                        disabled={false}
                      >
                        {row.content.name}
                      </LoLink>
                    ) : (
                      row.content.name
                    )}
                  </td>

                  {thru(({ graded, completed, notStarted }: OverviewCounts) => (
                    <React.Fragment>
                      <td className="points-cell text-right">
                        <PointsEditor
                          column={row.column}
                          canEdit={graded === 0 && completed === 0}
                        />
                      </td>

                      <td>
                        <div className="date-picker-container">
                          {row.content.dueDate
                            ? dayjs(row.content.dueDate).format('LLL z')
                            : translate('LO_DATE_PICKER_NO_DATE')}
                        </div>
                      </td>
                      <td>{completed}</td>
                      <td>{notStarted}</td>
                      <td>{graded}</td>
                    </React.Fragment>
                  ))(overviewToCounts(this.props.enrollmentCount, row.attemptOverviews))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {this.filteredRows(this.state.filters).length > 0 ? (
          <div className="card-footer">
            <PaginationWithMax
              pageIndex={this.state.filters.page - 1}
              numPages={Math.ceil(this.filteredRows(this.state.filters).length / DEFAULT_PAGE_SIZE)}
              pageAction={pageIndex => this.updateFilters(() => ({ page: pageIndex + 1 }))}
            />
          </div>
        ) : (
          <Alert
            color="info"
            className="m-2"
          >
            {translate('ASSIGNMENTS_TABLE_NO_RESULTS')}
          </Alert>
        )}
      </div>
    );
  }
}

export const AttemptOverviews = withTranslation(AttemptOverviewsInner);
