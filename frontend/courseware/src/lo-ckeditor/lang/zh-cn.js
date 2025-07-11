﻿/*
Copyright (c) 2003-2021, CKSource - Frederico Knabben. All rights reserved.
For licensing, see LICENSE.md or https://ckeditor.com/license
*/
CKEDITOR.lang['zh-cn'] = {
  editor: '所见即所得编辑器',
  editorPanel: '所见即所得编辑器面板',
  common: {
    editorHelp: '按 ALT+0 获得帮助',
    browseServer: '浏览服务器',
    url: 'URL',
    protocol: '协议',
    upload: '上传',
    uploadSubmit: '上传到服务器',
    image: '图像',
    flash: 'Flash',
    form: '表单',
    checkbox: '复选框',
    radio: '单选按钮',
    textField: '单行文本',
    textarea: '多行文本',
    hiddenField: '隐藏域',
    button: '按钮',
    select: '列表/菜单',
    imageButton: '图像按钮',
    notSet: '<没有设置>',
    id: 'ID',
    name: '名称',
    langDir: '语言方向',
    langDirLtr: '从左到右 (LTR)',
    langDirRtl: '从右到左 (RTL)',
    langCode: '语言代码',
    longDescr: '详细说明 URL',
    cssClass: '样式类名称',
    advisoryTitle: '标题',
    cssStyle: '行内样式',
    ok: '确定',
    cancel: '取消',
    close: '关闭',
    preview: '预览',
    resize: '拖拽以改变大小',
    generalTab: '常规',
    advancedTab: '高级',
    validateNumberFailed: '需要输入数字格式',
    confirmNewPage: '当前文档内容未保存，是否确认新建文档？',
    confirmCancel: '部分修改尚未保存，是否确认关闭对话框？',
    options: '选项',
    target: '目标窗口',
    targetNew: '新窗口 (_blank)',
    targetTop: '整页 (_top)',
    targetSelf: '本窗口 (_self)',
    targetParent: '父窗口 (_parent)',
    langDirLTR: '从左到右 (LTR)',
    langDirRTL: '从右到左 (RTL)',
    styles: '样式',
    cssClasses: '样式类',
    width: '宽度',
    height: '高度',
    align: '对齐方式',
    left: '左对齐',
    right: '右对齐',
    center: '居中',
    justify: '两端对齐',
    alignLeft: '左对齐',
    alignRight: '右对齐',
    alignCenter: '居中',
    alignTop: '顶端',
    alignMiddle: '居中',
    alignBottom: '底部',
    alignNone: '无',
    invalidValue: '无效的值。',
    invalidHeight: '高度必须为数字格式',
    invalidWidth: '宽度必须为数字格式',
    invalidLength: '为 "%1" 字段设置的值必须是一个正数或者没有一个有效的度量单位 (%2)。',
    invalidCssLength:
      '此“%1”字段的值必须为正数，可以包含或不包含一个有效的 CSS 长度单位(px, %, in, cm, mm, em, ex, pt 或 pc)',
    invalidHtmlLength:
      '此“%1”字段的值必须为正数，可以包含或不包含一个有效的 HTML 长度单位(px 或 %)',
    invalidInlineStyle: '内联样式必须为格式是以分号分隔的一个或多个“属性名 : 属性值”。',
    cssLengthTooltip:
      '输入一个表示像素值的数字，或加上一个有效的 CSS 长度单位(px, %, in, cm, mm, em, ex, pt 或 pc)。',
    unavailable: '%1<span class="cke_accessibility">，不可用</span>',
    keyboard: {
      8: '退格键',
      13: '回车键',
      16: 'Shift',
      17: 'Ctrl',
      18: 'Alt',
      32: '空格键',
      35: '行尾键',
      36: '行首键',
      46: '删除键',
      112: 'F1',
      113: 'F2',
      114: 'F3',
      115: 'F4',
      116: 'F5',
      117: 'F6',
      118: 'F7',
      119: 'F8',
      120: 'F9',
      121: 'F10',
      122: 'F11',
      123: 'F12',
      124: 'F13',
      125: 'F14',
      126: 'F15',
      127: 'F16',
      128: 'F17',
      129: 'F18',
      130: 'F19',
      131: 'F20',
      132: 'F21',
      133: 'F22',
      134: 'F23',
      135: 'F24',
      224: 'Command',
    },
    keyboardShortcut: '快捷键',
    optionDefault: '默认',
  },
  about: {
    copy: '版权所有 &copy; $1。<br />保留所有权利。',
    dlgTitle: '关于 CKEditor 4',
    moreInfo: '相关授权许可信息请访问我们的网站：',
  },
  basicstyles: {
    bold: '加粗',
    italic: '倾斜',
    strike: '删除线',
    subscript: '下标',
    superscript: '上标',
    underline: '下划线',
  },
  notification: { closed: '通知已关闭' },
  toolbar: {
    toolbarCollapse: '折叠工具栏',
    toolbarExpand: '展开工具栏',
    toolbarGroups: {
      document: '文档',
      clipboard: '剪贴板/撤销',
      editing: '编辑',
      forms: '表单',
      basicstyles: '基本格式',
      paragraph: '段落',
      links: '链接',
      insert: '插入',
      styles: '样式',
      colors: '颜色',
      tools: '工具',
    },
    toolbars: '工具栏',
  },
  clipboard: {
    copy: '复制',
    copyError:
      '您的浏览器安全设置不允许编辑器自动执行复制操作，请使用键盘快捷键(Ctrl/Cmd+C)来完成。',
    cut: '剪切',
    cutError:
      '您的浏览器安全设置不允许编辑器自动执行剪切操作，请使用键盘快捷键(Ctrl/Cmd+X)来完成。',
    paste: '粘贴',
    pasteNotification: '您的浏览器不支持通过工具栏或右键菜单进行粘贴，请按 %1 进行粘贴。',
    pasteArea: '粘贴区域',
    pasteMsg: '将您的内容粘贴到下方区域，然后按确定。',
  },
  indent: { indent: '增加缩进量', outdent: '减少缩进量' },
  fakeobjects: {
    anchor: '锚点',
    flash: 'Flash 动画',
    hiddenfield: '隐藏域',
    iframe: 'IFrame',
    unknown: '未知对象',
  },
  link: {
    acccessKey: '访问键',
    advanced: '高级',
    advisoryContentType: '内容类型',
    advisoryTitle: '标题',
    anchor: {
      toolbar: '插入/编辑锚点链接',
      menu: '锚点链接属性',
      title: '锚点链接属性',
      name: '锚点名称',
      errorName: '请输入锚点名称',
      remove: '删除锚点',
    },
    anchorId: '按锚点 ID',
    anchorName: '按锚点名称',
    charset: '字符编码',
    cssClasses: '样式类名称',
    download: '强制下载',
    displayText: '显示文本',
    emailAddress: '地址',
    emailBody: '内容',
    emailSubject: '主题',
    id: 'ID',
    info: '超链接信息',
    langCode: '语言代码',
    langDir: '语言方向',
    langDirLTR: '从左到右 (LTR)',
    langDirRTL: '从右到左 (RTL)',
    menu: '编辑超链接',
    name: '名称',
    noAnchors: '(此文档没有可用的锚点)',
    noEmail: '请输入电子邮件地址',
    noUrl: '请输入超链接地址',
    noTel: '请输入电话号码',
    other: '<其他>',
    phoneNumber: '电话号码',
    popupDependent: '依附 (NS)',
    popupFeatures: '弹出窗口属性',
    popupFullScreen: '全屏 (IE)',
    popupLeft: '左',
    popupLocationBar: '地址栏',
    popupMenuBar: '菜单栏',
    popupResizable: '可缩放',
    popupScrollBars: '滚动条',
    popupStatusBar: '状态栏',
    popupToolbar: '工具栏',
    popupTop: '右',
    rel: '关联',
    selectAnchor: '选择一个锚点',
    styles: '行内样式',
    tabIndex: 'Tab 键次序',
    target: '目标',
    targetFrame: '<框架>',
    targetFrameName: '目标框架名称',
    targetPopup: '<弹出窗口>',
    targetPopupName: '弹出窗口名称',
    title: '超链接',
    toAnchor: '页内锚点链接',
    toEmail: '电子邮件',
    toUrl: '地址',
    toPhone: '电话',
    toolbar: '插入/编辑超链接',
    type: '超链接类型',
    unlink: '取消超链接',
    upload: '上传',
  },
  list: { bulletedlist: '项目列表', numberedlist: '编号列表' },
  undo: { redo: '重做', undo: '撤消' },
  blockquote: { toolbar: '块引用' },
  contextmenu: { options: '快捷菜单选项' },
  font: {
    fontSize: { label: '大小', voiceLabel: '文字大小', panelTitle: '大小' },
    label: '字体',
    panelTitle: '字体',
    voiceLabel: '字体',
  },
  format: {
    label: '格式',
    panelTitle: '格式',
    tag_address: '地址',
    tag_div: '段落(DIV)',
    tag_h1: '标题 1',
    tag_h2: '标题 2',
    tag_h3: '标题 3',
    tag_h4: '标题 4',
    tag_h5: '标题 5',
    tag_h6: '标题 6',
    tag_p: '普通',
    tag_pre: '已编排格式',
  },
  horizontalrule: { toolbar: '插入水平线' },
  liststyle: {
    bulletedTitle: '项目列表属性',
    circle: '空心圆',
    decimal: '数字 (1, 2, 3, 等)',
    disc: '实心圆',
    lowerAlpha: '小写英文字母(a, b, c, d, e, 等)',
    lowerRoman: '小写罗马数字(i, ii, iii, iv, v, 等)',
    none: '无标记',
    notset: '<没有设置>',
    numberedTitle: '编号列表属性',
    square: '实心方块',
    start: '开始序号',
    type: '标记类型',
    upperAlpha: '大写英文字母(A, B, C, D, E, 等)',
    upperRoman: '大写罗马数字(I, II, III, IV, V, 等)',
    validateStartNumber: '列表开始序号必须为整数格式',
  },
  maximize: { maximize: '全屏', minimize: '最小化' },
  widget: { move: '点击并拖拽以移动', label: '%1 小部件' },
  oembed: {
    title: 'Embed Media Content (Photo, Video, Audio or Rich Content)',
    button: 'Embed Media from External Sites',
    pasteUrl:
      'Paste a URL (shorted URLs are also supported) from one of the supported sites (e.g. YouTube, Flickr, Qik, Vimeo, Hulu, Viddler, MyOpera, etc.).',
    invalidUrl: 'Please provide a valid URL.',
    noEmbedCode: 'No embed code found, or site is not supported.',
    embedTitle: "Title (or aria-label) <span class='oembed-required'>[Required]</span>:",
    url: "URL <span class='oembed-required'>[Required]</span>:",
    width: 'Width:',
    height: 'Height:',
    widthTitle: 'Width for the embeded content',
    heightTitle: 'Height for the embeded content',
    maxWidth: 'Max. Width:',
    maxHeight: 'Max. Height:',
    maxWidthTitle: 'Maximum Width for the embeded Content',
    maxHeightTitle: 'Maximum Height for the embeded Content',
    none: 'None',
    resizeType: 'Resize Type (videos only):',
    noresize: 'No Resize (use default)',
    responsive: 'Responsive Resize',
    custom: 'Specific Resize',
    noVimeo:
      'The owner of this video has set domain restrictions and you will not be able to embed it on your website.',
    Error: 'Media Content could not been retrieved, please try a different URL.',
    titleError: 'Media Title is required to meet accessibility standards.',
  },
  specialchar: { options: '特殊符号选项', title: '选择特殊符号', toolbar: '插入特殊符号' },
  stylescombo: {
    label: '样式',
    panelTitle: '样式',
    panelTitle1: '块级元素样式',
    panelTitle2: '内联元素样式',
    panelTitle3: '对象元素样式',
  },
  table: {
    border: '边框',
    caption: '标题',
    cell: {
      menu: '单元格',
      insertBefore: '在左侧插入单元格',
      insertAfter: '在右侧插入单元格',
      deleteCell: '删除单元格',
      merge: '合并单元格',
      mergeRight: '向右合并单元格',
      mergeDown: '向下合并单元格',
      splitHorizontal: '水平拆分单元格',
      splitVertical: '垂直拆分单元格',
      title: '单元格属性',
      cellType: '单元格类型',
      rowSpan: '纵跨行数',
      colSpan: '横跨列数',
      wordWrap: '自动换行',
      hAlign: '水平对齐',
      vAlign: '垂直对齐',
      alignBaseline: '基线',
      bgColor: '背景颜色',
      borderColor: '边框颜色',
      data: '数据',
      header: '表头',
      yes: '是',
      no: '否',
      invalidWidth: '单元格宽度必须为数字格式',
      invalidHeight: '单元格高度必须为数字格式',
      invalidRowSpan: '行跨度必须为整数格式',
      invalidColSpan: '列跨度必须为整数格式',
      chooseColor: '选择',
    },
    cellPad: '边距',
    cellSpace: '间距',
    column: {
      menu: '列',
      insertBefore: '在左侧插入列',
      insertAfter: '在右侧插入列',
      deleteColumn: '删除列',
    },
    columns: '列数',
    deleteTable: '删除表格',
    headers: '标题单元格',
    headersBoth: '第一列和第一行',
    headersColumn: '第一列',
    headersNone: '无',
    headersRow: '第一行',
    heightUnit: '高度单位',
    invalidBorder: '边框粗细必须为数字格式',
    invalidCellPadding: '单元格填充必须为数字格式',
    invalidCellSpacing: '单元格间距必须为数字格式',
    invalidCols: '指定的行数必须大于零',
    invalidHeight: '表格高度必须为数字格式',
    invalidRows: '指定的列数必须大于零',
    invalidWidth: '表格宽度必须为数字格式',
    menu: '表格属性',
    row: {
      menu: '行',
      insertBefore: '在上方插入行',
      insertAfter: '在下方插入行',
      deleteRow: '删除行',
    },
    rows: '行数',
    summary: '摘要',
    title: '表格属性',
    toolbar: '表格',
    widthPc: '百分比',
    widthPx: '像素',
    widthUnit: '宽度单位',
  },
  mathjax: {
    title: 'TeX 语法的数学公式编辑器',
    button: '数学公式',
    dialogInput: '在此编写您的 TeX 指令',
    docUrl: 'http://zh.wikipedia.org/wiki/TeX',
    docLabel: 'TeX 语法（可以参考维基百科自身关于数学公式显示方式的帮助）',
    loading: '正在加载...',
    pathName: '数字公式',
  },
};
