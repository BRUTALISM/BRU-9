# BRU-9

A visualization, a distraction.

## Overview

This project is currently in very early stages of development – its shape is
still being determined. Follow [@brtlsm](https://twitter.com/brtlsm) and/or the
development updates on [BRUTALISM's website](http://brutalism.rs).

## Setup

On Mac/Linux:

    scripts/setup.sh

On Windows:

    script\setup.bat

This will install the node dependencies for the project, along with grunt and bower and will also run `grunt setup`.

To get an interactive development environment run:

    lein figwheel

In another terminal window, launch the electron app:

    grunt launch

You can edit any of the ClojureScript source files and the changes should show
up in the electron app without the need to re-launch.

## Dependencies

Node dependencies are in `package.json` file. Bower dependencies are in
`bower.json` file. Clojure/ClojureScript dependencies are in `project.clj`.

## Creating a build for release

To create a Windows build from a non-Windows platform, please install `wine`. On
OS X, an easy option is using homebrew.

On Windows before doing a production build, please edit the
`scripts/build-windows-exe.nsi` file. The file is the script for creating the
NSIS based setup file.

On Mac OSX, please edit the variables for the plist in `release-mac` task in
`Gruntfile.js`.

Using [`electron-packager`](https://github.com/maxogden/electron-packager), we
are able to create a directory which has OS executables (.app, .exe etc) running
from any platform.

If NSIS is available on the path, a further setup executable will be created for
Windows. Further, if the release command is run from a OS X machine, a DMG file
will be created.

To create the release on OS X:

    grunt cljsbuild-once
    grunt prepare-release
    grunt release-mac --force

This will create the directories in the `builds` folder.

Note: you will need to be on OSX to create a DMG file and on Windows to create
the setup .exe file.

## License

Copyright © 2016 BRUTALISM

Distributed under the [Creative Commons Attribution-NonCommercial-ShareAlike 4.0
International license](http://creativecommons.org/licenses/by-nc-sa/4.0/).
See LICENSE for details.
