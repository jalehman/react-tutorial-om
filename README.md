player-ladder-om
=================

A player ladder for any 1v1 game (e.g. table tennis).

Originally forked from [React Tutorial Om](https://github.com/jalehman/react-tutorial-om)

Frontend written in [Om](https://github.com/swannodette/om).

## Installation

Clone this repo

    git clone git@github.com:minimal/react-tutorial-om.git

Clone submodule

    git submodule update --init

Install submodule

    cd checkouts/ranking-algorithms
    lein install
    cd ../..

Compile js

    lein cljsbuild once dev
    # or for development
    lein cljsbuild auto dev


Run server

    lein ring server

Point Browser to

    http://localhost:3000
