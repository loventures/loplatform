/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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

import { connect } from 'react-redux';
import { withTranslation, Translate } from '../../i18n/translationContext.js';
import { viewParentFromContentActionCreator } from '../../courseContentModule/actions/contentPageActions.js';

interface BackToModuleButtonProps {
  translate: Translate;
  content: any;
  visitParent: (content: any) => void;
}

const BackToModuleButton = ({ translate, content, visitParent }: BackToModuleButtonProps) => (
  <button
    className="back-to-module-btn btn btn-primary"
    onClick={() => visitParent(content)}
  >
    {translate('ASSET_GO_UP')}
  </button>
);

export default connect(null, {
  visitParent: viewParentFromContentActionCreator,
})(withTranslation(BackToModuleButton));
