'use strict';

describe('Service: commonSrv', function () {

  // load the service's module
  beforeEach(module('uiApp'));

  // instantiate service
  var commonSrv;
  beforeEach(inject(function (_commonSrv_) {
    commonSrv = _commonSrv_;
  }));

  it('should do something', function () {
    expect(!!commonSrv).toBe(true);
  });

});
