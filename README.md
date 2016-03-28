# BRU-9

An alternative renderer for the web.

## Overview

This project is currently in very early stages of conceptualization and
development – its shape is still being determined. Follow
[@brtlsm](https://twitter.com/brtlsm) and/or the development updates on
[BRUTALISM's website](http://brutalism.rs).

## Setup

To get an interactive development environment run:

    lein figwheel

To run in LightTable, evaluate any line in any `.cljs` file (Cmd+Enter) and
LightTable will automatically open a new browser tab and
connect to the REPL.

To manually connect to the REPL, open your browser at
[localhost:3449](http://localhost:3449/). This will auto compile and send all
changes to the browser without the need to reload.

To clean all compiled files:

    lein clean

To create a production build run:

    lein cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL.

## License

Copyright © 2016 BRUTALISM

Distributed under the [Creative Commons Attribution-NonCommercial-ShareAlike 4.0
International license](http://creativecommons.org/licenses/by-nc-sa/4.0/).
See LICENSE for details.
