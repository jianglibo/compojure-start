var App = Ember.Application.create();
App.ApplicationRoute = Ember.Route.extend({
  setupController: function(controller) {
// `controller` is the instance of ApplicationController
    controller.set('title', "Hello world!");
  }
});
App.ApplicationController = Ember.Controller.extend({
  appName: 'My First Example'
});
