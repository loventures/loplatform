﻿/*
Copyright (c) 2003-2021, CKSource - Frederico Knabben. All rights reserved.
For licensing, see LICENSE.md or https://ckeditor.com/license
*/
CKEDITOR.lang['fo'] = {
  editor: 'Rich Text Editor',
  editorPanel: 'Rich Text Editor panel',
  common: {
    editorHelp: 'Trýst ALT og 0 fyri vegleiðing',
    browseServer: 'Ambætarakagi',
    url: 'URL',
    protocol: 'Protokoll',
    upload: 'Send til ambætaran',
    uploadSubmit: 'Send til ambætaran',
    image: 'Myndir',
    flash: 'Flash',
    form: 'Formur',
    checkbox: 'Flugubein',
    radio: 'Radioknøttur',
    textField: 'Tekstteigur',
    textarea: 'Tekstumráði',
    hiddenField: 'Fjaldur teigur',
    button: 'Knøttur',
    select: 'Valskrá',
    imageButton: 'Myndaknøttur',
    notSet: '<ikki sett>',
    id: 'Id',
    name: 'Navn',
    langDir: 'Tekstkós',
    langDirLtr: 'Frá vinstru til høgru (LTR)',
    langDirRtl: 'Frá høgru til vinstru (RTL)',
    langCode: 'Málkoda',
    longDescr: 'Víðkað URL frágreiðing',
    cssClass: 'Typografi klassar',
    advisoryTitle: 'Vegleiðandi heiti',
    cssStyle: 'Typografi',
    ok: 'Góðkent',
    cancel: 'Avlýs',
    close: 'Lat aftur',
    preview: 'Frumsýn',
    resize: 'Drag fyri at broyta stødd',
    generalTab: 'Generelt',
    advancedTab: 'Fjølbroytt',
    validateNumberFailed: 'Hetta er ikki eitt tal.',
    confirmNewPage:
      'Allar ikki goymdar broytingar í hesum innihaldið hvørva. Skal nýggj síða lesast kortini?',
    confirmCancel:
      'Nakrir valmøguleikar eru broyttir. Ert tú vísur í, at dialogurin skal latast aftur?',
    options: 'Options',
    target: 'Target',
    targetNew: 'Nýtt vindeyga (_blank)',
    targetTop: 'Vindeyga ovast (_top)',
    targetSelf: 'Sama vindeyga (_self)',
    targetParent: 'Upphavligt vindeyga (_parent)',
    langDirLTR: 'Frá vinstru til høgru (LTR)',
    langDirRTL: 'Frá høgru til vinstru (RTL)',
    styles: 'Style',
    cssClasses: 'Stylesheet Classes',
    width: 'Breidd',
    height: 'Hædd',
    align: 'Justering',
    left: 'Vinstra',
    right: 'Høgra',
    center: 'Miðsett',
    justify: 'Javnir tekstkantar',
    alignLeft: 'Vinstrasett',
    alignRight: 'Høgrasett',
    alignCenter: 'Align Center',
    alignTop: 'Ovast',
    alignMiddle: 'Miðja',
    alignBottom: 'Botnur',
    alignNone: 'Eingin',
    invalidValue: 'Invalid value.',
    invalidHeight: 'Hædd má vera eitt tal.',
    invalidWidth: 'Breidd má vera eitt tal.',
    invalidLength:
      'Value specified for the "%1" field must be a positive number with or without a valid measurement unit (%2).',
    invalidCssLength:
      'Virðið sett í "%1" feltið má vera eitt positivt tal, við ella uttan gyldugum CSS mátieind (px, %, in, cm, mm, em, ex, pt, ella pc).',
    invalidHtmlLength:
      'Virðið sett í "%1" feltiðield má vera eitt positivt tal, við ella uttan gyldugum CSS mátieind (px ella %).',
    invalidInlineStyle:
      'Virði specifiserað fyri inline style má hava eitt ella fleiri pør (tuples) skrivað sum "name : value", hvørt parið sundurskilt við semi-colon.',
    cssLengthTooltip:
      'Skriva eitt tal fyri eitt virði í pixels ella eitt tal við gyldigum CSS eind (px, %, in, cm, mm, em, ex, pt, ella pc).',
    unavailable: '%1<span class="cke_accessibility">, ikki tøkt</span>',
    keyboard: {
      8: 'Backspace',
      13: 'Enter',
      16: 'Shift',
      17: 'Ctrl',
      18: 'Alt',
      32: 'Space',
      35: 'End',
      36: 'Home',
      46: 'Delete',
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
    keyboardShortcut: 'Keyboard shortcut',
    optionDefault: 'Default',
  },
  about: {
    copy: 'Copyright &copy; $1. All rights reserved.',
    dlgTitle: 'Um CKEditor 4',
    moreInfo: 'Licens upplýsingar finnast á heimasíðu okkara:',
  },
  basicstyles: {
    bold: 'Feit skrift',
    italic: 'Skráskrift',
    strike: 'Yvirstrikað',
    subscript: 'Lækkað skrift',
    superscript: 'Hækkað skrift',
    underline: 'Undirstrikað',
  },
  notification: { closed: 'Notification closed.' },
  toolbar: {
    toolbarCollapse: 'Lat Toolbar aftur',
    toolbarExpand: 'Vís Toolbar',
    toolbarGroups: {
      document: 'Dokument',
      clipboard: 'Clipboard/Undo',
      editing: 'Editering',
      forms: 'Formar',
      basicstyles: 'Grundleggjandi Styles',
      paragraph: 'Reglubrot',
      links: 'Leinkjur',
      insert: 'Set inn',
      styles: 'Styles',
      colors: 'Litir',
      tools: 'Tól',
    },
    toolbars: 'Editor toolbars',
  },
  clipboard: {
    copy: 'Avrita',
    copyError:
      'Trygdaruppseting alnótskagans forðar tekstviðgeranum í at avrita tekstin. Vinarliga nýt knappaborðið til at avrita tekstin (Ctrl/Cmd+C).',
    cut: 'Kvett',
    cutError:
      'Trygdaruppseting alnótskagans forðar tekstviðgeranum í at kvetta tekstin. Vinarliga nýt knappaborðið til at kvetta tekstin (Ctrl/Cmd+X).',
    paste: 'Innrita',
    pasteNotification:
      'Press %1 to paste. Your browser doesn‘t support pasting with the toolbar button or context menu option.',
    pasteArea: 'Avritingarumráði',
    pasteMsg: 'Paste your content inside the area below and press OK.',
  },
  indent: { indent: 'Økja reglubrotarinntriv', outdent: 'Minka reglubrotarinntriv' },
  fakeobjects: {
    anchor: 'Anchor',
    flash: 'Flash Animation',
    hiddenfield: 'Fjaldur teigur',
    iframe: 'IFrame',
    unknown: 'Ókent Object',
  },
  link: {
    acccessKey: 'Snarvegisknöttur',
    advanced: 'Fjølbroytt',
    advisoryContentType: 'Vegleiðandi innihaldsslag',
    advisoryTitle: 'Vegleiðandi heiti',
    anchor: {
      toolbar: 'Ger/broyt marknastein',
      menu: 'Eginleikar fyri marknastein',
      title: 'Eginleikar fyri marknastein',
      name: 'Heiti marknasteinsins',
      errorName: 'Vinarliga rita marknasteinsins heiti',
      remove: 'Strika marknastein',
    },
    anchorId: 'Eftir element Id',
    anchorName: 'Eftir navni á marknasteini',
    charset: 'Atknýtt teknsett',
    cssClasses: 'Typografi klassar',
    download: 'Force Download',
    displayText: 'Display Text',
    emailAddress: 'Teldupost-adressa',
    emailBody: 'Breyðtekstur',
    emailSubject: 'Evni',
    id: 'Id',
    info: 'Tilknýtis upplýsingar',
    langCode: 'Tekstkós',
    langDir: 'Tekstkós',
    langDirLTR: 'Frá vinstru til høgru (LTR)',
    langDirRTL: 'Frá høgru til vinstru (RTL)',
    menu: 'Broyt tilknýti',
    name: 'Navn',
    noAnchors: '(Eingir marknasteinar eru í hesum dokumentið)',
    noEmail: 'Vinarliga skriva teldupost-adressu',
    noUrl: 'Vinarliga skriva tilknýti (URL)',
    noTel: 'Please type the phone number',
    other: '<annað>',
    phoneNumber: 'Phone number',
    popupDependent: 'Bundið (Netscape)',
    popupFeatures: 'Popup vindeygans víðkaðu eginleikar',
    popupFullScreen: 'Fullur skermur (IE)',
    popupLeft: 'Frástøða frá vinstru',
    popupLocationBar: 'Adressulinja',
    popupMenuBar: 'Skrábjálki',
    popupResizable: 'Stødd kann broytast',
    popupScrollBars: 'Rullibjálki',
    popupStatusBar: 'Støðufrágreiðingarbjálki',
    popupToolbar: 'Amboðsbjálki',
    popupTop: 'Frástøða frá íerva',
    rel: 'Relatión',
    selectAnchor: 'Vel ein marknastein',
    styles: 'Typografi',
    tabIndex: 'Tabulator indeks',
    target: 'Target',
    targetFrame: '<ramma>',
    targetFrameName: 'Vís navn vindeygans',
    targetPopup: '<popup vindeyga>',
    targetPopupName: 'Popup vindeygans navn',
    title: 'Tilknýti',
    toAnchor: 'Tilknýti til marknastein í tekstinum',
    toEmail: 'Teldupostur',
    toUrl: 'URL',
    toPhone: 'Phone',
    toolbar: 'Ger/broyt tilknýti',
    type: 'Tilknýtisslag',
    unlink: 'Strika tilknýti',
    upload: 'Send til ambætaran',
  },
  list: { bulletedlist: 'Punktmerktur listi', numberedlist: 'Talmerktur listi' },
  undo: { redo: 'Vend aftur', undo: 'Angra' },
  blockquote: { toolbar: 'Blockquote' },
  contextmenu: { options: 'Context Menu Options' },
  font: {
    fontSize: { label: 'Skriftstødd', voiceLabel: 'Skriftstødd', panelTitle: 'Skriftstødd' },
    label: 'Skrift',
    panelTitle: 'Skrift',
    voiceLabel: 'Skrift',
  },
  format: {
    label: 'Skriftsnið',
    panelTitle: 'Skriftsnið',
    tag_address: 'Adressa',
    tag_div: 'Vanligt (DIV)',
    tag_h1: 'Yvirskrift 1',
    tag_h2: 'Yvirskrift 2',
    tag_h3: 'Yvirskrift 3',
    tag_h4: 'Yvirskrift 4',
    tag_h5: 'Yvirskrift 5',
    tag_h6: 'Yvirskrift 6',
    tag_p: 'Vanligt',
    tag_pre: 'Sniðgivið',
  },
  horizontalrule: { toolbar: 'Ger vatnrætta linju' },
  liststyle: {
    bulletedTitle: 'Eginleikar fyri lista við prikkum',
    circle: 'Sirkul',
    decimal: 'Vanlig tøl (1, 2, 3, etc.)',
    disc: 'Disc',
    lowerAlpha: 'Lítlir bókstavir (a, b, c, d, e, etc.)',
    lowerRoman: 'Lítil rómaratøl (i, ii, iii, iv, v, etc.)',
    none: 'Einki',
    notset: '<ikki sett>',
    numberedTitle: 'Eginleikar fyri lista við tølum',
    square: 'Fýrkantur',
    start: 'Byrjan',
    type: 'Slag',
    upperAlpha: 'Stórir bókstavir (A, B, C, D, E, etc.)',
    upperRoman: 'Stór rómaratøl (I, II, III, IV, V, etc.)',
    validateStartNumber: 'Byrjunartalið fyri lista má vera eitt heiltal.',
  },
  maximize: { maximize: 'Maksimera', minimize: 'Minimera' },
  widget: { move: 'Click and drag to move', label: '%1 widget' },
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
  specialchar: {
    options: 'Møguleikar við serteknum',
    title: 'Vel sertekn',
    toolbar: 'Set inn sertekn',
  },
  stylescombo: {
    label: 'Typografi',
    panelTitle: 'Formatterings stílir',
    panelTitle1: 'Blokk stílir',
    panelTitle2: 'Inline stílir',
    panelTitle3: 'Object stílir',
  },
  table: {
    border: 'Bordabreidd',
    caption: 'Tabellfrágreiðing',
    cell: {
      menu: 'Meski',
      insertBefore: 'Set meska inn áðrenn',
      insertAfter: 'Set meska inn aftaná',
      deleteCell: 'Strika meskar',
      merge: 'Flætta meskar',
      mergeRight: 'Flætta meskar til høgru',
      mergeDown: 'Flætta saman',
      splitHorizontal: 'Kloyv meska vatnrætt',
      splitVertical: 'Kloyv meska loddrætt',
      title: 'Mesku eginleikar',
      cellType: 'Mesku slag',
      rowSpan: 'Ræð spenni',
      colSpan: 'Kolonnu spenni',
      wordWrap: 'Orðkloyving',
      hAlign: 'Horisontal plasering',
      vAlign: 'Loddrøtt plasering',
      alignBaseline: 'Basislinja',
      bgColor: 'Bakgrundslitur',
      borderColor: 'Bordalitur',
      data: 'Data',
      header: 'Header',
      yes: 'Ja',
      no: 'Nei',
      invalidWidth: 'Meskubreidd má vera eitt tal.',
      invalidHeight: 'Meskuhædd má vera eitt tal.',
      invalidRowSpan: 'Raðspennið má vera eitt heiltal.',
      invalidColSpan: 'Kolonnuspennið má vera eitt heiltal.',
      chooseColor: 'Vel',
    },
    cellPad: 'Meskubreddi',
    cellSpace: 'Fjarstøða millum meskar',
    column: {
      menu: 'Kolonna',
      insertBefore: 'Set kolonnu inn áðrenn',
      insertAfter: 'Set kolonnu inn aftaná',
      deleteColumn: 'Strika kolonnur',
    },
    columns: 'Kolonnur',
    deleteTable: 'Strika tabell',
    headers: 'Yvirskriftir',
    headersBoth: 'Báðir',
    headersColumn: 'Fyrsta kolonna',
    headersNone: 'Eingin',
    headersRow: 'Fyrsta rað',
    heightUnit: 'height unit',
    invalidBorder: 'Borda-stødd má vera eitt tal.',
    invalidCellPadding: 'Cell padding má vera eitt tal.',
    invalidCellSpacing: 'Cell spacing má vera eitt tal.',
    invalidCols: 'Talið av kolonnum má vera eitt tal størri enn 0.',
    invalidHeight: 'Tabell-hædd má vera eitt tal.',
    invalidRows: 'Talið av røðum má vera eitt tal størri enn 0.',
    invalidWidth: 'Tabell-breidd má vera eitt tal.',
    menu: 'Eginleikar fyri tabell',
    row: {
      menu: 'Rað',
      insertBefore: 'Set rað inn áðrenn',
      insertAfter: 'Set rað inn aftaná',
      deleteRow: 'Strika røðir',
    },
    rows: 'Røðir',
    summary: 'Samandráttur',
    title: 'Eginleikar fyri tabell',
    toolbar: 'Tabell',
    widthPc: 'prosent',
    widthPx: 'pixels',
    widthUnit: 'breiddar unit',
  },
  mathjax: {
    title: 'Mathematics in TeX',
    button: 'Math',
    dialogInput: 'Write your TeX here',
    docUrl: 'http://en.wikibooks.org/wiki/LaTeX/Mathematics',
    docLabel: 'TeX documentation',
    loading: 'loading...',
    pathName: 'math',
  },
};
