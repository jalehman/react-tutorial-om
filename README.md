react-tutorial-om
=================

The [React tutorial](http://facebook.github.io/react/docs/tutorial.html) rewritten in [Om](https://github.com/swannodette/om)

## Intro

Like many, [swannodette's](https://github.com/swannodette) [post](http://swannodette.github.io/2013/12/17/the-future-of-javascript-mvcs/) about the release of Om got me really excited. Check out the post if you haven't. 

I worked through the React tutorial yesterday in JS(X), and did it today in Om. I would definitely recommend doing it in that order.

I've tried to stay really close in concept to the original tutorial, but made the following substitutions to be as idiomatic as possible:

+ jQuery (ajax) -> [clj-http](https://github.com/r0man/cljs-http)
+ Showdown (markdown) -> [markdown-clj](https://github.com/yogthos/markdown-clj)

The [original react tutorial repo](https://github.com/petehunt/react-tutorial) includes a nodejs server; therefore, this one has a Clojure one. run `lein ring server` to fire it up.

## Installation

[Leiningen](https://github.com/technomancy/leiningen)

For getting the latest version of CLJS and Om, follow swannodette's instructions [here](https://github.com/swannodette/todomvc/tree/gh-pages/labs/architecture-examples/om).

+ **Client:** `lein cljsbuild once dev`
+ **Server:** `lein ring server`
