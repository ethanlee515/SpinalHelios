# SpinalHelios

Reimplementation of "Helios" quantum error correction in SpinalHDL.

Usage:
* `./mill Helios.test`: run all tests
* TODO build verilog? Vivado stuff?

## Prerequisites

This is tested on Ubuntu, and will probably work for any reasonable Linux.
Should be straightforward to adapt for other operating systems as well.

Requires a reasonably recent version of JVM (so, JDK or runtime) and Verilator.
Specifically, Scala or Mill is not required.
The `./mill` wrapper script takes care of that.

## Departures from the Original Verilog Version

The goal of this implementation is to match the original verilog version,
not only in input-output behavior, but ideally "down to the netlist".
This is so that the desirable benchmarks are all preserved.
If we must make adjustments however, we list and justify them below.
* We use SpinalHDL's `StreamFIFO`, instead of implementing one from scratch.
* Similarly, we use SpinalHDL's serializer (or whatever it is that combines streams?)
