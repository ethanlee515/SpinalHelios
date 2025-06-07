# SpinalHelios

Reimplementation of ["Helios" quantum error correction](https://github.com/NamiLiy/Helios_scalable_QEC) in SpinalHDL.

Usage:
* `./mill Helios.test`: run all tests
* TODO build verilog? Vivado stuff?

## Prerequisites

This is tested on Ubuntu, and will probably work for any reasonable Linux.
Should be straightforward to adapt for other operating systems as well.

Requires a reasonably recent version of JVM (so, JDK or runtime) and Verilator.
In particular, Scala or Mill is not required;
the `./mill` wrapper script takes care of that.

## Comparisons with the Original Verilog Version

In an ideal world, this implementation would match the original verilog,
not only in input-output behavior, but "down to the netlist".
The desirable benchmarks would then be preserved.

Instead of that, we list and justify our adjustments below.

### Tree Compare Solver

Many signals are turned into `Flow`s and `Vec`s accordingly.
Many `CHANNEL_COUNT` parameters are now hard-coded.

###  Controller

`StreamFIFO` and serializer parts scrapped
