'use strict';

/**
 * @ngdoc service
 * @name uiApp.centralSrv
 * @description
 * # centralSrv
 * Factory in the uiApp.
 */
angular.module('uiApp')
  .factory('centralSrv', function ($http) {
    // Service logic
    // ...

    var currentStatePromise = $http.post('/')

    // Public API here
    return {
      someMethod: function () {
        return meaningOfLife;
      }
    };
  });
