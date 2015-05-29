'use strict';
/*global app: false */

app.factory('utilsSrv', [function(){
	return {
		map: function(f) {
			return function(object) {
				return f(object);
			};
		},
		
		mapMember: function(memberName) {
			return function(object) {
				return object[memberName];
			};
		},
		
		mapHttpData: function(promise) {
			return promise.then(function(value){
				return value.data;
			}, function(reason){
				throw reason.data ? reason.data : reason;
			});
		}
	};
}]);