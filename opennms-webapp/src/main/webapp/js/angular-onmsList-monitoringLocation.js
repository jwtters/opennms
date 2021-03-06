(function() {
	'use strict';

	var MODULE_NAME = 'onmsList.monitoringLocation';

	// $filters that can be used to create human-readable versions of filter values
	angular.module('monitoringLocationsListFilters', [ 'onmsListFilters' ])
	.filter('property', function() {
		return function(input) {
			switch (input) {
			case 'locationName':
				return 'Location name';
			case 'monitoringArea':
				return 'Monitoring area';
			case 'geolocation':
				return 'Geolocation';
			case 'latitude':
				return 'Latitude';
			case 'longitude':
				return 'Longitude';
			case 'priority':
				return 'Priority';
			}
			// If no match, return the input
			return input;
		}
	})
	.filter('value', function($filter) {
		return function(input, property) {
			switch (property) {
				// There is no special formatting
			}
			return input;
		}
	});


	// Minion module
	angular.module(MODULE_NAME, [ 'ngResource', 'onmsList', 'monitoringLocationsListFilters' ])


	/**
	 * OnmsMonitoringLocation REST $resource
	 */
	.factory('MonitoringLocations', function($resource, $log, $http, $location) {
		return $resource(BASE_REST_URL + '/monitoringLocations/:id', {},
			{
				'query': { 
					method: 'GET',
					isArray: true,
					// Append a transformation that will unwrap the item array
					transformResponse: appendTransform($http.defaults.transformResponse, function(data, headers, status) {
						// TODO: Figure out how to handle session timeouts that redirect to 
						// the login screen
						/*
						if (status === 302) {
							$window.location.href = $location.absUrl();
							return [];
						}
						*/
						if (status === 204) { // No content
							return [];
						} else {
							// Always return the data as an array
							return angular.isArray(data.location) ? data.location : [ data.location ];
						}
					})
				},
				'update': { 
					method: 'PUT'
				}
			}
		);
	})

	/**
	 * MonitoringLocations list controller
	 */
	.controller('MonitoringLocationsListCtrl', ['$scope', '$location', '$window', '$log', '$filter', 'MonitoringLocations', function($scope, $location, $window, $log, $filter, MonitoringLocations) {
		$log.debug('MonitoringLocationsListCtrl initializing...');

		// Set the default sort and set it on $scope.$parent.query
		$scope.$parent.defaults.orderBy = 'locationName';
		$scope.$parent.query.orderBy = 'locationName';

		$scope.newItem = {};

		// Reload all resources via REST
		$scope.$parent.refresh = function() {
			// Fetch all of the items
			MonitoringLocations.query(
				{
					_s: $scope.query.searchParam, // FIQL search
					limit: $scope.query.limit,
					offset: $scope.query.offset,
					orderBy: $scope.query.orderBy,
					order: $scope.query.order
				}, 
				function(value, headers) {
					$scope.items = value;

					var contentRange = parseContentRange(headers('Content-Range'));
					$scope.query.lastOffset = contentRange.end;
					// Subtract 1 from the value since offsets are zero-based
					$scope.query.maxOffset = contentRange.total - 1;
					$scope.setOffset(contentRange.start);
				},
				function(response) {
					switch(response.status) {
					case 404:
						// If we didn't find any elements, then clear the list
						$scope.items = [];
						$scope.query.lastOffset = 0;
						$scope.query.maxOffset = -1;
						$scope.setOffset(0);
						break;
					case 401:
					case 403:
						// Handle session timeout by reloading page completely
						$window.location.href = $location.absUrl();
						break;
					}
					// TODO: Handle 500 Server Error by executing an undo callback?
				}
			);
		};

		// Save an item by using $resource.$update
		$scope.$parent.update = function(item) {
			// Check to make sure that the item has an ID
			if (item['location-name'] === null || item['location-name'] === '') {
				// TODO: Throw a validation error
				return;
			}

			// We have to provide the locationName here because it has a dash in its
			// name and we can't use dot notation to refer to it as a default param
			var saveMe = MonitoringLocations.get({id: item['location-name']}, function() {
				// Update fields
				saveMe['monitoring-area'] = item['monitoring-area'];
				saveMe.geolocation = item.geolocation;
				saveMe.latitude = item.latitude;
				saveMe.longitude = item.longitude;
				saveMe.priority = item.priority;
				saveMe['polling-package-names'] = item['polling-package-names'];
				saveMe['collection-package-names'] = item['collection-package-names'];

				// We have to provide the locationName here because it has a dash in its
				// name and we can't use dot notation to refer to it as a default param
				saveMe.$update({id: item['location-name']}, function() {
					// If there's a search in effect, refresh the view
					if ($scope.query.searchParam !== '') {
						$scope.refresh();
					}
				});
			}, function(response) {
				if (response.status === 404) {
					// Create a new $resource and assign properties on it
					var saveMe = new MonitoringLocations({});
					saveMe['location-name'] = item['location-name'];
					saveMe['monitoring-area'] = item['monitoring-area'];
					saveMe.geolocation = item.geolocation;
					saveMe.latitude = item.latitude;
					saveMe.longitude = item.longitude;
					saveMe.priority = item.priority;
					saveMe['polling-package-names'] = item['polling-package-names'];
					saveMe['collection-package-names'] = item['collection-package-names'];

					// Insert the object instead of updating it
					saveMe.$save({}, function() {
						$scope.refresh();
						$scope.newItem = {};
						// Return true to indicate successful submission
						return true;
					});
				}
			});

		};

		$scope.$parent.deleteItem = function(item) {
			// We have to provide the locationName here because it has a dash in its
			// name and we can't use dot notation to refer to it as a default param
			var saveMe = MonitoringLocations.get({id: item['location-name']}, function() {
				if ($window.confirm('Are you sure you want to remove location \"' + item['location-name'] + '\"?')) {
					// We have to provide the locationName here because it has a dash in its
					// name and we can't use dot notation to refer to it as a default param
					saveMe.$delete({id: item['location-name']}, function() {
						$scope.refresh();
					});
				}
			}, function(response) {
				if (response.status === 404) {
					// We didn't find the item so it can't be deleted
					// Might as well call refresh()
					$scope.refresh();
				}
			});
		};

		// Refresh the item list;
		$scope.$parent.refresh();

		$log.debug('MonitoringLocationsListCtrl initialized');
	}])

	.run(['$rootScope', '$log', function($rootScope, $log) {
		$log.debug('Finished initializing ' + MODULE_NAME);
	}])

	;

	angular.element(document).ready(function() {
		console.log('Bootstrapping ' + MODULE_NAME);
		angular.bootstrap(document, [MODULE_NAME]);
	});
}());
