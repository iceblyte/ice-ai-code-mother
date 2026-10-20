/* ============================================================
   CodeNow · Shared chrome (header / footer / toggles)
   依赖 i18n.js。页面用 <div data-chrome="header"></div> 与
   <div data-chrome="footer"></div> 作为挂载点。
   页面通过 <body data-chrome="app|admin|auth" data-page="home|explore|..."> 控制形态。
   ============================================================ */
(function () {
  'use strict';

  /* ---------------- 公共文案 ---------------- */
  I18N.define({
    'nav.home': '首页',
    'nav.explore': '案例广场',
    'nav.templates': '模板',
    'nav.myapps': '我的应用',
    'nav.doc': '使用文档',
    'nav.userManage': '用户管理',
    'nav.appManage': '应用管理',
    'nav.chatManage': '对话管理',
    'nav.back': '返回工作台',

    'act.login': '登录',
    'act.register': '注册',
    'act.profile': '个人中心',
    'act.logout': '退出登录',
    'act.generate': '生成',
    'act.deploy': '部署',
    'act.download': '下载代码',
    'act.openNew': '新窗口打开',
    'act.editMode': '编辑模式',
    'act.attach': '+ 附件',
    'act.refresh': '刷新预览',

    'toggle.theme': '切换主题',
    'toggle.lang': '切换语言',

    'foot.tagline': '一句话，编译成可运行的应用。',
    'foot.product': '产品',
    'foot.resources': '资源',
    'foot.company': '公司',
    'foot.pricing': '定价',
    'foot.changelog': '更新日志',
    'foot.about': '关于我们',
    'foot.contact': '联系我们',
    'foot.terms': '服务条款',
    'foot.privacy': '隐私政策',
    'foot.rights': '保留所有权利',
    'foot.icp': '京ICP备00000000号'
  }, {
    'nav.home': 'Home',
    'nav.explore': 'Explore',
    'nav.templates': 'Templates',
    'nav.myapps': 'My apps',
    'nav.doc': 'Docs',
    'nav.userManage': 'Users',
    'nav.appManage': 'Apps',
    'nav.chatManage': 'Chats',
    'nav.back': 'Back to workspace',

    'act.login': 'Sign in',
    'act.register': 'Sign up',
    'act.profile': 'Profile',
    'act.logout': 'Sign out',
    'act.generate': 'Generate',
    'act.deploy': 'Deploy',
    'act.download': 'Download code',
    'act.openNew': 'Open in new tab',
    'act.editMode': 'Edit mode',
    'act.attach': '+ Attach',
    'act.refresh': 'Refresh preview',

    'toggle.theme': 'Toggle theme',
    'toggle.lang': 'Toggle language',

    'foot.tagline': 'One sentence, compiled into a working app.',
    'foot.product': 'Product',
    'foot.resources': 'Resources',
    'foot.company': 'Company',
    'foot.pricing': 'Pricing',
    'foot.changelog': 'Changelog',
    'foot.about': 'About',
    'foot.contact': 'Contact',
    'foot.terms': 'Terms',
    'foot.privacy': 'Privacy',
    'foot.rights': 'All rights reserved.',
    'foot.icp': 'ICP Filing No. 00000000'
  });

  /* ---------------- 图标 ---------------- */
  var ICON = {
    sun: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><circle cx="12" cy="12" r="4.2"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M19.1 4.9l-1.4 1.4M6.3 17.7l-1.4 1.4"/></svg>',
    moon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5Z"/></svg>',
    menu: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M4 7h16M4 12h16M4 17h16"/></svg>',
    mark: '<svg class="cn-logo__mark" viewBox="0 0 120 120" aria-hidden="true">' +
      '<defs>' +
        '<linearGradient id="cnNavyM" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#2B3A8E"/><stop offset="1" stop-color="#151F55"/></linearGradient>' +
        '<linearGradient id="cnTealM" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#47E0D6"/><stop offset="1" stop-color="#12A2C2"/></linearGradient>' +
        '<linearGradient id="cnBoltM" x1="0" y1="0" x2="0.85" y2="1"><stop offset="0" stop-color="#58A9F8"/><stop offset="1" stop-color="#2450D8"/></linearGradient>' +
      '</defs>' +
      '<circle cx="40" cy="62" r="12" fill="none" stroke="url(#cnTealM)" stroke-width="10"/>' +
      '<ellipse cx="40" cy="62" rx="28" ry="9" transform="rotate(-25 40 62)" fill="none" stroke="#FFFFFF" stroke-width="4.5"/>' +
      '<path d="M14 50 V37 L60 10 L106 37 V50" fill="none" stroke="url(#cnNavyM)" stroke-width="13" stroke-linejoin="round"/>' +
      '<path d="M106 68 V83 L60 110 L14 83 V68" fill="none" stroke="url(#cnNavyM)" stroke-width="13" stroke-linejoin="round"/>' +
      '<path d="M64 35 L76 29 L86 60 L86 28 L100 21 L88 54 L96 54 L76 98 L70 58 L70 91 L54 91 Z" fill="url(#cnBoltM)"/>' +
      '<path d="M4 55 H21 M9 64 H21" fill="none" stroke="url(#cnTealM)" stroke-width="4.5" stroke-linecap="round"/>' +
    '</svg>'
  };

  function navItems(mode) {
    if (mode === 'admin') {
      return [
        { key: 'home', href: 'index.html', label: 'nav.home' },
        { key: 'userManage', href: 'admin-users.html', label: 'nav.userManage' },
        { key: 'appManage', href: 'admin-apps.html', label: 'nav.appManage' },
        { key: 'chatManage', href: 'admin-chats.html', label: 'nav.chatManage' }
      ];
    }
    if (mode === 'auth') return [];
    return [
      { key: 'home', href: 'index.html', label: 'nav.home' },
      { key: 'explore', href: 'explore.html', label: 'nav.explore' },
      { key: 'templates', href: 'explore.html#templates', label: 'nav.templates' },
      { key: 'myapps', href: 'profile.html', label: 'nav.myapps' }
    ];
  }

  function header() {
    var body = document.body;
    var mode = body.dataset.chrome || 'app';
    var current = body.dataset.page || '';

    var nav = navItems(mode).map(function (it) {
      var on = current === it.key ? ' aria-current="page"' : '';
      return '<a href="' + it.href + '"' + on + ' data-i18n="' + it.label + '"></a>';
    }).join('');

    var account = '';
    if (mode !== 'auth') {
      account = '<details class="cn-user">' +
          '<summary class="cn-user__summary"><span class="cn-avatar">A</span>' +
          '<span class="cn-user__name u-nowrap">Alex</span></summary>' +
          '<div class="cn-user__menu">' +
            '<a href="profile.html" data-i18n="act.profile"></a>' +
            '<a href="index.html" data-i18n="nav.back"></a>' +
            '<a href="login.html" data-i18n="act.logout"></a>' +
          '</div>' +
        '</details>';
    }

    return '' +
      '<header class="cn-header">' +
        '<div class="u-container cn-header__inner">' +
          '<a class="cn-logo" href="index.html" aria-label="CodeNow">' + ICON.mark +
            '<span class="cn-logo__word">Code<i>Now</i><span class="cn-logo__cursor"></span></span>' +
          '</a>' +
          '<nav class="cn-nav">' + nav + '</nav>' +
          '<div class="u-grow"></div>' +
          '<div class="u-row u-gap-2">' +
            '<button class="cn-iconbtn" data-toggle="theme" data-i18n-title="toggle.theme"></button>' +
            '<button class="cn-iconbtn" data-toggle="lang" data-i18n-title="toggle.lang">' +
              '<span class="cn-iconbtn__text" data-lang-text>中</span></button>' +
            account +
            '<button class="cn-iconbtn cn-burger" data-toggle="menu" aria-label="Menu">' + ICON.menu + '</button>' +
          '</div>' +
        '</div>' +
        '<div class="cn-mobilenav" hidden><div class="u-container">' + nav + '</div></div>' +
      '</header>';
  }

  function footer() {
    return '' +
      '<footer class="cn-footer">' +
        '<div class="u-container">' +
          '<div class="cn-footer__inner">' +
            '<div class="cn-footer__col" style="max-width:280px">' +
              '<a class="cn-logo" href="index.html">' + ICON.mark +
                '<span class="cn-logo__word">Code<i>Now</i></span></a>' +
              '<p class="u-caption u-mt-4" data-i18n="foot.tagline"></p>' +
            '</div>' +
            '<div class="cn-footer__links">' +
              '<div class="cn-footer__col"><h4 data-i18n="foot.product"></h4>' +
                '<a href="explore.html" data-i18n="nav.explore"></a>' +
                '<a href="explore.html#templates" data-i18n="nav.templates"></a>' +
                '<a href="#" data-i18n="foot.pricing"></a></div>' +
              '<div class="cn-footer__col"><h4 data-i18n="foot.resources"></h4>' +
                '<a href="#" data-i18n="nav.doc"></a>' +
                '<a href="#" data-i18n="foot.changelog"></a>' +
                '<a href="https://github.com/iceblyte" target="_blank" rel="noreferrer">GitHub</a></div>' +
              '<div class="cn-footer__col"><h4 data-i18n="foot.company"></h4>' +
                '<a href="#" data-i18n="foot.about"></a>' +
                '<a href="#" data-i18n="foot.contact"></a></div>' +
            '</div>' +
          '</div>' +
          '<div class="cn-footer__bottom">' +
            '<span>© 2026 CodeNow. <span data-i18n="foot.rights"></span></span>' +
            '<span class="u-row u-gap-4"><span data-i18n="foot.icp"></span>' +
              '<a href="#" data-i18n="foot.terms"></a><a href="#" data-i18n="foot.privacy"></a></span>' +
          '</div>' +
        '</div>' +
      '</footer>';
  }

  /* ---------------- 交互绑定 ---------------- */
  function paintToggles() {
    var t = document.querySelector('[data-toggle="theme"]');
    if (t) {
      t.innerHTML = I18N.theme === 'dark' ? ICON.sun : ICON.moon;
      t.setAttribute('aria-pressed', I18N.theme === 'dark');
    }
    var l = document.querySelector('[data-lang-text]');
    if (l) l.textContent = I18N.lang === 'zh' ? '中' : 'EN';
  }

  function bind() {
    var root = document.querySelector('[data-chrome="header"]');
    if (!root) return;
    root.addEventListener('click', function (e) {
      var btn = e.target.closest('[data-toggle]');
      if (!btn) return;
      var kind = btn.getAttribute('data-toggle');
      if (kind === 'theme') I18N.toggleTheme();
      if (kind === 'lang') I18N.toggleLang();
      if (kind === 'menu') {
        var panel = document.querySelector('.cn-mobilenav');
        if (panel) panel.hidden = !panel.hidden;
      }
    });
    document.addEventListener('cn:theme', paintToggles);
    document.addEventListener('cn:lang', paintToggles);
  }

  function render() {
    var h = document.querySelector('[data-chrome="header"]');
    if (h) h.innerHTML = header();
    var f = document.querySelector('[data-chrome="footer"]');
    if (f) f.innerHTML = footer();
    paintToggles();
    I18N.apply();
    bind();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', render);
  } else {
    render();
  }
})();
