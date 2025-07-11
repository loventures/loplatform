﻿/*
 Copyright (c) 2003-2021, CKSource - Frederico Knabben. All rights reserved.
 For licensing, see LICENSE.md or https://ckeditor.com/legal/ckeditor-oss-license
*/
(function () {
  function w(a) {
    for (var f = 0, p = 0, n = 0, q, d = a.$.rows.length; n < d; n++) {
      q = a.$.rows[n];
      for (var e = (f = 0), b, c = q.cells.length; e < c; e++) (b = q.cells[e]), (f += b.colSpan);
      f > p && (p = f);
    }
    return p;
  }
  function t(a) {
    return function () {
      var f = this.getValue(),
        f = !!(CKEDITOR.dialog.validate.integer().call(this, f) && 0 < f);
      f || (alert(a), this.select());
      return f;
    };
  }
  function r(a, f) {
    var p = function (d) {
        return new CKEDITOR.dom.element(d, a.document);
      },
      r = a.editable(),
      q = a.plugins.dialogadvtab;
    return {
      title: a.lang.table.title,
      minWidth: 310,
      minHeight: CKEDITOR.env.ie ? 310 : 280,
      getModel: function (d) {
        return 'tableProperties' !== this.dialog.getName()
          ? null
          : (d = (d = d.getSelection()) && d.getRanges()[0])
            ? d._getTableElement({ table: 1 })
            : null;
      },
      onLoad: function () {
        var d = this,
          a = d.getContentElement('advanced', 'advStyles');
        if (a)
          a.on('change', function () {
            var a = this.getStyle('width', ''),
              c = d.getContentElement('info', 'txtWidth');
            c && c.setValue(a, !0);
            a = this.getStyle('height', '');
            (c = d.getContentElement('info', 'txtHeight')) && c.setValue(a, !0);
          });
      },
      onShow: function () {
        var d = a.getSelection(),
          e = d.getRanges(),
          b,
          c = this.getContentElement('info', 'txtRows'),
          h = this.getContentElement('info', 'txtCols'),
          u = this.getContentElement('info', 'txtWidth'),
          l = this.getContentElement('info', 'txtHeight');
        'tableProperties' == f &&
          ((d = d.getSelectedElement()) && d.is('table')
            ? (b = d)
            : 0 < e.length &&
              (CKEDITOR.env.webkit && e[0].shrink(CKEDITOR.NODE_ELEMENT),
              (b = a.elementPath(e[0].getCommonAncestor(!0)).contains('table', 1))),
          (this._.selectedElement = b));
        b
          ? (this.setupContent(b), c && c.disable(), h && h.disable())
          : (c && c.enable(), h && h.enable());
        u && u.onChange();
        l && l.onChange();
      },
      onOk: function () {
        var d = a.getSelection(),
          e = this._.selectedElement && d.createBookmarks(),
          b = this._.selectedElement || p('table'),
          c = {};
        this.commitContent(c, b);
        if (c.info) {
          c = c.info;
          if (!this._.selectedElement)
            for (
              var h = b.append(p('tbody')),
                f = parseInt(c.txtRows, 10) || 0,
                l = parseInt(c.txtCols, 10) || 0,
                k = 0;
              k < f;
              k++
            )
              for (var g = h.append(p('tr')), m = 0; m < l; m++) g.append(p('td')).appendBogus();
          f = c.selHeaders;
          if (!b.$.tHead && ('row' == f || 'both' == f)) {
            g = b.getElementsByTag('thead').getItem(0);
            h = b.getElementsByTag('tbody').getItem(0);
            l = h.getElementsByTag('tr').getItem(0);
            g || ((g = new CKEDITOR.dom.element('thead')), g.insertBefore(h));
            for (k = 0; k < l.getChildCount(); k++)
              (h = l.getChild(k)),
                h.type != CKEDITOR.NODE_ELEMENT ||
                  h.data('cke-bookmark') ||
                  (h.renameNode('th'), h.setAttribute('scope', 'col'));
            g.append(l.remove());
          }
          if (null !== b.$.tHead && 'row' != f && 'both' != f) {
            g = new CKEDITOR.dom.element(b.$.tHead);
            for (h = b.getElementsByTag('tbody').getItem(0); 0 < g.getChildCount(); ) {
              l = g.getFirst();
              for (k = 0; k < l.getChildCount(); k++)
                (m = l.getChild(k)),
                  m.type == CKEDITOR.NODE_ELEMENT &&
                    (m.renameNode('td'), m.removeAttribute('scope'));
              h.append(l, !0);
            }
            g.remove();
          }
          if (!this.hasColumnHeaders && ('col' == f || 'both' == f))
            for (g = 0; g < b.$.rows.length; g++)
              (m = new CKEDITOR.dom.element(b.$.rows[g].cells[0])),
                m.renameNode('th'),
                m.setAttribute('scope', 'row');
          if (this.hasColumnHeaders && 'col' != f && 'both' != f)
            for (k = 0; k < b.$.rows.length; k++)
              (g = new CKEDITOR.dom.element(b.$.rows[k])),
                'tbody' == g.getParent().getName() &&
                  ((m = new CKEDITOR.dom.element(g.$.cells[0])),
                  m.renameNode('td'),
                  m.removeAttribute('scope'));
          c.txtHeight ? b.setStyle('height', c.txtHeight) : b.removeStyle('height');
          c.txtWidth ? b.setStyle('width', c.txtWidth) : b.removeStyle('width');
          b.getAttribute('style') || b.removeAttribute('style');
        }
        if (this._.selectedElement)
          try {
            d.selectBookmarks(e);
          } catch (n) {}
        else
          a.insertElement(b),
            setTimeout(function () {
              var d = new CKEDITOR.dom.element(b.$.rows[0].cells[0]),
                c = a.createRange();
              c.moveToPosition(d, CKEDITOR.POSITION_AFTER_START);
              c.select();
            }, 0);
      },
      contents: [
        {
          id: 'info',
          label: a.lang.table.title,
          elements: [
            {
              type: 'hbox',
              widths: [null, null],
              styles: ['vertical-align:top'],
              children: [
                {
                  type: 'vbox',
                  padding: 0,
                  children: [
                    {
                      type: 'text',
                      id: 'txtRows',
                      default: 3,
                      label: a.lang.table.rows,
                      required: !0,
                      controlStyle: 'width:5em',
                      validate: t(a.lang.table.invalidRows),
                      setup: function (d) {
                        this.setValue(d.$.rows.length);
                      },
                      commit: n,
                    },
                    {
                      type: 'text',
                      id: 'txtCols',
                      default: 2,
                      label: a.lang.table.columns,
                      required: !0,
                      controlStyle: 'width:5em',
                      validate: t(a.lang.table.invalidCols),
                      setup: function (d) {
                        this.setValue(w(d));
                      },
                      commit: n,
                    },
                    { type: 'html', html: '\x26nbsp;' },
                    {
                      type: 'select',
                      id: 'selHeaders',
                      requiredContent: 'th',
                      default: '',
                      label: a.lang.table.headers,
                      items: [
                        [a.lang.table.headersNone, ''],
                        [a.lang.table.headersRow, 'row'],
                        [a.lang.table.headersColumn, 'col'],
                        [a.lang.table.headersBoth, 'both'],
                      ],
                      setup: function (d) {
                        var a = this.getDialog();
                        a.hasColumnHeaders = !0;
                        for (var b = 0; b < d.$.rows.length; b++) {
                          var c = d.$.rows[b].cells[0];
                          if (c && 'th' != c.nodeName.toLowerCase()) {
                            a.hasColumnHeaders = !1;
                            break;
                          }
                        }
                        null !== d.$.tHead
                          ? this.setValue(a.hasColumnHeaders ? 'both' : 'row')
                          : this.setValue(a.hasColumnHeaders ? 'col' : '');
                      },
                      commit: n,
                    },
                    {
                      type: 'text',
                      id: 'txtBorder',
                      requiredContent: 'table[border]',
                      default: a.filter.check('table[border]') ? 1 : 0,
                      label: a.lang.table.border,
                      controlStyle: 'width:3em',
                      validate: CKEDITOR.dialog.validate.number(a.lang.table.invalidBorder),
                      setup: function (d) {
                        this.setValue(d.getAttribute('border') || '');
                      },
                      commit: function (d, a) {
                        this.getValue()
                          ? a.setAttribute('border', this.getValue())
                          : a.removeAttribute('border');
                      },
                    },
                    {
                      id: 'cmbAlign',
                      type: 'select',
                      requiredContent: 'table[align]',
                      default: '',
                      label: a.lang.common.align,
                      items: [
                        [a.lang.common.notSet, ''],
                        [a.lang.common.left, 'left'],
                        [a.lang.common.center, 'center'],
                        [a.lang.common.right, 'right'],
                      ],
                      setup: function (a) {
                        this.setValue(a.getAttribute('align') || '');
                      },
                      commit: function (a, e) {
                        this.getValue()
                          ? e.setAttribute('align', this.getValue())
                          : e.removeAttribute('align');
                      },
                    },
                  ],
                },
                {
                  type: 'vbox',
                  padding: 0,
                  children: [
                    {
                      type: 'hbox',
                      widths: ['5em'],
                      children: [
                        {
                          type: 'text',
                          id: 'txtWidth',
                          requiredContent: 'table{width}',
                          controlStyle: 'width:5em',
                          label: a.lang.common.width,
                          title: a.lang.common.cssLengthTooltip,
                          default: a.filter.check('table{width}')
                            ? 500 > r.getSize('width')
                              ? '100%'
                              : 500
                            : 0,
                          getValue: v,
                          validate: CKEDITOR.dialog.validate.cssLength(
                            a.lang.common.invalidCssLength.replace('%1', a.lang.common.width)
                          ),
                          onChange: function () {
                            var a = this.getDialog().getContentElement('advanced', 'advStyles');
                            a && a.updateStyle('width', this.getValue());
                          },
                          setup: function (a) {
                            a = a.getStyle('width');
                            this.setValue(a);
                          },
                          commit: n,
                        },
                      ],
                    },
                    {
                      type: 'hbox',
                      widths: ['5em'],
                      children: [
                        {
                          type: 'text',
                          id: 'txtHeight',
                          requiredContent: 'table{height}',
                          controlStyle: 'width:5em',
                          label: a.lang.common.height,
                          title: a.lang.common.cssLengthTooltip,
                          default: '',
                          getValue: v,
                          validate: CKEDITOR.dialog.validate.cssLength(
                            a.lang.common.invalidCssLength.replace('%1', a.lang.common.height)
                          ),
                          onChange: function () {
                            var a = this.getDialog().getContentElement('advanced', 'advStyles');
                            a && a.updateStyle('height', this.getValue());
                          },
                          setup: function (a) {
                            (a = a.getStyle('height')) && this.setValue(a);
                          },
                          commit: n,
                        },
                      ],
                    },
                    { type: 'html', html: '\x26nbsp;' },
                    {
                      type: 'text',
                      id: 'txtCellSpace',
                      requiredContent: 'table[cellspacing]',
                      controlStyle: 'width:3em',
                      label: a.lang.table.cellSpace,
                      default: a.filter.check('table[cellspacing]') ? 1 : 0,
                      validate: CKEDITOR.dialog.validate.number(a.lang.table.invalidCellSpacing),
                      setup: function (a) {
                        this.setValue(a.getAttribute('cellSpacing') || '');
                      },
                      commit: function (a, e) {
                        this.getValue()
                          ? e.setAttribute('cellSpacing', this.getValue())
                          : e.removeAttribute('cellSpacing');
                      },
                    },
                    {
                      type: 'text',
                      id: 'txtCellPad',
                      requiredContent: 'table[cellpadding]',
                      controlStyle: 'width:3em',
                      label: a.lang.table.cellPad,
                      default: a.filter.check('table[cellpadding]') ? 1 : 0,
                      validate: CKEDITOR.dialog.validate.number(a.lang.table.invalidCellPadding),
                      setup: function (a) {
                        this.setValue(a.getAttribute('cellPadding') || '');
                      },
                      commit: function (a, e) {
                        this.getValue()
                          ? e.setAttribute('cellPadding', this.getValue())
                          : e.removeAttribute('cellPadding');
                      },
                    },
                  ],
                },
              ],
            },
            { type: 'html', align: 'right', html: '' },
            {
              type: 'vbox',
              padding: 0,
              children: [
                {
                  type: 'text',
                  id: 'txtCaption',
                  requiredContent: 'caption',
                  label: a.lang.table.caption,
                  setup: function (a) {
                    this.enable();
                    a = a.getElementsByTag('caption');
                    if (0 < a.count()) {
                      a = a.getItem(0);
                      var e = a.getFirst(CKEDITOR.dom.walker.nodeType(CKEDITOR.NODE_ELEMENT));
                      e && !e.equals(a.getBogus())
                        ? (this.disable(), this.setValue(a.getText()))
                        : ((a = CKEDITOR.tools.trim(a.getText())), this.setValue(a));
                    }
                  },
                  commit: function (d, e) {
                    if (this.isEnabled()) {
                      var b = this.getValue(),
                        c = e.getElementsByTag('caption');
                      if (b)
                        0 < c.count()
                          ? ((c = c.getItem(0)), c.setHtml(''))
                          : ((c = new CKEDITOR.dom.element('caption', a.document)),
                            e.append(c, !0)),
                          c.append(new CKEDITOR.dom.text(b, a.document));
                      else if (0 < c.count())
                        for (b = c.count() - 1; 0 <= b; b--) c.getItem(b).remove();
                    }
                  },
                },
                {
                  type: 'text',
                  id: 'txtSummary',
                  bidi: !0,
                  requiredContent: 'table[summary]',
                  label: a.lang.table.summary,
                  setup: function (a) {
                    this.setValue(a.getAttribute('summary') || '');
                  },
                  commit: function (a, e) {
                    this.getValue()
                      ? e.setAttribute('summary', this.getValue())
                      : e.removeAttribute('summary');
                  },
                },
              ],
            },
          ],
        },
        q && q.createAdvancedTab(a, null, 'table'),
      ],
    };
  }
  var v = CKEDITOR.tools.cssLength,
    n = function (a) {
      var f = this.id;
      a.info || (a.info = {});
      a.info[f] = this.getValue();
    };
  CKEDITOR.dialog.add('table', function (a) {
    return r(a, 'table');
  });
  CKEDITOR.dialog.add('tableProperties', function (a) {
    return r(a, 'tableProperties');
  });
})();
