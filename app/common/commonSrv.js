'use strict';

/**
 * @ngdoc service
 * @name uiApp.commonSrv
 * @description # commonSrv Factory in the uiApp.
 */
angular.module('uiApp').factory('commonSrv', function($auth, $q, accountSrv) {
	
	var AccountLocal = function(initializer) {
		this.token = $auth.getToken();
		this.account = accountSrv.getCurrentAccount();
		this.initializer = initializer;
		this.reset();
	};
	AccountLocal.prototype.reset() {
		this.data = angular.isFunction(this.initializer) ? this.initializer(this.account) : this.initializer;
	};
	AccountLocal.prototype.validateState = function() {
		var currentToken = $auth.getToken();
		var currentAccount = accountSrv.getCurrentAccount();
		if( this.token !== currentToken || this.account !== currentAccount) {
			this.token = currentToken;
			this.account = currentAccount;
			this.reset();
		}
	};
	AccountLocal.prototype.getAccount = function() {
		this.validateState();
		return this.account;
	};
	AccountLocal.prototype.get() {
		this.validateState();
		return this.data;
	};
	AccountLocal.prototype.set(data) {
		this.validateState();
		this.data = data;
	};
	
	
	
	var AccountLocalBuffer = function(defaultOmission) {
		AccountLocal.call(this, {});
		this.defaultOmission = defaultOmission;
	};
	AccountLocalBuffer.prototype = Object.create(AccountLocal.prototype, {constructor: {value: AccountLocalBuffer}});
	AccountLocalBuffer.prototype.get = function(key, omission) {
		this.validateState();
		var value = this.data[key];
		if(value === undefined) {
			if( omission === undefined ) {
				value = angular.isFunction(this.defaultOmission) ? this.defaultOmission(key) : this.defaultOmission;
			} else {
				value = omission;
			}
			this.data[key] = value;
		}
		return value;
	};
	AccountLocalBuffer.prototype.getOrElse = function(key, orElseFunc) {
		this.validateState();
		var value = this.data[key];
		if(value === undefined) {
			value = this.data[key] = orElseFunc(key);
		}
		return value;
	};
	AccountLocalBuffer.prototype.set = function(key, value) {
		this.validateState();
		this.data[key] = value;
	};
	AccountLocalBuffer.prototype.getOrElsePromise = function(key, orElsePromise) {
		this.validateState();
		var value = this.data[key];
		if( value === undefined) {
			return orElsePromise(key);
		} else {
			return $q.when(value);
		}
	};
	
	
	return {
		createAccountLocal : function(initializer) {
			return new AccountLocal(initializer);
		},
		createAccountLocalBuffer : function(defaultOmission) {
			return new AccountLocalBuffer(defaultOmission);
		}
	};
});
