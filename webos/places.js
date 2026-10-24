/*
 * Pictures of places to visit, the same as the Android launcher's (PlaceWallpapers.java): the pictures
 * Bing showed on its home page over the past days, renewed weekly, named with their country. Pictures
 * whose caption names people are skipped; webOS has no face detector, so that is the only people check
 * here. Pictures whose caption names no country are skipped too.
 */
var Places = (function () {
  'use strict';
  var PEOPLE = new RegExp('\\b(people|persons?|man|men|woman|women|girls?|boys?' +
    '|child|children|kids?|famil(y|ies)|crowds?|festivals?|parades?|carnivals?|danc(er|ers|ing)' +
    '|surf(er|ers|ing)|hik(er|ers|ing)|climb(er|ers)|ski(er|ers)|swimm(er|ers)|fisherm[ae]n|monks?' +
    '|pilgrims?|tourists?|visitors?|weddings?|brides?|processions?|performers?|athletes?|runners?' +
    '|cyclists?|worshipp?ers?|spectators?|audiences?|musicians?|farmers?|riders?|couples?|locals' +
    '|lad(y|ies)|portraits?|selfies?|residents?|travell?ers?|crew|pedestrians?|sailors?|divers?' +
    '|kayakers?|soldiers?|guards?|vendors?|shoppers?|players?|fans)\\b', 'i');
  var LEAD = /^((aerial|panoramic|close-up|night|winter|autumn|summer|spring)\s+)?(views?|replicas?|life-size replica)\s+of\s+(the\s+)?|^(the|a|an)\s+/i;
  var SMALL = ['a', 'an', 'the', 'of', 'in', 'on', 'at', 'to', 'for', 'and', 'or', 'by', 'with', 'de', 'del', 'la', 'le', 'du'];
  var US_STATES = ['Alabama', 'Alaska', 'Arizona', 'Arkansas', 'California', 'Colorado', 'Connecticut', 'Delaware',
    'Florida', 'Georgia', 'Hawaii', 'Idaho', 'Illinois', 'Indiana', 'Iowa', 'Kansas', 'Kentucky', 'Louisiana', 'Maine',
    'Maryland', 'Massachusetts', 'Michigan', 'Minnesota', 'Mississippi', 'Missouri', 'Montana', 'Nebraska', 'Nevada',
    'New Hampshire', 'New Jersey', 'New Mexico', 'New York', 'North Carolina', 'North Dakota', 'Ohio', 'Oklahoma',
    'Oregon', 'Pennsylvania', 'Rhode Island', 'South Carolina', 'South Dakota', 'Tennessee', 'Texas', 'Utah',
    'Vermont', 'Virginia', 'Washington', 'West Virginia', 'Wisconsin', 'Wyoming'];
  var KEY = 'glasstv.places', WEEK = 7 * 24 * 3600 * 1000, RULES = 1;

  /** Capitals for each word but the small ones, and for each part of a hyphenated word. */
  function titleCase(s) {
    return s.split(' ').map(function (w, i) {
      if (i > 0 && SMALL.indexOf(w.toLowerCase()) >= 0) return w.toLowerCase();
      return w.split('-').map(function (p) { return p ? p.charAt(0).toUpperCase() + p.slice(1) : p; }).join('-');
    }).join(' ');
  }

  /** "Mount Sneffels in the San Juan Mountains near Ridgway, Colorado" -> ["Mount Sneffels", "United States"]. */
  function nameAndCountry(caption) {
    var c = caption.replace(/\s*\([^)]*\)/g, '').replace(/\s+/g, ' ').trim();
    var comma = c.indexOf(', ');
    var country = comma >= 0 ? c.slice(c.lastIndexOf(', ') + 2).trim() : '';
    if (US_STATES.indexOf(country) >= 0) country = 'United States';
    var name = comma >= 0 ? c.slice(0, comma) : c;
    var cut = /\s+(in|near|at|on|from|over|above|under|along|off|across)\s+/i.exec(name);
    if (cut && cut.index > 0) name = name.slice(0, cut.index);
    name = name.replace(LEAD, '').trim();
    if (!name) name = c;
    return [titleCase(name), country];
  }

  function cached() {
    try { return JSON.parse(localStorage.getItem(KEY)); } catch (e) { return null; }
  }

  /** This week's places, fetched at most once a week; calls back with [] when there are none. */
  function load(callback) {
    var c = cached();
    if (c && c.rules === RULES && Date.now() - c.at < WEEK && c.items.length) return callback(c.items);
    var mkt = encodeURIComponent(navigator.language || 'en-US');
    fetch('https://www.bing.com/hp/api/model?mkt=' + mkt).then(function (r) {
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.json();
    }).then(function (model) {
      var items = [];
      (model.MediaContents || []).forEach(function (m) {
        var about = m && m.ImageContent, image = about && about.Image;
        if (!image || !about.Title || !image.Url) return;
        if (PEOPLE.test(about.Title + ' ' + (about.Headline || ''))) return;
        var named = nameAndCountry(about.Title);
        if (!named[1]) return;
        items.push({ name: named[0], country: named[1], text: (about.Description || '').replace(/\n/g, ' '),
          url: 'https://www.bing.com' + image.Url.replace('.webp', '.jpg') });
      });
      if (items.length) {
        try { localStorage.setItem(KEY, JSON.stringify({ at: Date.now(), rules: RULES, items: items })); } catch (e) {}
      }
      callback(items.length ? items : (c ? c.items : []));
    }).catch(function (e) {
      console.log('[GlassTV] places: ' + e);
      callback(c ? c.items : []);
    });
  }

  return { load: load, nameAndCountry: nameAndCountry };
})();
