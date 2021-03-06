var services = angular.module('mesos-es-ui.services', []);

services.factory('Cluster', function($resource, $location, config) {
    var URL = $location.protocol() + '://' + $location.host() + ':' + config.api.port + '/cluster';
    return $resource(URL);
});

services.factory('Tasks', function($resource, config, $location, config) {
    var URL = $location.protocol() + '://' + $location.host() + ':' + config.api.port + '/tasks';
    return $resource(URL);
});
