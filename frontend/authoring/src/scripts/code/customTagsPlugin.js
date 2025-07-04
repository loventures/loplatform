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

import $ from 'jquery';

/**
 * Plugin to add buttons for the Article and Aside insertion functionality. These buttons take
 *  selected text and wrap them in the desired tag, nothing else.
 * */
$.extend($.summernote.plugins, {
  /**
   * TODO: summernote.destroy should breakdown the tooltip.
   * */

  customtags: function (context) {
    const self = this;
    const ui = $.summernote.ui;
    const editable = context.layoutInfo.editable;

    context.memo(
      'button.article',
      ui
        .button({
          contents: `<i class="fa fa-list-alt" />`,
          tooltip: 'Article',
          container: 'body',
          click: () => {
            self.wrapInTag('article');
            context.triggerEvent('change', editable.html(), editable);
          },
        })
        .render()
    );

    context.memo(
      'button.aside',
      ui
        .button({
          contents: `<i class="fa fa-exclamation" />`,
          tooltip: 'Aside',
          container: 'body',
          click: () => {
            self.wrapInTag('aside');
            context.triggerEvent('change', editable.html(), editable);
          },
        })
        .render()
    );

    context.memo(
      'button.blur',
      ui
        .button({
          contents: `<span class="material-icons md-18 text-primary">check</span>`,
          tooltip: 'Save',
          container: 'body',
          click: () => {
            setTimeout(() => editable.blur(), 100);
          },
        })
        .render()
    );

    /**
     * wrapInTag, areDifferentBlockElements, isSelectionParsable are modified via MIT License
     *  from https://github.com/tylerecouture/summernote-add-text-tags
     * */

    self.areDifferentBlockElements = function (startEl, endEl) {
      var startElDisplay = getComputedStyle(startEl, null).display;
      var endElDisplay = getComputedStyle(endEl, null).display;

      if (startElDisplay !== 'inline' && endElDisplay !== 'inline') {
        console.log("Can't insert across two block elements.");
        return true;
      } else {
        return false;
      }
    };

    self.isSelectionParsable = function (startEl, endEl) {
      if (startEl.isSameNode(endEl)) {
        return true;
      }
      if (self.areDifferentBlockElements(startEl, endEl)) {
        return false;
      }
      // if they're not different block elements, then we need to check if they share a common block ancestor
      // could do this recursively, if we want to back farther up the node chain...
      var startElParent = startEl.parentElement;
      var endElParent = endEl.parentElement;
      if (
        startEl.isSameNode(endElParent) ||
        endEl.isSameNode(startElParent) ||
        startElParent.isSameNode(endElParent)
      ) {
        return true;
      } else console.log('Unable to parse across so many nodes. Sorry!');
      return false;
    };

    self.wrapInTag = function (tag) {
      // from: https://github.com/summernote/summernote/pull/1919#issuecomment-304545919
      // https://github.com/summernote/summernote/pull/1919#issuecomment-304707418

      if (window.getSelection) {
        var selection = window.getSelection(),
          selected = selection.rangeCount > 0 && selection.getRangeAt(0);

        // Only wrap tag around selected text
        if (selected.startOffset !== selected.endOffset) {
          var range = selected.cloneRange();

          var startParentElement = range.startContainer.parentElement;
          var endParentElement = range.endContainer.parentElement;

          // if the selection starts and ends different elements, we could be in trouble
          if (!startParentElement.isSameNode(endParentElement)) {
            if (!self.isSelectionParsable(startParentElement, endParentElement)) {
              return;
            }
          }

          var newNode = document.createElement(tag);
          // https://developer.mozilla.org/en-US/docs/Web/API/Range/surroundContents
          // Parses inline nodes, but not block based nodes...blocks are handled above.
          newNode.appendChild(range.extractContents());
          range.insertNode(newNode);

          // Restore the selections
          range.selectNodeContents(newNode);
          selection.removeAllRanges();
          selection.addRange(range);
        }
      }
    };
  },
});
