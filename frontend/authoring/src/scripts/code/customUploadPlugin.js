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

import ModalService from '../modals/ModalService';
import { beautifyCode } from './beautifyCode';

/**
 * Custom upload plugin for Summernote. This simply calls out to our existing modals for the upload
 * and asset creation. We get the resulting html to insert and do what we're told.
 * */
$.extend($.summernote.plugins, {
  customUpload: function (context) {
    const self = this;
    this.ui = $.summernote.ui;
    this.options = context.options;
    this.lang = this.options.langInfo;

    context.memo('button.file', function () {
      return self.ui
        .button({
          contents: `<i class="fa fa-file-o" style="font-style: normal" />`,
          tooltip: 'Insert File',
          container: 'body',
          click: () => {
            context.invoke('customUpload.show', 'file.1');
          },
        })
        .render();
    });

    context.memo('button.image', function () {
      return self.ui
        .button({
          contents: `<i class="fa fa-picture-o" style="font-style: normal" />`,
          tooltip: 'Insert Image',
          container: 'body',
          click: () => {
            context.invoke('customUpload.show', 'image.1');
          },
        })
        .render();
    });

    context.memo('button.fitb', function () {
      return self.ui
        .button({
          contents: `<span class="material-icons md-18">edit_attributes</span>`,
          tooltip: 'Insert Blank',
          container: 'body',
          click: () => {
            context.invoke('editor.insertText', '{{Answer 1;Answer 2}}');
          },
        })
        .render();
    });

    // this is under the style menu already so probably pointless
    context.memo('button.h6', function () {
      return self.ui
        .button({
          contents: `<svg width="16px" height="16px" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><g>
<path d="M11 17H9v-4H5v4H3V7h2v4h4V7h2v10zm8.58-7.508c-.248-.204-.524-.37-.82-.49-.625-.242-1.317-.242-1.94 0-.3.11-.566.287-.78.52-.245.27-.432.586-.55.93-.16.46-.243.943-.25 1.43.367-.33.79-.59 1.25-.77.405-.17.84-.262 1.28-.27.415-.006.83.048 1.23.16.364.118.704.304 1 .55.295.253.528.57.68.93.193.403.302.843.32 1.29.01.468-.094.93-.3 1.35-.206.387-.49.727-.83 1-.357.287-.764.504-1.2.64-.98.31-2.033.293-3-.05-.507-.182-.968-.472-1.35-.85-.437-.416-.778-.92-1-1.48-.243-.693-.352-1.426-.32-2.16-.02-.797.11-1.59.38-2.34.215-.604.556-1.156 1-1.62.406-.416.897-.74 1.44-.95.54-.21 1.118-.314 1.7-.31.682-.02 1.36.096 2 .34.5.19.962.464 1.37.81l-1.31 1.34zm-2.39 5.84c.202 0 .405-.03.6-.09.183-.046.356-.128.51-.24.15-.136.27-.303.35-.49.092-.225.136-.467.13-.71.037-.405-.123-.804-.43-1.07-.328-.23-.72-.347-1.12-.33-.346-.002-.687.07-1 .21-.383.17-.724.418-1 .73.046.346.143.683.29 1 .108.23.257.44.44.62.152.15.337.26.54.33.225.055.46.068.69.04z"/>
</g></svg>`,
          tooltip: 'Insert H6',
          container: 'body',
          click: () => {
            context.invoke('editor.formatH6');
            //insertHtml('<h6></h6>');
          },
        })
        .render();
    });

    context.memo('button.done', function () {
      return self.ui
        .button({
          contents: `<span class="material-icons md-18 text-primary">check</span>`,
          tooltip: 'Done Editing',
          container: 'body',
          click: () => {
            self.options.lo.doneEditing();
          },
        })
        .render();
    });

    context.memo('button.newline', function () {
      return self.ui
        .button({
          contents: `<span class="material-icons md-18">keyboard_return</span>`,
          tooltip: 'Append Paragraph (SHIFT to prepend)',
          container: 'body',
          click: e => {
            // If you are in a UL or div this annoyingly splits the UL container so it
            // goes UL P UL.. But at least you can type something in and then delete the UL.
            // context.invoke('editor.pasteHTML', '<p><br></p>');
            pasteTopLevelHTML('<p><br></p>', e.shiftKey);
          },
        })
        .render();
    });

    context.memo('button.beautify', function () {
      return self.ui
        .button({
          contents: '<i class="fa fa-medkit" style="font-style: normal"/>',
          tooltip: 'Beautify HTML Code',
          container: 'body',
          click: function () {
            const dirtyCode = context.invoke('code');

            const cleanCode = beautifyCode(dirtyCode) + '\n\n<p><br></p>'; // add this at the end so summernote works ok

            context.invoke('code', cleanCode);
          },
        })
        .render();
    });

    // invoked from above...
    this.show = typeId => {
      context.invoke('editor.saveRange');
      ModalService.openAddFileModal(typeId)
        .then(html => {
          if (html) context.invoke('editor.pasteHTML', html);
        })
        .catch(() => {
          context.invoke('editor.restoreRange');
        });
    };

    // For cmd-enter
    self.done = () => self.options.lo.doneEditing();

    const pasteTopLevelHTML = function (html, before) {
      const selection = window.getSelection(),
        selected = selection.rangeCount > 0 && selection.getRangeAt(0);
      const range = selected.cloneRange();

      let element = range.endContainer;
      let parent = element.parentElement;
      while (parent && parent.contentEditable !== 'true') {
        element = parent;
        parent = element.parentElement;
      }
      if (parent) {
        const newNode = $(html)[0];
        parent.insertBefore(newNode, before ? element : element.nextSibling);
        range.setStart(newNode, 0);
        range.setEnd(newNode, 0);
        selection.removeAllRanges();
        selection.addRange(range);
      }
    };

    // comes from our other custom plugin, updated to not not work, but doubtfully useful
    // would need to use range.commonAncestorContainer to be better I think
    // eslint-disable-next-line no-unused-vars
    const wrapInTag = function (tag) {
      if (window.getSelection) {
        var selection = window.getSelection(),
          selected = selection.rangeCount > 0 && selection.getRangeAt(0);

        // Only wrap tag around selected text
        if (selected.startOffset !== selected.endOffset) {
          var range = selected.cloneRange();
          // suspect I want to find the least common parent of the start and end
          // and adjust the start and end to encompass the entirey of the parent
          // range from start to end.

          var newNode = $(tag)[0];
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
