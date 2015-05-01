Play Silhouette Angular Seed Project
=====================================

The Play Silhouette Angular Seed project shows how [Silhouette](https://github.com/mohiva/play-silhouette) can be used
to create a SPA with [AngularJS](https://angularjs.org/)/[Satellizer](https://github.com/sahat/satellizer) and Play
scaffolded by [yeoman](https://github.com/tuplejump/play-yeoman). It's a starting point which can be extended to fit
your needs.

## Example

[![Deploy to Heroku](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy)

(The "Build App" phase will take a few minutes)

Or you can find a running example of this template under the following URL: https://play-silhouette-angular-seed.herokuapp.com/

## Features

* Sign Up
* Sign In (Credentials)
* JWT authentication
* Social Auth (Facebook, Google+, Twitter)
* Dependency Injection with Guice
* Publishing Events
* Avatar service

## Documentation

Consulate the [Silhouette documentation](http://silhouette.mohiva.com/docs) for more information. If you need help with the integration of Silhouette into your project, don't hesitate and ask questions in our [mailing list](https://groups.google.com/forum/#!forum/play-silhouette) or on [Stack Overflow](http://stackoverflow.com/questions/tagged/playframework).

## Getting started

1. Make sure u have [Ruby](https://www.ruby-lang.org/de/) and [node.js](http://nodejs.org/) installed.

  Then you must install the node packages [yo](http://yeoman.io), [grunt](http://gruntjs.com/) and [bower](http://bower.io/):

  ```
  npm install -g yo grunt grunt-cli bower
  ```

  And the ruby packages [sass](http://sass-lang.com/) and [compass](http://compass-style.org/):

  ```
  gem install sass compass
  ```

  Alternative you can use Bundler to install the ruby packages:

  ```
  bundle install -j4 --path .bundle
  ```
2. Configure social providers 
  
  To configure the social providers for [Satellizer](https://github.com/sahat/satellizer), Open the file "~\ui\app\scripts\app.js" and input your providers `clientId`: 
  ```
  // Facebook
  $authProvider.facebook({
    clientId: 'your-client-id',
    ...
  });
  
  // Google
  $authProvider.google({
    clientId: 'your-client-id',
    ...
  });
  ...
  ```
  If you are using Heroku Update the "~\app.json" file with your client secret and client ID.
  ```
  "env": {
    "BUILDPACK_URL": "https://github.com/heroku/heroku-buildpack-multi",
    "NPM_CONFIG_PRODUCTION": "false",
    "PLAY_CONF_FILE": "application.conf",
    "PLAY_APP_SECRET": "changeme",
    "FACEBOOK_CLIENT_ID": "",
    "FACEBOOK_CLIENT_SECRET": "",
    "GOOGLE_CLIENT_ID": "",
    "GOOGLE_CLIENT_SECRET": "",
    "TWITTER_CONSUMER_KEY": "",
    "TWITTER_CONSUMER_SECRET": ""
  }
  ```
  
  To test social providers on localhost, you can either set your system environment variables as defined in the app.json "env" section or manually update the "~\conf\silhouette.conf" file directly with your client ID and client secret.
  ```
  # Google provider
  google.accessTokenURL="https://accounts.google.com/o/oauth2/token"
  google.redirectURL="http://localhost:9000"
  google.clientID="your-client-id"
  google.clientSecret="your-client-secret"
  google.scope="profile email"
  ```

3. Start sbt and run the following:

  ```
  $ update

  $ npm install

  $ bower install

  $ grunt build

  $ run
  ```

## Eclipse configuration
1. In the activator console enter ```eclipse with-sources=true```
2. Open eclipse and import the project using the "General/Existing project into workspace" importer.
3. Install the angularJs plugin
4. Apply the angularJs nature to the project: right-click the project, left-click "configure", left-click "Convert to angular project". This step should add the "org.eclipse.wst.jsdt.core.jsNature", but sometimes that requires a restart.
5. Open the project properties dialog, select the "javaScript/Include Path" tab, and add all the relevant java script folders/files.
	Note: By default, eclipse adds the project root as a script folder. Remove it to speed up the script validations.
6. In "conf/application.conf" file, edit the "yeoman.devDirs" entry to include the play "app" package folder, like so: yeoman.devDirs=["ui/.tmp", "ui/app", "app"]

## Activator

See https://typesafe.com/activator/template/play-silhouette-angular-seed

# License

The code is licensed under [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0).
