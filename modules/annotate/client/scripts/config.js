/*
 * Copyright 2018 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
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
      minify:false,
      apiHost: 'http://localhost:9099/annotate',
      assetHost:'http://localhost:9099/annotate/client'
    },

    // Below values overrides the defaults based on env  
    dev: {
      apiHost: 'https://intragate.development.ec.europa.eu/annotate',
      assetHost:'https://intragate.development.ec.europa.eu/annotate/client'
    },
    test: {
      apiHost: 'https://intragate.test.ec.europa.eu/annotate',
      assetHost:'https://intragate.test.ec.europa.eu/annotate/client'
    },
    acc: {
      apiHost: 'https://webgate.acceptance.ec.europa.eu/annotate',
      assetHost:'https://webgate.acceptance.ec.europa.eu/annotate/client'
    },
    prod: {
      sassOpts:{
        outputStyle: 'compressed'
      },
      minify:true,
      apiHost: 'https://intragate.ec.europa.eu/annotate',
      assetHost:'https://intragate.ec.europa.eu/annotate/client'
    }
  };

module.exports = {
  getConfig: function (environment) {
    var env = environment || gulpUtil.env.env || process.env.hasOwnProperty("env") ;
    return Object.assign({}, {}, configs.default, configs[env]);
  }
};