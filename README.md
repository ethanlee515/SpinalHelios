# SpinalHDL Port of ["Helios"](https://github.com/NamiLiy/Helios_scalable_QEC) Quantum Error Correction

Usage:
* `./mill Helios.runMain CompileVerilog`: Output Verilog as "./HeliosCore.v"
* `./mill Helios.test`: Run the following tests using the [utest](https://github.com/com-lihaoyi/utest) framework.
    1. The ["root test"](./Helios/test/src/RootTest.scala): Checks that the union find algorithm works as intended.
    This test is ported from what was labelled as ["full test"](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/test_benches/full_tests/single_FPGA_FIFO_verification_test_rsc.sv) in the original Verilog implementation of Helios.
    1. The ["correction test"](./Helios/test/src/CorrectionTest.scala): Checks that the output Pauli corrections matches that of the original Verilog implementation.
    1. Formal verification using SymbiYosys

## Prerequisites

This is tested on WSL Ubuntu, and will most likely run on any reasonable Linux.
Should be straightforward to adapt for other operating systems as well.

Requires a reasonably recent version of JDK.
Verilator and SymbiYosys are also required.
In particular, Scala or Mill is not required;
the `./mill` wrapper script takes care of that.

## Interfaces and Handshakes

Recall that Helios takes `grid_width_u` rounds of syndrome measurements to account for errors occurring during the measurement process itself.
Using these syndrome measurements, Helios then computes the corresponding Pauli errors to correct.
We now sketch how this corresponds to the input and output interfaces of our implementation.
1. Parameters such as the code distance can be chosen by modifying ["Parameters.scala"](./Helios/src/Parameters.scala).
2. Instantiate the hardware component using `new HeliosCore()`. This creates the input stream `meas_in` and output flow `output`.
3. The `meas_in` input stream expects `grid_width_x * grid_width_z` booleans.
   It will set the `ready` signal for `grid_width_u` times.
   Each time it is ready, a round of syndrome measurements can be fed as its payload.
4. The `correction` flow outputs a `Correction` bundle.
   This bundle implements a `.ns(k, i, j) -> Bool` function, allowing one to access the Pauli correction for each north-south edge.
   Similarly for `.ew(k, i, j)` and `.ud(k, i, j)`.

See an example in ["Driver.scala"](./Helios/test/src/Driver.scala).

## Comparison with the Original Helios Implementation

We try to translate the original Verilog/SystemVerilog to SpinalHDL in a wire-by-wire and register-by-register way wherever possible.
Our implementation is therefore expected to preserve any timing closure properties satisfied by the original implementation.

### Implemented Subset and Simplifications

We started from [this test](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/test_benches/full_tests/single_FPGA_FIFO_verification_test_rsc.sv) from the original repository.
It simulates the hardware design on [generated](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/software_code/main.c) test [data](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/test_benches/test_data/input_data_3_rsc.txt), and verifies the union-find result against [the correct result](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/test_benches/test_data/output_data_3_rsc.txt) from a [golden reference](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/software_code/union_find.c).
We determined a minimal [subset of components](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/Makefile#L1-L8) required to pass this test.
These components are then ported to SpinalHDL in a mostly file-by-file fashion, with adjustments described below.

First, a couple features are scrapped.
Namely, there is currently no systolic error streaming or serialization.
Instead, the corrections are all outputted in a single cycle.
Additionally, we removed the `parameter_loading` stage from the state machine.
This involves changing the `weight_in` and `boundary_condition_in` in ["Neighbor Link"](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/design/channels/neighbor_link_internal_v2.v#L30-L31) from input wires to parameters.
Finally, by removing the `parameter_loading` stage, there is no longer a meaningful difference between the `idle` and `measurement_preparing` stages.
The two stages are therefore combined, and the [message framing headers](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/parameters/parameters.sv#L15-L17) are no longer necessary.

### Development Effort

By porting the design to SpinalHDL, we reduce the code volume and complexity.
In the original design, everything under ["design/"](https://github.com/ethanlee515/Helios_scalable_QEC/tree/make-test/design) adds up to over 11k lines.
Even counting only the minimal subset described above, the files still [add up](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/Makefile#L38-L39) to 1877 lines.
In contrast, our SpinalHDL implementation is under 800 lines total.

More importantly, a Scala-based implementation improves modularity and readability.
We hope this in turn makes our implementation more maintainable.
Finally, our idiomatic SpinalHDL interface simplifies future usage as a building block in other SpinalHDL-based efforts.

## Project Structure

Here we highlight some of our important building blocks, in roughly the top-down order.
For the theory behind this union-find algorithm, we direct the readers to the [Helios paper](https://arxiv.org/abs/2301.08419).
* ["Helios/src/"](./Helios/src): Main Helios implementation
  * ["HeliosCore.scala"](./Helios/src/HeliosCore.scala): Top level.
    Instantiates and connects the decoding graph and the state machine ("controller") components.
  * ["Parameters.scala"](./Helios/src/Parameters.scala): Parameters such as the _distance_ of the surface code considered.
  * ["DecodingGraph.scala"](./Helios/src/DecodingGraph.scala): Decoding graph that is processed by the union-find algorithm.
    The graph vertices and edges are encoded as follows:
    * ["ProcessingUnit.scala"](./Helios/src/ProcessingUnit.scala): Represents a vertex of the decoding graph.
      Equivalently, an ancilla that stores the measured error syndrome.
    * ["NeighborLink.scala"](./Helios/src/NeighborLink.scala): Represents an edge of the decoding graph.
      Equivalently, a physical qubit of the quantum ECC.
  * ["UnifiedController.scala"](./Helios/src/UnifiedController.scala): State machine for the union-find algorithm.
  * Other utility and miscellaneous files.
* ["Helios/test/src/unit-tests"](./Helios/test/src): Simulations of our Helios core, and tests against expected outputs.
  * ["Root test"](./Helios/test/src/RootTest.scala): The test borrowed from Helios, as described above.
    It is labelled as one of the "full tests", though it in fact focuses on the union-find logic and does *not* verify the final Pauli corrections.
  * ["Corrections test"](./Helios/test/src/CorrectionTest.scala): Checks that the output Pauli corrections match those from the original Helios implementation.
    We created a simple [SystemVerilog testbench](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/test_benches/full_tests/print_corrections.sv) to log the original Helios output into a [text file](https://github.com/ethanlee515/Helios_scalable_QEC/blob/make-test/test_benches/test_data/corrections_7.txt).
    Our "corrections test" then simulates the SpinalHDL port and verifies that the outputs match.
  * ["Flattened Helios"](./Helios/test/src/FlattenedHeliosCore.scala): A variant of the top-level `HeliosCore` module, where `Vec` fields are unfolded to avoid issues with `simPublic()`.
  * ["Driver.scala"](./Helios/test/src/Driver.scala): Usage example for how to drive the input stream and observe the output flow.
* ["Helios/test/src/formal-verification"](./Helios/test/src/formal-verification/): Formally verifying the equivalence between our building blocks and those of the original Helios.
