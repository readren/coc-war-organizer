'use strict';
/*global app: false */

/**
 * @ngdoc service
 * @name uiApp.teamEditorSrv
 * @description
 * # teamEditorSrv
 * Factory in the uiApp.
 */
app.factory('teamEditorSrv', function () {
    // Service logic
    // ...

    var meaningOfLife = 42;

    // Public API here
    return {
      someMethod: function () {
        return meaningOfLife;
      }
    };
  });
