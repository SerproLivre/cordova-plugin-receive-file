var exec = require('cordova/exec');

var PLUGIN_NAME = 'ReceiveFilePlugin';

var ReceiveFilePlugin = {
    onComplete: function(callback) {
       return new Promise(function (resolve, reject) {
         exec(callback || resolve, reject, PLUGIN_NAME, 'onComplete', []);
       });
    },

    newIntent: function(callback) {
       return new Promise(function (resolve, reject) {
         exec(callback || resolve, reject, PLUGIN_NAME, 'newIntent', []);
       });
    }
};

module.exports = ReceiveFilePlugin;