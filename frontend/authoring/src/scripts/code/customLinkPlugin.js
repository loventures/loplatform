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

import { openModal } from '../modals/modalActions';
import { ModalIds } from '../modals/modalIds';

/**
 * Custom link plugin for Summernote. Opens ebook/content link modals and inserts links.
 * */
$.extend($.summernote.plugins, {
  customLink: function (context) {
    const self = this;
    this.ui = $.summernote.ui;
    this.dom = $.summernote.dom;
    this.lists = $.summernote.lists;
    this.options = context.options;
    this.lang = this.options.langInfo;

    context.memo('button.content', function () {
      return self.ui
        .button({
          contents: `<span class="material-icons md-18">polyline</span>`,
          tooltip: 'Content Link',
          container: 'body',
          click: () => insertCustomLink(ModalIds.ContentHyperlinkModal),
        })
        .render();
    });

    context.memo('button.ebook', function () {
      return self.ui
        .button({
          contents: `<span class="material-icons md-18">menu_book</span>`,
          tooltip: 'E-Book Link',
          container: 'body',
          click: () => insertCustomLink(ModalIds.EBookEmbedModal),
        })
        .render();
    });

    function insertCustomLink(modalId) {
      const linkInfo = context.invoke('editor.getLinkInfo');
      context.invoke('editor.saveRange');
      const config = {
        callback: (title, href, isNewWindow, onClick) => {
          context.invoke('editor.restoreRange');
          createLink({
            ...linkInfo,
            text: linkInfo.text || title,
            url: href,
            isNewWindow,
            onClick,
          });
        },
        cancelback: () => {
          context.invoke('editor.restoreRange');
        },
        name: self.options.lo.name,
      };
      self.options.lo.dispatch(openModal(modalId, config));
    }

    // largely verbatim from summernote with onclick support...
    function createLink(linkInfo) {
      const linkUrl = linkInfo.url;
      const linkText = linkInfo.text;
      const isNewWindow = linkInfo.isNewWindow;
      const onClick = linkInfo.onClick;

      let rng = linkInfo.range || context.invoke('editor.getLastRange');

      const isTextChanged = rng.toString() !== linkText;

      let anchors = [];

      if (isTextChanged) {
        rng = rng.deleteContents();
        const anchor = rng.insertNode($('<A>' + linkText + '</A>')[0]);
        anchors.push(anchor);
      } else {
        anchors = styleNodes(rng, {
          nodeName: 'A',
          expandClosestSibling: true,
          onlyPartialContains: true,
        });
      }

      $.each(anchors, (idx, anchor) => {
        $(anchor).attr('href', linkUrl);

        if (isNewWindow) {
          $(anchor).attr('target', '_blank');
        } else {
          $(anchor).removeAttr('target');
        }
        if (onClick) {
          $(anchor).attr('onclick', onClick);
        } else {
          $(anchor).removeAttr('onclick');
        }
      });

      context.invoke(
        'editor.setLastRange',
        context.invoke('editor.createRangeFromList', anchors).select()
      );

      const editable = context.layoutInfo.editable;
      context.triggerEvent('change', editable.html(), editable);
    }

    // verbatim from summernote
    function styleNodes(rng, options) {
      rng = rng.splitText();
      const nodeName = (options && options.nodeName) || 'SPAN';
      const expandClosestSibling = !!(options && options.expandClosestSibling);
      const onlyPartialContains = !!(options && options.onlyPartialContains);

      if (rng.isCollapsed()) {
        return [rng.insertNode(self.dom.create(nodeName))];
      }

      let pred = self.dom.makePredByNodeName(nodeName);
      const nodes = rng
        .nodes(self.dom.isText, {
          fullyContains: true,
        })
        .map(function (text) {
          return self.dom.singleChildAncestor(text, pred) || self.dom.wrap(text, nodeName);
        });

      if (expandClosestSibling) {
        if (onlyPartialContains) {
          const nodesInRange = rng.nodes(); // compose with partial contains predication

          pred = func_and(pred, function (node) {
            return self.lists.contains(nodesInRange, node);
          });
        }

        return nodes.map(function (node) {
          const siblings = self.dom.withClosestSiblings(node, pred);
          const head = self.lists.head(siblings);
          const tails = self.lists.tail(siblings);
          $.each(tails, function (idx, elem) {
            self.dom.appendChildNodes(head, elem.childNodes);
            self.dom.remove(elem);
          });
          return self.lists.head(siblings);
        });
      } else {
        return nodes;
      }
    }

    // verbatim from summernote
    function func_and(fA, fB) {
      return function (item) {
        return fA(item) && fB(item);
      };
    }
  },
});
