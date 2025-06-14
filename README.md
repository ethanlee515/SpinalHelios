# SpinalHelios

Reimplementation of ["Helios" quantum error correction](https://github.com/NamiLiy/Helios_scalable_QEC) in SpinalHDL.

Usage:
* `./mill Helios.runMain CompileVerilog`: Output Verilog as "HeliosCore.v"
* `./mill Helios.test`: run all tests. (Not yet implemented.)

## Prerequisites

This is tested on Ubuntu, and will probably work for any reasonable Linux.
Should be straightforward to adapt for other operating systems as well.

Requires a reasonably recent version of JVM (so, JDK or runtime) and Verilator.
In particular, Scala or Mill is not required;
the `./mill` wrapper script takes care of that.
