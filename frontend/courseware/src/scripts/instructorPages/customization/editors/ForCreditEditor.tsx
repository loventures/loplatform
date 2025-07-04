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

import { CustomisableContent } from '../../../api/customizationApi';
import { LoCheckbox } from '../../../directives/LoCheckbox';
import { CreditTypes } from '../../../utilities/creditTypes';
import React from 'react';
import { withHandlers } from 'recompose';
import { Dispatch } from 'redux';

import { setForCredit } from '../contentEdits';
import { addEdit } from '../courseCustomizerReducer';

type ForCreditEditorProps = ForCreditEditorOuterProps & {
  toggleForCredit: () => void;
};

type ForCreditEditorOuterProps = {
  dispatch: Dispatch;
  content: CustomisableContent;
};

const ForCreditEditorInner: React.FC<ForCreditEditorProps> = ({ content, toggleForCredit }) => (
  <span className="mx-3 credit-editor">
    <LoCheckbox
      state={content.isForCredit}
      checkboxLabel="FOR_CREDIT"
      onToggle={toggleForCredit}
    />
  </span>
);

export const ForCreditEditor = withHandlers({
  toggleForCredit: (props: ForCreditEditorOuterProps) => () => {
    props.dispatch(
      addEdit(
        setForCredit({
          id: props.content.id,
          type: props.content.isForCredit ? CreditTypes.NoCredit : CreditTypes.Credit,
        })
      )
    );
  },
})(ForCreditEditorInner);
