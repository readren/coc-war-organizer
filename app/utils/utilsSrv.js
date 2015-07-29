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
		
		find: function(where, what) {
			var i;
			for(i=0; i < where.length && !what(where[i], i); ++i);
			return i < where.length ? where[i] : null;
		},
		
		/** Creates a new array whose elements are the result of applying the received partialFunc to the elements of the source array.
		 * The source elements for which the partialFunc is undefined (=== undefined) are skipped.*/
		collect: function(source, partialFunc) {
			var result = [];
			for(var i = 0; i< source.length; ++i) {
				var pfr = partialFunc(source[i], i);
				if(pfr !== undefined) {
					result.push(pfr);
				}
			}
			return result;
		},
		
		fill: function(size, func) {
			var result = [];
			for( var i=0; i<size; ++i) {
				result.push(func(i));
			}
			return result;
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