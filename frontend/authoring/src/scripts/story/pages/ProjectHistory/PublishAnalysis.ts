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

export class PublishAnalysis {
  constructor(
    public readonly numStaleSections = 0,
    public readonly creates: CreateLineItem[] = [],
    public readonly updates: UpdateLineItem[] = [],
    public readonly deletes: DeleteLineItem[] = []
  ) {}

  hasChanges(): boolean {
    return this.creates.length > 0 || this.updates.length > 0 || this.deletes.length > 0;
  }
}

export type LineItemChange = {
  name: string;
  edgePath: string;
  title: string;
  parentTitle: string | null;
  pointsPossible: number;
  isForCredit: boolean;
};

export type CreateLineItem = LineItemChange;

export type UpdateLineItem = LineItemChange & {
  lineItemId: string;
  prevPointsPossible: number | null;
  prevIsForCredit: boolean | null;
};

export type DeleteLineItem = LineItemChange & {
  lineItemId: string;
};
