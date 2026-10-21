/* ============================================================
   CodeNow · i18n + Theme runtime
   用法（页面底部脚本内）：
     I18N.define({ homeTitle: '把一句话…' }, { homeTitle: 'Turn one sentence…' })
   标记：data-i18n（文本）· data-i18n-ph（placeholder）· data-i18n-title（title）
   ============================================================ */
(function () {
  'use strict';

  var dict = { zh: {}, en: {} };
  var listeners = [];
  var KEY_THEME = 'cn-theme';
  var KEY_LANG = 'cn-lang';
  var KEY_SKIN = 'cn-skin';
  var SKINS = ['minimal', 'codenow', 'cohere', 'dell'];

  function read(k, fb) { try { return localStorage.getItem(k) || fb; } catch (e) { return fb; } }
  function write(k, v) { try { localStorage.setItem(k, v); } catch (e) {} }

  var I18N = {
    lang: 'zh',
    theme: 'light',
    skin: 'minimal',
    SKINS: SKINS,

    /* ---------- 字典 ---------- */
    define: function (zh, en) {
      Object.assign(dict.zh, zh || {});
      Object.assign(dict.en, en || {});
      return this;
    },
    t: function (key, fallback) {
      var v = dict[this.lang][key];
      if (v === undefined) v = dict[this.lang === 'zh' ? 'en' : 'zh'][key];
      return v === undefined ? (fallback !== undefined ? fallback : key) : v;
    },

    /* ---------- 主题 ---------- */
    applyTheme: function (theme) {
      this.theme = theme === 'dark' ? 'dark' : 'light';
      document.documentElement.setAttribute('data-theme', this.theme);
      document.dispatchEvent(new CustomEvent('cn:theme', { detail: { theme: this.theme } }));
    },
    setTheme: function (theme) {
      write(KEY_THEME, theme);
      this.applyTheme(theme);
    },
    toggleTheme: function () {
      this.setTheme(this.theme === 'dark' ? 'light' : 'dark');
    },

    /* ---------- 外观皮肤（与明暗主题正交） ---------- */
    applySkin: function (skin) {
      this.skin = SKINS.indexOf(skin) > -1 ? skin : 'minimal';
      document.documentElement.setAttribute('data-skin', this.skin);
      document.dispatchEvent(new CustomEvent('cn:skin', { detail: { skin: this.skin } }));
    },
    setSkin: function (skin) {
      write(KEY_SKIN, skin);
      this.applySkin(skin);
    },

    /* ---------- 语言 ---------- */
    applyLang: function (lang) {
      this.lang = lang === 'en' ? 'en' : 'zh';
      document.documentElement.setAttribute('lang', this.lang === 'en' ? 'en' : 'zh-CN');
      this.apply();
      document.dispatchEvent(new CustomEvent('cn:lang', { detail: { lang: this.lang } }));
      listeners.forEach(function (cb) { try { cb(I18N.lang); } catch (e) {} });
    },
    setLang: function (lang) {
      write(KEY_LANG, lang);
      this.applyLang(lang);
    },
    toggleLang: function () {
      this.setLang(this.lang === 'zh' ? 'en' : 'zh');
    },

    /* ---------- 应用翻译到 DOM ---------- */
    apply: function (root) {
      var scope = root || document;
      scope.querySelectorAll('[data-i18n]').forEach(function (el) {
        el.textContent = I18N.t(el.getAttribute('data-i18n'), el.textContent);
      });
      scope.querySelectorAll('[data-i18n-ph]').forEach(function (el) {
        el.setAttribute('placeholder', I18N.t(el.getAttribute('data-i18n-ph')));
      });
      scope.querySelectorAll('[data-i18n-title]').forEach(function (el) {
        var txt = I18N.t(el.getAttribute('data-i18n-title'));
        el.setAttribute('title', txt);
        el.setAttribute('aria-label', txt);
      });
    },

    onChange: function (cb) { listeners.push(cb); },

    /* ---------- 启动 ---------- */
    boot: function () {
      var storedTheme = read(KEY_THEME, null);
      var sysDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
      this.applyTheme(storedTheme || (sysDark ? 'dark' : 'light'));
      this.applySkin(read(KEY_SKIN, 'minimal'));
      this.lang = read(KEY_LANG, 'zh') === 'en' ? 'en' : 'zh';
      document.documentElement.setAttribute('lang', this.lang === 'en' ? 'en' : 'zh-CN');

      if (window.matchMedia) {
        try {
          window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function (e) {
            if (!read(KEY_THEME, null)) I18N.applyTheme(e.matches ? 'dark' : 'light');
          });
        } catch (err) {}
      }
    }
  };

  window.I18N = I18N;
  I18N.boot();
})();
