'use strict';
/*global app: false */

app.factory('logSrv', [ '$http', '$auth', '$timeout', '$rootScope', 'accountSrv', function($http, $auth, $timeout, $rootScope, accountSrv) {
	var eventsCache = {};
	var lastQueryToken = 0;

	/**updates the cache if a different token is detected
	 * @return true if a different token was detected*/
	var updateCache = function() {
		var currentToken = $auth.getToken();
		if( currentToken !== lastQueryToken) {
			lastQueryToken = currentToken;
			eventsCache = {};
		}
	};
	
	return {
		getLastEventInstant: function() {
			updateCache();
			var events = eventsCache[accountSrv.getCurrentAccount().tag] || [];
			return events.length === 0 ? null : events[0].instant;
		},
		
		getMembers: function() {
			
		},

		getEvents: function() {
			var srv = this;
			return accountSrv.getCurrentAccountPromise().then(
				function(currentAccount) {
					if(currentAccount) {
						return $http.post('/log/getEventsAfter', {eventInstant: srv.getLastEventInstant(), actor: currentAccount.tag}).then(
							function(valueE) {
								// get the cached events corresponding to the current account
								var events = eventsCache[currentAccount.tag] || [];
								// the new events are the ones received in the response that are not already in the cache. So, the repeated ones are filtered out, and the remaining are sorted in order to show them in the correct order. 
								var newEvents = valueE.data.filter(function(ne) {
									return events.indexOf(ne) === -1;
								}).sort(function(a, b) {
									return b.id - a.id;
								});
								// Update the state of the independent machines conceived by events received in previous calls to this operation (getEvents). Note that the state of the machines corresponding to new events are not updated here because their controllers aren't instantiated at this moment. Each of them should be updated by itself, calling the updateMe function, during the controller instantiation.  
								angular.forEach(newEvents, function(newEvent) {
									srv.treat(newEvent);
								});
								// add the new events to the cache
								events = newEvents.concat(events);
								eventsCache[currentAccount.tag] = events;
								// give all the events, new and cached
								return events;
							},
							function(reason) {
								throw reason.data;
							}
						);
					} else {
						return [];
					}
				}
			);
		},
		
		/* Updates the state of the independent machines affected by the inciting event. Given the server has no direct reference to the independent machines, it refers to them trough the ids of the events who conceived them (the affectedEvents). */  
		treat: function(incitingEvent) {
			if( incitingEvent.affectedEvents) {
				var events = eventsCache[accountSrv.getCurrentAccount().tag] || [];
				angular.forEach(incitingEvent.affectedEvents, function(affectedEventId) {
					angular.forEach(events, function(event) {
						if(event.id === affectedEventId && event.updateHandler) {
							event.updateHandler(incitingEvent);
						}
					});
				});
			}
		},
		
		/**For every log event E, the controller that shows it should call this function during it's initialization, in order to let newer events that arrived in the same getEvents() response be able to update the state of this event. */
		updateMe: function(eventId, updateHandler) {
			var events = eventsCache[accountSrv.getCurrentAccount().tag] || [];
			angular.forEach(events, function(incitingEvent) {
				if (incitingEvent.affectedEvents) {
					angular.forEach(incitingEvent.affectedEvents, function(affectedEventId) {
						if(eventId === affectedEventId) {
							updateHandler(incitingEvent);
						}
					});
				}
			});
		} 
	};
	
} ]);
