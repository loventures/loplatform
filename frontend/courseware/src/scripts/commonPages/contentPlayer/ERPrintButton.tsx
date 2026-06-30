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

import { ContentPrinterPageLink } from '../../utils/pageLinks';
import { ContentWithRelationships } from '../../courseContentModule/selectors/assembleContentView';
import {
  ContentWithNebulousDetails,
  ViewingAs,
} from '../../courseContentModule/selectors/contentEntry';
import { useTranslation } from '../../i18n/translationContext';
import { allowPrintingEntireLesson } from '../../utilities/preferences';
import React, { useState } from 'react';
import { FiPrinter } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import {
  UncontrolledDropdown as Dropdown,
  DropdownItem,
  DropdownMenu,
  DropdownToggle,
  UncontrolledTooltip,
} from 'reactstrap';
import { printService } from '../../utilities/printService.ts';

type ERPrintButtonProps = {
  viewingAs: ViewingAs;
  content: ContentWithRelationships;
  module?: ContentWithNebulousDetails;
};

const ERPrintButton: React.FC<ERPrintButtonProps> = ({ viewingAs, content, module }) => {
  const translate = useTranslation();
  const navigate = useNavigate();
  // history v5 / v6 take navigation state separately from the path target.
  const go = (to: any) => navigate(to, { state: to?.state });

  const moduleNameLower = module?.name.toLowerCase();
  const printAllKey = moduleNameLower?.startsWith('lesson')
    ? 'PRINT_ENTIRE_LESSON'
    : moduleNameLower?.startsWith('chapter')
      ? 'PRINT_ENTIRE_CHAPTER'
      : moduleNameLower?.startsWith('module')
        ? 'PRINT_ENTIRE_MODULE'
        : 'PRINT_ENTIRE_THING';

  const [unprintable, setUnprintable] = useState(false);

  const checkPrintable = () => {
    setUnprintable(printService.isUnprintable());
  };

  // The DropdownMenu should be `right` but there is a horrid bug that causes it to then
  // use a bogus width. Without `positionFixed` the `left` menu gets rendered offscreen.
  // What a sad state of affairs.
  return (
    <Dropdown onClick={checkPrintable}>
      <DropdownToggle
        id="print-button"
        color="primary"
        outline
        className="border-white px-2"
        aria-label={translate('PRINT_OPTIONS')}
        title={translate('PRINT_OPTIONS')}
      >
        <FiPrinter
          size="2rem"
          strokeWidth={1.25}
          aria-hidden={true}
        />
      </DropdownToggle>
      <DropdownMenu
        id="print-menu"
        positionFixed
      >
        {unprintable ? (
          <>
            <div id="print-page">
              <DropdownItem
                aria-label={translate('CANNOT_PRINT_PAGE')}
                disabled
              >
                {translate('PRINT_THIS_PAGE')}
              </DropdownItem>
            </div>
            <UncontrolledTooltip
              className=""
              placement="left"
              target="print-page"
            >
              {translate('CANNOT_PRINT_PAGE')}
            </UncontrolledTooltip>
          </>
        ) : (
          <DropdownItem
            id="print-page"
            onClick={() => go(ContentPrinterPageLink.toLink({ content }))}
          >
            {translate('PRINT_THIS_PAGE')}
          </DropdownItem>
        )}
        {module && viewingAs.isInstructor && allowPrintingEntireLesson && (
          <>
            <DropdownItem
              id="print-module-no-questions"
              onClick={() =>
                go(ContentPrinterPageLink.toLink({ content: module, questions: false }))
              }
            >
              {translate(printAllKey, { name: module.name })}
              <div className="text-muted small">{translate('PRINT_WITHOUT_QUESTIONS')}</div>
            </DropdownItem>
            {/* Disabled because without randomization, answers are probably obvious.
            {module && (
              <DropdownItem
                id="print-module-no-answers"
                onClick={() =>
                  go(ContentPrinterPageLink.toLink({ content: module, answers: false }))
                }
              >
                {translate(printAllKey, { name: module.name })}{' '}
                <div className="text-muted small">{translate('PRINT_WITHOUT_ANSWERS')}</div>
              </DropdownItem>
            )} */}
          </>
        )}
        {module && allowPrintingEntireLesson && (
          <DropdownItem
            id="print-module"
            onClick={() => go(ContentPrinterPageLink.toLink({ content: module }))}
          >
            {translate(printAllKey, { name: module?.name })}
            {viewingAs.isInstructor && (
              <div className="text-muted small">{translate('PRINT_WITH_ANSWERS')}</div>
            )}
          </DropdownItem>
        )}
      </DropdownMenu>
    </Dropdown>
  );
};

export default ERPrintButton;
