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

export const createDataListUpdateMergeAction = (sliceName, list) => ({
  type: 'DATA_LIST_UPDATE_MERGE',
  sliceName,
  data: { list },
});

export const createDataListUpdateReplaceAction = (sliceName, list) => ({
  type: 'DATA_LIST_UPDATE_REPLACE',
  sliceName,
  data: { list },
});

export const createDataItemInvalidateAction = (sliceName, id) => ({
  type: 'DATA_ITEM_INVALIDATE',
  sliceName,
  id,
});

export const createDataItemUpdateReplaceAction = (sliceName, id, item) => ({
  type: 'DATA_ITEM_UPDATE',
  sliceName,
  id,
  data: { item },
});

export const createDataItemExtendReplaceAction = (sliceName, id, item) => ({
  type: 'DATA_ITEM_EXTEND',
  sliceName,
  id,
  data: { item },
});

export const createDataItemReplaceAction = (sliceName, id, item) => ({
  type: 'DATA_ITEM_REPLACE',
  sliceName,
  id,
  data: { item },
});
