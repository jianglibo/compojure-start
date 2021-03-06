/* jshint globalstrict:true, newcap:false  */
/* global require, __dirname, process, console */

/**
 * Copyright 2014, Yahoo! Inc.
 * Copyrights licensed under the New BSD License. See the accompanying LICENSE file for terms.
 */
'use strict';
require('node-jsx').install({ extension: '.jsx' });
var express = require('express');
var favicon = require('serve-favicon');
var expressState = require('express-state');
var navigateAction = require('flux-router-component').navigateAction;
var debug = require('debug')('Example');
var React = require('react');
var app = require('./app');
var HeadComponent = React.createFactory(require('./components/Head.jsx'));
var exphbs  = require('express-handlebars');
var server = express();
expressState.extend(server);
server.set('state namespace', 'App');

server.engine('.hbs', exphbs({defaultLayout: 'main'}));
server.set('view engine', '.hbs');

//server.use(favicon(__dirname + '/../favicon.ico'));
server.use('/public', express.static(__dirname + '/build'));

server.use(function (req, res, next) {
  var context = app.createContext();

  debug('Executing navigate action');
  context.getActionContext().executeAction(navigateAction, {
    path: req.path
  }, function (err) {
    if (err) {
      if (err.status && err.status === 404) {
        next();
      } else {
        next(err);
      }
      return;
    }
    debug('Rendering Application component');
    var head = React.renderToStaticMarkup(HeadComponent());
    var html = React.renderToString(app.getAppComponent()({
      context: context.getComponentContext()
    }));
    debug('Exposing context state');
    res.expose(app.dehydrate(context), 'App');
    debug('Rendering application into layout');

    debug('Sending markup');
    res.render('home', {
      html: html,
      head: head,
      locals: res.locals,
      layout: false});
  });
});

var port = process.env.PORT || 3000;
server.listen(port);
console.log('Listening on port ' + port);

console.log(__dirname);
