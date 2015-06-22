'use strict';

describe('Service: centralSrv', function () {

  // load the service's module
  beforeEach(module('uiApp'));

  // instantiate service
  var centralSrv;
  beforeEach(inject(function (_centralSrv_) {
    centralSrv = _centralSrv_;
  }));

  it('should do something', function () {
    expect(!!centralSrv).toBe(true);
  });

});
