
# bru-9

## Requirements

* JDK 1.7+
* Leiningen 2.5.3
* node.js 5.1.1 [This is done to match the verion of node.js being used in Electron v0.37.2]
* [NSIS](http://nsis.sourceforge.net/)

On Mac/Linux, installing node.js using [Node Version Manager](https://github.com/creationix/nvm) is recommended.

This project uses Electron v0.35.2. Please check [Electron's GitHub page](https://github.com/atom/electron) for the latest version. The version is specified in `Gruntfile.js` under the `Grunt Config` section.

## Setup

On Mac/Linux:

```
scripts/setup.sh
```

On Windows:

```
scripts\setup.bat
```

This will install the node dependencies for the project, along with grunt and bower and will also run `grunt setup`.


## Development mode

Start the figwheel server:

```
lein figwheel
```

If you are on OSX/Linux and have `rlwrap` installed, you can start the figwheel server with:

```
rlwrap lein figwheel
```

This will give better readline support.

More about [figwheel](https://github.com/bhauman/lein-figwheel) here.


In another terminal window, launch the electron app:

```
grunt launch
```

You can edit the `src/cljs/bru_9/core.cljs` file and the changes should show up in the electron app without the need to re-launch.

## Using nREPL with figwheel

- Start the repl using `lein repl`.

```
user> (use 'figwheel-sidecar.repl-api)
nil
user> (def figwheel-config
        {:figwheel-options {:css-dirs ["app/css"]}
         :build-ids ["dev"]
         :all-builds
           [{:id "dev"
             :figwheel {:on-jsload "bru-9.core/mount-root"}
             :source-paths ["src/cljs" "env/dev/cljs"]
             :compiler {:main "bru-9.dev"
                        :asset-path "js/p/out"
                        :output-to "app/js/p/app.js"
                        :output-dir "app/js/p/out" }}]})
#'user/figwheel-config
user> (start-figwheel! figwheel-config)
Figwheel: Starting server at http://localhost:3449
Figwheel: Watching build - dev
Compiling "resources/public/js/repler.js" from ["src/cljs" "env/dev/cljs"]...
Successfully compiled "app/js/p/app.js" in 2.06 seconds.
Figwheel: Starting CSS Watcher for paths  ["app/css"]
#<SystemMap>
```

See [Figwheel wiki](https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl) for more details.

## Dependencies

Node dependencies are in `package.json` file. Bower dependencies are in `bower.json` file. Clojure/ClojureScript dependencies are in `project.clj`.

## Icons

Please replace the icons provided with your application's icons. The development icons are from [node-appdmg](https://github.com/LinusU/node-appdmg) project.

Files to replace:

* app/img/logo.icns
* app/img/logo.ico
* app/img/logo_96x96.png
* scripts/dmg/TestBkg.png
* scripts/dmg/TestBkg@2x.png

## Creating a build for release

To create a Windows build from a non-Windows platform, please install `wine`. On OS X, an easy option is using homebrew.

On Windows before doing a production build, please edit the `scripts/build-windows-exe.nsi` file. The file is the script for creating the NSIS based setup file.

On Mac OSX, please edit the variables for the plist in `release-mac` task in `Gruntfile.js`.

Using [`electron-packager`](https://github.com/maxogden/electron-packager), we are able to create a directory which has OS executables (.app, .exe etc) running from any platform.

If NSIS is available on the path, a further setup executable will be created for Windows. Further, if the release command is run from a OS X machine, a DMG file will be created.

To create the release directories:

```
grunt release
```

This will create the directories in the `builds` folder.

Note: you will need to be on OSX to create a DMG file and on Windows to create the setup .exe file.


## Grunt commands

To run a command, type `grunt <command>` in the terminal.


| Command       | Description                                                                               |
|---------------|-------------------------------------------------------------------------------------------|
| setup         | Download electron project, installs bower dependencies and setups up the app config file. |
| launch        | Launches the electron app                                                                 |
| release       | Creates a Win/OSX/Linux executables                                                       |
| outdated      | List all outdated clj/cljs/node/bower dependencies                                        |

## Leiningen commands

To run a command, type `lein <command>` in the terminal.

| Command       | Description                                                                               |
|---------------|-------------------------------------------------------------------------------------------|
| cljfmt fix    | Auto-formats all clj/cljs code. See [cljfmt](https://github.com/weavejester/cljfmt)       |
| kibit         | Statically analyse clj/cljs and give suggestions                                          |
