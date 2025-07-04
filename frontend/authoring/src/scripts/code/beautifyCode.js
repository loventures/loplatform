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
import { replace } from 'lodash';

import { md5 } from './md5';

const idTags = 'p,li,dt,dd,h1,h2,h3,h4,h5,h6,th,hd,img,pre';

const normaliseWs = cleanCode => {
  const n = cleanCode.length;
  let i = 0;
  let result = '';
  while (i < n) {
    const j0 = cleanCode.indexOf('<pre', i),
      j = j0 < 0 ? n : j0;
    result = result + cleanCode.substring(i, j).replace(/\s+/g, ' ');
    const k0 = cleanCode.indexOf('</pre', j),
      k = k0 < 0 ? n : k0;
    result = result + cleanCode.substring(j, k);
    i = k;
  }
  return result;
};

const beautifyCodeImpl = (dirtyCode, dataIds) => {
  let cleanCode = dirtyCode;
  const injectDataIds = window.lo_platform?.config.effective.injectDataIds && dataIds !== false;

  $('body').append('<div id="soapBox" style="display:none;"></div>');
  const soapBox = $('#soapBox');

  // remove shit that some security plugins inject
  cleanCode = cleanCode.replace(/<html[^>]*>(.*?)<\/html>/g, '$1');
  cleanCode = cleanCode.replace(/<body[^>]*>(.*?)<\/body>/g, '$1');
  cleanCode = cleanCode.replace(/<head.*?<\/head>/g, '');
  cleanCode = cleanCode.replace(/<script.*?<\/script>/g, '');
  cleanCode = cleanCode.replace(/<style.*?<\/style>/g, '');

  // We have to insert these initial IDs before any cleaning so that the front-end IDs
  // match the back-end IDs so if someone added feedback on unedited content, the ID will
  // match after an edit.

  if (injectDataIds && !cleanCode.includes('data-id="')) {
    const found = new Set();
    soapBox[0].innerHTML = cleanCode;
    soapBox.find(idTags).each(function () {
      if (!this.getAttribute('data-id')) {
        const str = this.outerHTML;
        let index = 0;
        let md5Str;
        do {
          md5Str = md5(!index ? str : str + '-' + index).substring(0, 8);
          ++index;
        } while (found.has(md5Str));
        found.add(md5Str);
        this.setAttribute('data-id', md5Str);
      }
    });
    cleanCode = soapBox.html();
  }

  // remove shit that summernote adds
  cleanCode = cleanCode.replace(/ style="(?:font-(?:size|weight): inherit;)?"/g, '');
  cleanCode = cleanCode.replace(/<span>([^<]*)<\/span>/g, '$1');
  // nbsp to space (!)
  cleanCode = cleanCode.replace(/&nbsp;/g, ' ');
  // delete empty paragraphs
  cleanCode = cleanCode.replace(/<p[^>]*>\s*<\/p>/gi, '');
  // delete empty paragraphs with line break
  cleanCode = cleanCode.replace(/<p[^>]*>\s*<br>\s*<\/p>/g, '');
  // normalize whitespace runs to space
  cleanCode = normaliseWs(cleanCode);
  // remove whitespace around line breaks
  cleanCode = cleanCode.replace(/\s*<br>\s*/gi, '<br>');
  // no longer unwrap images for folks
  // cleanCode = cleanCode.replace(/<p[^>]*>\s*(<img[^>]*>)(?:<br>)?\s*<\/p>/, '$1');
  cleanCode = cleanCode.trim();

  // unknown
  cleanCode = cleanCode.replace(
    /<p><!--\[if !supportLists]-->Â· <!--\[endif]-->/gi,
    '<p class="ul-li">'
  );
  cleanCode = cleanCode.replace(
    /<p><!--\[if !supportLists]-->\d+. <!--\[endif]-->/gi,
    '<p class="ol-li">'
  );

  // avoid downloading all the images, especially if they are edgeid urls that download badly...
  cleanCode = replace(cleanCode, /<img[^>]*>/g, s => s.replace(' src=', ' data-img-src='));
  if (cleanCode.indexOf('<') < 0) cleanCode = `<p>${cleanCode}</p>`;

  soapBox[0].innerHTML = cleanCode;
  soapBox.find('p.ul-li').each(function () {
    $(this).wrap('<li class="ul-li" />');
  });
  soapBox.find('p.ol-li').each(function () {
    $(this).wrap('<li class="ol-li" />');
  });
  soapBox.find('p.ol-li, p.ul-li').contents().unwrap();
  soapBox.find('li.ol-li').each(function () {
    if (!$(this).prev().is('li.ol-li')) {
      $(this).addClass('first-li');
    }
  });
  soapBox.find('li.ul-li').each(function () {
    if (!$(this).prev().is('li.ul-li')) {
      $(this).addClass('first-li');
    }
  });
  soapBox.find('li.ol-li.first-li').each(function () {
    $(this).nextAll('li.ol-li').andSelf().wrapAll('<ol></ol>');
  });
  soapBox.find('li.ul-li.first-li').each(function () {
    $(this).nextAll('li.ul-li').andSelf().wrapAll('<ul></ul>');
  });
  soapBox.find('li.ol-li, li.ul-li').each(function () {
    $(this).removeAttr('class');
  });
  soapBox.find('img').each(function () {
    if (this.getAttribute('alt') == null) this.setAttribute('alt', '');
  });
  soapBox.find('script').remove();
  soapBox.find('link').remove();
  soapBox.find('style').remove();
  soapBox.find('head').remove();
  if (injectDataIds) {
    const visited = new Set();
    soapBox.find(idTags).each(function () {
      const id = this.getAttribute('data-id');
      if (!id || visited.has(id)) {
        const rndStr = Array.from(crypto.getRandomValues(new Uint8Array(4)))
          .map(b => b.toString(16).padStart(2, '0'))
          .join('');
        this.setAttribute('data-id', rndStr);
      } else {
        visited.add(id); // because summernote clones ids when you add a paragraph
      }
    });
  }

  cleanCode = soapBox.html();

  soapBox.remove();

  cleanCode = cleanCode.replace(/data-img-src/g, 'src');

  // \n\n<p>\nTEXT\n</p>
  cleanCode = cleanCode.replace(/\s*<p(\s[^>]*)?>\s*/g, '\n\n<p$1>\n');
  cleanCode = cleanCode.replace(/\s*<\/p>/g, '\n</p>');

  // \n<br... also div
  cleanCode = cleanCode.replace(/\s*<(br|div)/g, '\n<$1');

  // \n\n<ol>...\n</ol> also ul, table, img, hr, hN, pre
  cleanCode = cleanCode.replace(/\s*<(ol|ul|dl|table|img|h|pre)/g, '\n\n<$1');
  cleanCode = cleanCode.replace(/\s*<\/(ol|ul|dl|table|div)/g, '\n</$1');

  // \n  <li>...</li>
  cleanCode = cleanCode.replace(/\s*<(li|dt|dd|tbody)/g, '\n  <$1');
  cleanCode = cleanCode.replace(/\s*<\/tbody/g, '\n  </tbody');

  cleanCode = cleanCode.replace(/\s*<tr/g, '\n    <tr');
  cleanCode = cleanCode.replace(/\s*<\/tr/g, '\n    </tr');

  cleanCode = cleanCode.replace(/\s*<(td|th)/g, '\n      <$1');
  cleanCode = cleanCode.replace(/\s*<(td|th)>\s*<br>/g, '<$1><br>');

  // madness where they wrap <pre> in <code>.. in other courses they wrap <code> in <pre>
  cleanCode = cleanCode.replace(/\s*<code>\s*<pre/g, '\n\n<code>\n<pre');
  cleanCode = cleanCode.replace(/<\/pre>\s*<\/code>/g, '</pre>\n</code>');

  cleanCode = cleanCode.replace(/<b(\s[^>]*)?>/g, '<strong$1>');
  cleanCode = cleanCode.replace(/<\/b>/g, '</strong>');
  cleanCode = cleanCode.replace(/<i(\s[^>]*)?>/g, '<em$1>');
  cleanCode = cleanCode.replace(/<\/i>/g, '</em>');
  cleanCode = cleanCode.replace(/\xA0/g, '&nbsp;');

  return cleanCode.trim();
};

export const beautifyCode = (dirtyCode, dataIds) => {
  try {
    return beautifyCodeImpl(dirtyCode, dataIds);
  } catch (e) {
    return dirtyCode;
  }
};
