'use strict';

describe('Service: teamEditorSrv', function () {

  // load the service's module
  beforeEach(module('uiApp'));

  // instantiate service
  var teamEditorSrv;
  beforeEach(inject(function (_teamEditorSrv_) {
    teamEditorSrv = _teamEditorSrv_;
  }));

  it('should do something', function () {
    expect(!!teamEditorSrv).toBe(true);
  });

});
