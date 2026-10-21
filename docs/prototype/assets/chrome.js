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
    'toggle.skin': '切换外观',

    'skin.title': '界面外观',
    'skin.note': '立即生效，并记住你的选择',
    'skin.codenow': '青松',
    'skin.minimal': '极简',
    'skin.cohere': '暖石',
    'skin.dell': '复古',

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
    'toggle.skin': 'Switch appearance',

    'skin.title': 'Appearance',
    'skin.note': 'Applies instantly and is saved',
    'skin.codenow': 'Pine',
    'skin.minimal': 'Minimal',
    'skin.cohere': 'Warmstone',
    'skin.dell': 'Retro',

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
    mark: '<svg class="cn-logo__mark" viewBox="0 0 318 281" aria-hidden="true"><g transform="translate(1 -23)"><path fill="var(--logo-ink)" d="M79 106.75Q79 101 82.22 95.94L82.5 95.5Q86 90 93.92 85.72L160.4 49.76Q169.2 45 177.85 45L177.85 45Q186.5 45 195.35 49.65L259.42 83.28Q266.5 87 271.65 93.1L272.28 93.85Q276.8 99.2 275.15 103L275.15 103Q273.5 106.8 269.17 109.29L263.57 112.51Q257.5 116 250.5 116L238.2 116Q236.2 116 234.39 115.15L179.63 89.36Q174.2 86.8 168.95 89.71L124.12 114.55Q121.5 116 118.5 116L87.2 116Q82.2 116 80.6 114.25L80.6 114.25Q79 112.5 79 108.5Z"/><path fill="var(--logo-ink)" d="M80.6 216.1Q79 218.2 79 222.85L79 222.85Q79 227.5 83.1 233.15L83.1 233.15Q87.2 238.8 95.02 243.25L152.25 275.84Q159.2 279.8 167.1 281.05L174.62 282.24Q184.5 283.8 193.32 279.08L262.56 242.04Q270.5 237.8 274.12 229.56L274.98 227.61Q277.8 221.2 274.07 215.27L272.16 212.23Q269.5 208 264.52 207.55L254.17 206.63Q247.2 206 241.08 209.4L184.25 241.03Q182.5 242 180.51 241.84L176.18 241.49Q170.2 241 164.95 238.09L124.12 215.45Q121.5 214 118.5 214L88.2 214Q82.2 214 80.6 216.1Z"/><rect x="20" y="138" width="111.8" height="22.8" rx="11.4" fill="var(--logo-cyan)"/><rect x="46" y="174" width="64.8" height="20.8" rx="10.4" fill="var(--logo-cyan)"/><path fill="var(--logo-accent)" d="M294.52 112.73Q295.8 111.2 294.07 112.2L221.96 153.99Q218.5 156 216.75 152.4L205.12 128.4Q202.5 123 196.5 123L186.2 123Q180.2 123 177.08 128.12L135 197.13Q132.4 201.4 137.26 202.56L140.55 203.34Q142.5 203.8 144.13 202.64L182.87 174.96Q184.5 173.8 185.23 175.66L195.11 201.01Q196.2 203.8 199.2 203.94L211.51 204.52Q217.5 204.8 221.35 200.2Z"/></g></svg>',
    word: '<svg class="cn-logo__wordmark" viewBox="0 0 955 173" aria-hidden="true"><g transform="translate(0 -25)"><path fill="var(--logo-ink)" fill-rule="evenodd" d="M78 184.9C62.3 183.3 46.8 176.2 35 165.3C25.7 156.7 20.5 148.9 16.1 137.1C12.7 127.7 12.6 127.2 12.6 112.5C12.6 97 13 94.5 17.8 82.4C25.9 62 46.2 44.5 67.8 39.3C77.1 37.1 97.3 37.1 106.3 39.3C116.1 41.7 128.5 48 136.2 54.4C143.3 60.3 145.4 63 144.4 65.2C143.4 67.4 122 87 120.7 87C120 87 117.6 85.2 115.5 82.9C108.1 75.3 94.3 70.3 84 71.3C56.3 74.3 40.1 104.2 52.4 129.7C63.9 153.5 94.4 158.8 114.8 140.5C117.6 138 120.2 136 120.8 136C121.9 136 143.1 154.6 144.2 156.5C145.2 158.1 140.9 163.5 134.2 169.2C120.5 181 98.6 187 78 184.9ZM185.4 182.7C160.4 174.4 146 154.7 146 128.8C146 96.7 171.1 71.8 203.5 71.8C230.4 71.8 254.2 90.5 259.6 115.9C265.7 144.7 252.1 170.6 225.5 181.2C214.2 185.6 196.3 186.3 185.4 182.7ZM299.9 182.7C278.4 174.9 266 155.3 266 129C266 95.5 286.3 72 315.5 71.7C324.7 71.6 330.9 73.4 339.5 78.5C342.7 80.4 345.4 82 345.6 82C345.8 82 346 72.3 346 60.5C346 40.3 346.1 39 347.9 38C351.3 36.3 377.6 36.7 379.4 38.6C380.8 40 381 47.9 381 110.9C381 164.5 380.7 181.9 379.8 182.8C378.5 184.1 350.7 184.6 347.6 183.4C346.5 183 346 181.5 346 178.3L346 173.8L342.2 176.8C334.9 182.6 329.5 184.3 317.5 184.7C307.8 185 305.7 184.8 299.9 182.7ZM434.5 184.4C420.2 180.8 410.5 175.4 402.3 166.6C387 150.1 383.4 124.3 393.5 102.5C409.2 68.3 458.5 60.8 484 88.7C493.1 98.6 498 111.7 498.3 126.6C498.6 139 501.9 138 459.4 138C434.6 138 423 138.3 423 139C423 141.4 426.2 146 430 149.2C439.7 157.6 455.7 156.9 466.6 147.7C468.4 146.2 470.2 145 470.7 145C471.9 145 491 163 491 164.2C491 167.7 476.2 178.1 465.8 181.8C458.6 184.3 440.4 185.8 434.5 184.4ZM333.8 150.8C342 146.9 346.6 137.7 345.8 126.5C344.5 106.9 324.6 96.8 309.2 108C298.1 116 296.6 135.4 306.4 145.7C313.9 153.7 323.8 155.5 333.8 150.8ZM211.1 151.9C222.9 147.9 229.4 134 225.4 121.3C217 94.7 180 100.5 180 128.4C180 145.7 195.3 157.2 211.1 151.9ZM464.6 113.4C464.3 112.5 464 111.5 464 111C464 110.5 462.8 108.7 461.4 107C451.1 94.8 428.1 98.2 423.6 112.8L422.9 115L444 115C462.9 115 465.1 114.8 464.6 113.4Z"/><path fill="var(--logo-accent)" fill-rule="evenodd" d="M688.9 183.5C644.7 172.7 630.1 118.4 663.2 87.4C694.5 58.2 747.8 72 759.6 112.5C761.6 119.5 761.9 134.7 760.1 142.5C757.1 155.3 746.4 169.8 734.8 176.7C722.7 184 703 186.9 688.9 183.5ZM508.2 182.8C507.3 181.9 507 164.4 507 110.3C507 43.2 507.1 39 508.8 38.1C510.8 37.1 534.8 36.6 537.4 37.6C538.3 38 544.8 45.7 551.8 54.8C573 82.3 576.1 86.2 588.3 101.4C594.8 109.4 600.5 116 601 116C601.6 116 602 101.5 602 77.6C602 56.4 602.4 38.9 602.8 38.6C604.8 37.4 633.7 36.8 635.8 37.9L638 39.1L638 110.5C638 151.2 637.6 182.1 637.1 182.4C636.6 182.7 629.8 183.3 621.9 183.6C608.7 184.2 607.3 184.1 605.3 182.4C604.2 181.3 595 170.2 585 157.5C575 144.8 561.6 128 555.2 120L543.5 105.5L543.2 143.9C543 173.2 542.7 182.6 541.7 183.2C539.6 184.5 509.6 184.2 508.2 182.8ZM792.7 182.3C791.8 181.3 790 177.3 788.7 173.5C787.3 169.6 783 157.3 779 146C775 134.7 769.6 119 766.9 111C764.3 103 760.5 91.8 758.5 86.1C756.3 79.6 755.3 75.3 755.8 74.8C756.4 74.2 763.9 73.6 772.6 73.3C787.2 72.8 788.7 72.9 790.6 74.6C792.2 76 795.4 84.7 801.6 104.8C806.5 120.3 810.8 133 811.2 133C811.6 133 812.6 130.8 813.4 128.1C818.1 112.3 831.4 75.3 832.8 74.2C834.6 72.9 859 72.6 862.4 73.9C863.9 74.5 866.6 81.3 874 103.6C879.3 119.5 884.1 132.3 884.6 132C885.1 131.8 889.7 119.2 894.7 104C901.4 84.1 904.6 76 906.2 74.7C908.2 73 909.9 72.9 924.9 73.4C934 73.7 941.8 74.4 942.3 74.9C942.9 75.5 940.4 83.2 936 94.7C932 105 927.3 117.5 925.5 122.5C923.7 127.5 921.7 132.6 921.1 134C920.5 135.4 916.4 146.4 912 158.5C907.1 171.8 903.1 181.2 901.8 182.2C900.1 183.7 897.6 184 885.4 184C877.2 184 870.1 183.5 868.7 182.9C866.6 182 865 178 857.4 155.1C852.5 140.4 848 128.3 847.5 128.2C847 128.2 842.3 140.1 837.2 154.8C832 169.5 827.5 181.8 827.1 182.3C826.7 182.7 819.2 183.2 810.4 183.5C795.8 184 794.2 183.9 792.7 182.3ZM711.1 151.9C734.5 144.9 731.1 108 706.7 103.6C697 101.8 684.1 110 681.2 119.6C675.1 140.2 691.3 157.8 711.1 151.9Z"/></g></svg>',
    palette: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"><path d="M12 3a9 9 0 1 0 0 18c1.1 0 1.7-.7 1.7-1.6 0-.8-.5-1.3-.5-2 0-.9.7-1.6 1.7-1.6H17a4 4 0 0 0 4-4A9 9 0 0 0 12 3Z"/><circle cx="7.4" cy="12.2" r="1.1" fill="currentColor" stroke="none"/><circle cx="9.6" cy="7.8" r="1.1" fill="currentColor" stroke="none"/><circle cx="14.6" cy="7.3" r="1.1" fill="currentColor" stroke="none"/></svg>'
  };

  /* ---------------- 外观皮肤目录（浅色 / 深色色板用于切换器预览） ---------------- */
  var SKIN_META = [
    { id: 'minimal', light: ['#f5f5f7', '#1d1d1f', '#0066cc'], dark: ['#000000', '#f5f5f7', '#5ab0ff'] },
    { id: 'codenow', light: ['#fafaf8', '#0b8a66', '#0e9f76'], dark: ['#0b0b0f', '#2bd3a3', '#5ee0bb'] },
    { id: 'cohere',  light: ['#eeece7', '#17171c', '#ff7759'], dark: ['#101312', '#efece4', '#ff8b70'] },
    { id: 'dell',    light: ['#ffffff', '#d81e28', '#0000ee'], dark: ['#000000', '#ff3b30', '#ffd400'] }
  ];

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

  function skinPicker() {
    var dark = I18N.theme === 'dark';
    var opts = SKIN_META.map(function (s) {
      var c = dark ? s.dark : s.light;
      return '<button class="cn-skinopt" data-skin-set="' + s.id + '"' +
        ' aria-pressed="' + (I18N.skin === s.id ? 'true' : 'false') + '"' +
        ' style="--sw-bg:' + c[0] + ';--sw-ink:' + c[1] + ';--sw-acc:' + c[2] + '">' +
        '<span class="cn-skinopt__swatch" aria-hidden="true"></span>' +
        '<span class="cn-skinopt__name" data-i18n="skin.' + s.id + '"></span></button>';
    }).join('');
    return '<div class="cn-skinpicker">' +
      '<button class="cn-iconbtn" data-toggle="skin" aria-haspopup="true" aria-expanded="false" data-i18n-title="toggle.skin">' + ICON.palette + '</button>' +
      '<div class="cn-skinpicker__menu" hidden>' +
        '<div class="cn-skinpicker__head" data-i18n="skin.title"></div>' +
        '<div class="cn-skinpicker__grid">' + opts + '</div>' +
        '<p class="cn-skinpicker__note" data-i18n="skin.note"></p>' +
      '</div></div>';
  }

  function closeSkinPicker() {
    var menu = document.querySelector('.cn-skinpicker__menu');
    if (menu) menu.hidden = true;
    var btn = document.querySelector('[data-toggle="skin"]');
    if (btn) btn.setAttribute('aria-expanded', 'false');
  }

  function paintSkinPicker() {
    var dark = I18N.theme === 'dark';
    SKIN_META.forEach(function (s) {
      var btn = document.querySelector('[data-skin-set="' + s.id + '"]');
      if (!btn) return;
      var c = dark ? s.dark : s.light;
      btn.style.setProperty('--sw-bg', c[0]);
      btn.style.setProperty('--sw-ink', c[1]);
      btn.style.setProperty('--sw-acc', c[2]);
      btn.setAttribute('aria-pressed', I18N.skin === s.id ? 'true' : 'false');
    });
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
    var mobileAccount = '';
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
      mobileAccount = '<div class="cn-mobilenav__account">' +
          '<a href="profile.html" data-i18n="act.profile"></a>' +
          '<a href="login.html" data-i18n="act.logout"></a>' +
        '</div>';
    }

    return '' +
      '<header class="cn-header">' +
        '<div class="u-container cn-header__inner">' +
          '<a class="cn-logo" href="index.html" aria-label="CodeNow">' + ICON.mark + ICON.word + '</a>' +
          '<nav class="cn-nav">' + nav + '</nav>' +
          '<div class="u-grow"></div>' +
          '<div class="u-row u-gap-2">' +
            '<button class="cn-iconbtn" data-toggle="theme" data-i18n-title="toggle.theme"></button>' +
            '<button class="cn-iconbtn" data-toggle="lang" data-i18n-title="toggle.lang">' +
              '<span class="cn-iconbtn__text" data-lang-text>中</span></button>' +
            skinPicker() +
            account +
            '<button class="cn-iconbtn cn-burger" data-toggle="menu" aria-label="Menu">' + ICON.menu + '</button>' +
          '</div>' +
        '</div>' +
        '<div class="cn-mobilenav" hidden><div class="u-container">' + nav + mobileAccount + '</div></div>' +
      '</header>';
  }

  function footer() {
    return '' +
      '<footer class="cn-footer">' +
        '<div class="u-container">' +
          '<div class="cn-footer__inner">' +
            '<div class="cn-footer__col" style="max-width:280px">' +
              '<a class="cn-logo" href="index.html">' + ICON.mark + ICON.word + '</a>' +
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
      var opt = e.target.closest('[data-skin-set]');
      if (opt) {
        I18N.setSkin(opt.getAttribute('data-skin-set'));
        closeSkinPicker();
        return;
      }
      var btn = e.target.closest('[data-toggle]');
      if (!btn) return;
      var kind = btn.getAttribute('data-toggle');
      if (kind === 'theme') I18N.toggleTheme();
      if (kind === 'lang') I18N.toggleLang();
      if (kind === 'skin') {
        var menu = document.querySelector('.cn-skinpicker__menu');
        if (!menu) return;
        var open = menu.hidden;
        closeSkinPicker();
        menu.hidden = !open;
        btn.setAttribute('aria-expanded', open ? 'true' : 'false');
      }
      if (kind === 'menu') {
        var panel = document.querySelector('.cn-mobilenav');
        if (panel) panel.hidden = !panel.hidden;
      }
    });
    document.addEventListener('click', function (e) {
      if (!e.target.closest('.cn-skinpicker')) closeSkinPicker();
    });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') closeSkinPicker();
    });
    document.addEventListener('cn:theme', function () { paintToggles(); paintSkinPicker(); });
    document.addEventListener('cn:lang', paintToggles);
    document.addEventListener('cn:skin', paintSkinPicker);
  }

  function render() {
    var h = document.querySelector('[data-chrome="header"]');
    if (h) h.innerHTML = header();
    var f = document.querySelector('[data-chrome="footer"]');
    if (f) f.innerHTML = footer();
    document.querySelectorAll('[data-logo]').forEach(function (el) {
      el.innerHTML = ICON.mark + ICON.word;
    });
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
