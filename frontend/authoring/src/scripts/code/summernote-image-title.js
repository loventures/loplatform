import $ from 'jquery';

// https://github.com/asiffermann/summernote-image-title
// but it is shit, doesn't mark the document dirty, lays out wrong, etc.

const ipColumns = [
  'ip-pickup',
  'ip-asset-type',
  'ip-pickup-info',
  'ip-asset-source',
  'ip-asset-source-id',
  'ip-credit-line',
  'ip-description',
];

$.extend(true, $.summernote.lang, {
  'en-US': {
    imageTitle: {
      edit: 'Image attributes',
      urlLabel: 'URL',
      titleLabel: 'Title/Alt text',
      altLabel: 'Alternative Text',
      ipLabel: 'IP Information',
      'ip-asset-type': 'Asset Type',
      'ip-pickup-info': 'Pickup Info',
      'ip-asset-source': 'Asset Source',
      'ip-asset-source-id': 'Asset Source ID',
      'ip-credit-line': 'Credit Line',
      'ip-description': 'Description',
      submit: 'Submit',
    },
  },
});

$.extend($.summernote.plugins, {
  imageTitle: function (context) {
    var self = this;

    var ui = $.summernote.ui;
    // var $note = context.layoutInfo.note;
    var $editor = context.layoutInfo.editor;
    var $editable = context.layoutInfo.editable;

    if (typeof context.options.imageTitle === 'undefined') {
      context.options.imageTitle = {};
    }

    if (typeof context.options.imageTitle.specificAltField === 'undefined') {
      context.options.imageTitle.specificAltField = false;
    }

    var options = context.options;
    var lang = options.langInfo;

    context.memo('button.imageTitle', function () {
      var button = ui.button({
        contents: ui.icon(options.icons.pencil),
        tooltip: lang.imageTitle.edit,
        container: options.container,
        placement: options.placement,
        click: function () {
          context.invoke('imageTitle.show');
        },
      });

      return button.render();
    });

    this.initialize = function () {
      var $container = options.dialogsInBody ? $(document.body) : $editor;

      var body =
        '<div class="form-group">' +
        '<label>' +
        lang.imageTitle.urlLabel +
        '</label>' +
        '<input class="note-image-url form-control" type="text" />' +
        '</div>' +
        '<div class="form-group">' +
        '<label>' +
        lang.imageTitle.titleLabel +
        '</label>' +
        '<input class="note-image-title-text form-control" type="text" />' +
        '</div>' +
        `<div class="fw-bold mb-1 p-1" style="background: #eee">${lang.imageTitle.ipLabel}</div>` +
        '<div class="form-group">' +
        `<div class="d-flex">
             <label class="d-flex align-items-center gap-2"> <input class="form-control d-inline note-ip-pickup" type="radio" name="note-ip-pickup" value="New" style="width: 1rem; height: 1rem" /> New</label>
             <label class="d-flex align-items-center gap-2"> <input class="form-control d-inline note-ip-pickup" type="radio" name="note-ip-pickup" value="Pickup" style="width: 1rem; height: 1rem" /> Pickup</label>
           </div>` +
        ipColumns
          .filter(c => c !== 'ip-pickup')
          .map(
            col =>
              '<label>' +
              lang.imageTitle[col] +
              '</label>' +
              `<input class="note-${col} form-control" type="text" />`
          )
          .join('\n') +
        '</div>';

      var footer =
        '<button href="#" class="btn btn-primary note-btn note-btn-primary note-image-title-btn">' +
        lang.imageTitle.submit +
        '</button>';

      this.$dialog = ui
        .dialog({
          title: lang.imageTitle.edit,
          body: body,
          footer: footer,
        })
        .render()
        .appendTo($container);
    };

    this.destroy = function () {
      ui.hideDialog(this.$dialog);
      this.$dialog.remove();
    };

    this.bindEnterKey = function ($input, $btn) {
      $input.on('keypress', function (event) {
        if (event.keyCode === 13) {
          $btn.trigger('click');
        }
      });
    };

    this.show = function () {
      var $img = $($editable.data('target'));
      var imgInfo = {
        imgDom: $img,
        url: $img.attr('src'),
        title: $img.attr('title') || $img.attr('alt'),
      };
      for (const col of ipColumns) {
        imgInfo[col] = $img.attr(`data-${col}`);
      }
      this.showLinkDialog(imgInfo).then(function (imgInfo) {
        ui.hideDialog(self.$dialog);
        context.invoke('editor.beforeCommand');
        var $img = imgInfo.imgDom;

        $img.attr('src', imgInfo.url);

        if (imgInfo.title) {
          $img.attr('alt', imgInfo.title);
          $img.attr('title', imgInfo.title);
        } else {
          $img.attr('alt', '');
          $img.removeAttr('title');
        }

        for (const col of ipColumns) {
          if (imgInfo[col]) {
            $img.attr(`data-${col}`, imgInfo[col]);
          } else {
            $img.removeAttr(`data-${col}`);
          }
        }
        context.invoke('editor.afterCommand');
      });
    };

    this.showLinkDialog = function (imgInfo) {
      return $.Deferred(function (deferred) {
        var $imageUrl = self.$dialog.find('.note-image-url'),
          $imageTitle = self.$dialog.find('.note-image-title-text'),
          $editBtn = self.$dialog.find('.note-image-title-btn');

        ui.onDialogShown(self.$dialog, function () {
          context.triggerEvent('dialog.shown');

          $editBtn.click(function (event) {
            event.preventDefault();
            const result = {
              imgDom: imgInfo.imgDom,
              url: $imageUrl.val(),
              title: $imageTitle.val(),
            };
            for (const col of ipColumns) {
              const $input = self.$dialog.find(`.note-${col}`);
              if (col === 'ip-pickup') {
                result[col] = $input[0].checked ? 'New' : $input[1].checked ? 'Pickup' : '';
              } else {
                result[col] = $input.val();
              }
            }
            if (result.url !== imgInfo.url && options.callbacks.onImageUrl) {
              options.callbacks.onImageUrl(result.url, (url, metadata) => {
                result.url = url;
                Object.assign(result, metadata);
                deferred.resolve(result);
              });
            } else {
              deferred.resolve(result);
            }
          });

          $imageUrl.val(imgInfo.url);
          $imageTitle.val(imgInfo.title).trigger('focus');
          self.bindEnterKey($imageUrl, $editBtn);
          self.bindEnterKey($imageTitle, $editBtn);
          for (const col of ipColumns) {
            const $input = self.$dialog.find(`.note-${col}`);
            if (col === 'ip-pickup') {
              const isNew = imgInfo[col]?.toLowerCase().startsWith('n');
              const isPickup = imgInfo[col]?.toLowerCase().startsWith('p');
              $input[0].checked = isNew;
              $input[1].checked = isPickup;
            } else {
              $input.val(imgInfo[col]);
              self.bindEnterKey($input, $editBtn);
            }
          }
        });

        ui.onDialogHidden(self.$dialog, function () {
          $editBtn.off('click');

          if (deferred.state() === 'pending') {
            deferred.reject();
          }
        });

        ui.showDialog(self.$dialog);
      });
    };
  },
});
