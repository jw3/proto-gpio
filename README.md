# README #

This repository is a sandbox to prototype and determine the feasibility and general usefulness of an [Akka](http://akka.io/) based library for providing access to the GPIO on a Raspberry PI.

There are several potential projects mixed into this repo

* gpio4s - GPIO library for Scala
* PiCfg - PI/GPIO configuration DSL
* TemPi - Temperature monitoring utility
* Common devices - Misc devices implemented on top of gpio4s and PiCfg

Currently Pi4j is being used on the backend, but care has been taken not to expose it in the APIs.

There will potentially be an additional project that wraps the PIGPIO library with Scala sugar, resulting in Pi4s
