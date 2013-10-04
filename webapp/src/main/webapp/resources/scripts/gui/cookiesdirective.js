(function() {
  'use strict';

  $(document).ready(function() {
    $.cookiesDirective({
      duration: 30,
      privacyPolicyUri: CONTEXTPATH + '/about.jsf#about',
      cookieScripts: 'Piwik',
      scriptWrapper: function() {
        if (PIWIK_BASEURL) {
          $.cookiesDirective.loadScript({
            uri: '//' + PIWIK_BASEURL + '/piwik.js'
          });
        };
      }  
    });
  });
  
}).call(this);