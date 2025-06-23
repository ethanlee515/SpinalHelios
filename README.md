# SpinalHDL Port of ["Helios" Quantum Error Correction](https://github.com/NamiLiy/Helios_scalable_QEC).

**NOTE: This README is still incomplete. It will be fully written soon.**

Usage:
* `./mill Helios.runMain CompileVerilog`: Output Verilog as "HeliosCore.v"
* `./mill Helios.test`: Run the following tests:
    1. Run the ["root test"](./Helios/test/src/RootTest.scala), which checks that the union find algorithm works as intended.
    This test is ported from what was labelled as ["full test"](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/test_benches/full_tests/single_FPGA_FIFO_verification_test_rsc.sv) in the original Verilog implementation of Helios.
    1. Run the ["correction test"](./Helios/test/src/CorrectionTest.scala), which checks that the output Pauli corrections matches that of the original Verilog implementation.

## Prerequisites

This is tested on WSL Ubuntu, and will most likely run on any reasonable Linux.
Should be straightforward to adapt for other operating systems as well.

Requires a reasonably recent version of JDK and Verilator.
In particular, Scala or Mill is not required;
the `./mill` wrapper script takes care of that.

## Handshakes and Interfaces

Here is how to use our implementation in a larger project.
TODO recap; quantum ECC maps syndrome measurements to Pauli corrections.
Here is a quick rundown on how to pass the measurements to the Helios core and receive the output corrections.
1. Choose parameters (such as code distance) by modifying ["Parameters.scala"](./Helios/src/Parameters.scala).
2. Instantiate the hardware component using `new HeliosCore()`. This creates the input and output ports:
  * input port `meas_in` of type TODO
  * output port `correction` of type TODO
3. The `meas_in` will set the `ready` signal for `grid_width_u` times.
   Each time this signal is set, a layer of input should be fed into TODO.
4. Wait until the output correction flow gives you a valid payload.
See an example in ["Driver.scala"](./Helios/test/src/Driver.scala).

Note: The `roots` are not a real output wire.
That is pending clean-up.

## Comparison with the Verilog Version

We try to translate the original Verilog to SpinalHDL in a wire-by-wire and register-by-register way where possible.
Our implementation is therefore expected to preserve any timing closure properties satisfied by the original Verilog implementation.

### Implemented Subset and Simplifications

We started from [this test](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/test_benches/full_tests/single_FPGA_FIFO_verification_test_rsc.sv) from the Verilog version.
It is labelled as one of the "full tests", and (TODO describe).
We determined a minimal subset of hardware components [here](TODO) that gets it to pass.
The components are then ported to SpinalHDL, with the following adjustments.

First, a couple features are scrapped.
Namely, there is currently no systolic error streaming or serialization.
Instead, the corrections are all outputed in a single cycle.

We then removed the `parameter_loading` stage from the state machine.
This stage originally sets the parameters from input wires into registers in the edges of the decoding graph.
These parameters are then chosen statically and hard-coded as part of the decoding graph.
The `weight_in` and `boundary_condition_in` inputs in [Neighbor Link](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/design/channels/neighbor_link_internal_v2.v#L30-L31) are turned from input wires to parameters.

Finally, by removing `parameter_loading`, there is no longer a meaningful difference between the `idle` and `measurement_preparing` stages.
The two stages are therefore combined, and the [message framing headers](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/parameters/parameters.sv#L15-L17) are no longer necessary.

### Development Effort

Reduction in code volume and complexity.
Minimal subset Verilog files = 1877 lines
Everything under "design/" = 11k+ lines
Now just under 800 lines of Scala.

Scala = more modular and readable. Maintainable. (We hope.)

## Project Structure

We now assume rudimentary understanding of the union-find based algorithm as described in the (TODO link paper).
Here we describe our implementation in roughly top-down order.
* "Helios/src/": Main Helios implementation
  * ["HeliosCore.scala"](./Helios/src/HeliosCore.scala): Top level.
    Instantiates and connects the decoding graph and the state machine ("controller") components.
    Adapted from TODO.
  * ["Parameters.scala"](./Helios/src/Parameters.scala): Parameters such as the _distance_ of the surface code considered.
  * ["DecodingGraph.scala"](./Helios/src/DecodingGraph.scala): Decoding graph that represents TODO.
    Adapted from TODO.
    The graph vertices and edges are encoded as follows:
    * ["ProcessingUnit.scala"](./Helios/src/ProcessingUnit.scala): Represents a vertex of the decoding graph.
      Equivalently, an ancilla that stores the measured error syndrome.
      Adapted from TODO.
    * ["NeighborLink.scala"](./Helios/src/NeighborLink.scala): Represents an edge of the decoding graph.
      Equivalently, a physical qubit of the quantum ECC.
      Adapted from TODO.
  * ["UnifiedController.scala"](./Helios/src/UnifiedController.scala): State machine for the union-find algorithm.
    Adapted from TODO.
  * Other utility and miscellaneous files.
* "Helios/tests/src": Tests

## TODO

* Generate a bigger "full test" and make sure the entire experiment still works.
  * Check that the implementation still works when `grid_width_z != 1`
  * Try values of `grid_width_x` and `grid_width_z` that are not powers of two.
    (This might break due to inconsistent uses of bitshifts vs multiplications/modulos.)
* `meas_in` is taken over multiple rounds. This is asymmetrical from how output is treated.
  Maybe we can try to take all measurements upfront.
* `roots` is a debug signal and shouldn't be output wire.
   The test should just grab it using `simPublic()`.
* Move edge weights to the parameters file