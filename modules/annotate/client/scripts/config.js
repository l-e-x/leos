//Should be used only in build

var gulpUtil = require('gulp-util');

var configs ={
    default: { //local
      src: {
        scripts: 'build/scripts',
        styles: 'build/styles',
        fonts: 'build/fonts',
        images: 'build/images',
        templates:'src/sidebar/templates'
      },
      dest: {
        base:'build',
        scripts: 'build/scripts',
        styles: 'build/styles',
        fonts: 'build/fonts',
        images: 'build/images'
      },
      sassOpts:{
        outputStyle: 'nested'
      },
      minify:false
    },

    // Below values overrides the defaults based on env  
    dev: {
    },
    test: {
    },
    acc: {
    },
    prod: {
      sassOpts:{
        outputStyle: 'compressed'
      },
      minify:true
    }
  };

module.exports = {
  getConfig: function (environment) {
    var env = environment || gulpUtil.env.env || process.env.hasOwnProperty("env") ;
    return Object.assign({}, {}, configs.default, configs[env]);
  }
};