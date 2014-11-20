/**
 * Copyright 2014, Yahoo! Inc.
 * Copyrights licensed under the New BSD License. See the accompanying LICENSE file for terms.
 */
'use strict';
var webpack = require('webpack');

module.exports = {
    resolve: {
        extensions: ['', '.js', '.jsx']
    },
//  context:  __dirname + "/resources/public",
    entry: './client.js',
    output: {
        path: __dirname + '/build/js',
        filename: 'client.js'
    },
    module: {
        loaders: [
            { test: /\.css$/, loader: 'style!css' },
            { test: /\.jsx$/, loader: 'jsx-loader' }
        ],
         preLoaders: [
            {
                test: /\.js$/, // include .js files
                exclude: /node_modules/, // exclude any and all files in the node_modules folder
                loader: "jshint-loader"
            }
        ]
    },
    jshint: {
        // any jshint option http://www.jshint.com/docs/options/
        // i. e.
        camelcase: true,

        // jshint errors are displayed by default as warnings
        // set emitErrors to true to display them as errors
        emitErrors: false,

        // jshint to not interrupt the compilation
        // if you want any file with jshint errors to fail
        // set failOnHint to true
        failOnHint: false,

        // custom reporter function
        //reporter: function(errors) { }
    },
    plugins: [
        // new webpack.optimize.UglifyJsPlugin()
    ]
};
