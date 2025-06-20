# SpinalHelios

Reimplementation of ["Helios" quantum error correction](https://github.com/NamiLiy/Helios_scalable_QEC) in SpinalHDL.

Usage:
* `./mill Helios.runMain CompileVerilog`: Output Verilog as "HeliosCore.v"
* `./mill Helios.test`: Run the ["root test"](./Helios/test/src/RootTest.scala), which checks that the union find algorithm works as intended.
  This test is ported from what was labelled as ["full test"](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/test_benches/full_tests/single_FPGA_FIFO_verification_test_rsc.sv) in the original Verilog implementation of Helios.
  We are currently working on adding another test that checks if the "peeling" stage indeed outputs the correct Pauli corrections.

## Prerequisites

This is tested on Ubuntu, and will probably work for any reasonable Linux.
Should be straightforward to adapt for other operating systems as well.

Requires a reasonably recent version of JVM (so, JDK or runtime) and Verilator.
In particular, Scala or Mill is not required;
the `./mill` wrapper script takes care of that.
