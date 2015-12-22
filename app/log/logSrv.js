'use strict';
/*global app: false */

app.factory('logSrv', [ '$http', '$auth', '$timeout', '$rootScope', 'accountSrv', 'utilsSrv', function($http, $auth, $timeout, $rootScope, accountSrv, utilsSrv) {
	var eventsCache = {};
	var membersCache = {};
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
	
	var getLastEventInstant = function() {
		updateCache();
		var events = eventsCache[accountSrv.getCurrentAccount().tag] || [];
		return events.length === 0 ? null : events[0].instant;
	};
	
	var handleTeamChanges = function(members, event) {
		if(event.type === 'joinResponse' && event.requesterIconDto) {
			members.push(event.requesterIconDto);
		} else if(event.type === 'abandon') {
			members.splice(utilsSrv.find(members, function(m) {
				return m.tag === event.iconDto.tag;  
			}), 1);
		} else if(event.type === 'roleChange') {
			var affectedMember = utilsSrv.find(members, function(m){
				return m.tag === event.affectedIconTag;
			});
			if(affectedMember)
				affectedMember.role = event.newRole;
		}
		return members;
	};
	
	var laterInstantFirst = function(a, b) {
		return b.instant - a.instant;
	};
	
	var getEventsAndMembers = function() {
		var srv = this;
		return accountSrv.getCurrentAccountPromise().then(
			function(currentAccount) {
				if(currentAccount) {
					var lastEventInstant = getLastEventInstant();
					if(lastEventInstant) {
						return $http.post('/log/getEventsAfter', {eventInstant: lastEventInstant, actor: currentAccount.tag}).then(
							function(response) {
								// Update the events cache
								// get the cached events and members corresponding to the current account
								var events = eventsCache[currentAccount.tag] || [];
								var members = membersCache[currentAccount.tag] || [];
								// the new events are the ones received in the response that are not already in the cache. So, the repeated ones are filtered out, and the remaining are sorted in order to show them in the correct order. 
								var newEvents = response.data.filter(function(ne) {
									return utilsSrv.find(events, function(oe) {
										return ne.id === oe.id; 
									}) === null;
								}).sort(laterInstantFirst); // NOTE that the sorting is scoped to the events that arrive in the same response. This avoids an event from an older response be before an event from a newer response, even if event from the older response is newer. 
								
								// for each new event, from oldest to newest
								for( var i = newEvents.length; --i >= 0;) {
									// Update the state of the independent machines conceived by events received in previous calls to this operation (getEvents). Note that the state of the machines corresponding to new events are not updated here because their controllers aren't instantiated at this moment. Each of them should be updated by itself, calling the updateMe function, during the controller instantiation.
									srv.treat(newEvents[i]);
									
									// Update the members cache
									handleTeamChanges(members, newEvents[i]);
								}
								
								// add the new events to the cache
								events = newEvents.concat(events);
								eventsCache[currentAccount.tag] = events;
								// give all the events and members
								return { currentAccount: currentAccount, events: events, members: members };
							},
							function(reason) {
								throw reason.data;
							}
						);
					} else {
						return $http.post('/log/getLogInitState', {actor: currentAccount.tag}).then(
							function(response) {
								var events = eventsCache[currentAccount.tag] = response.data.events.sort(laterInstantFirst);
								var members = membersCache[currentAccount.tag] = response.data.members;
								return { currentAccount: currentAccount, events: events, members: members };
							},
							function(reason) {
								throw reason.data;
							}
						);
					}
				} else {
					return null;
				}
			}
		);
	};

	
	return {
		/**Gives an Array[IconDto] with the current members of the organisation */
		getMembers: function() {
			return getEventsAndMembers.call(this).then(function(eam) { return eam.members.slice(0); });
		},

		/** Gives an Array[OrgaEvent] with the more recent organisation events. */
		getEvents: function() {
			return getEventsAndMembers.call(this).then(function(eam) { return eam.events.slice(0); });
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
