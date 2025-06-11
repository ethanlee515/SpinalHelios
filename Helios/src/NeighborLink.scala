// Ported from "neighbor_link_internal_v2.v"

import spinal.core._
import spinal.lib._

class NeighborLink(
  address_width: Int = 6,
  max_weight: Int = 2
) extends Component {
  // `+ 1` to store from 0 to max_weight *inclusive*
  val link_bit_width = log2Up(max_weight + 1)
//  val exposed_data_size = address_width + 1 + 1 + 1 + 1 + 3
  /* IO */
  val global_stage = in port Stage()
  val fully_grown = out Bool()
  val a_increase = in Bool()
  val b_increase = in Bool()
  val is_boundary = out Bool()
  val a_is_error = in Bool()
  val b_is_error = in Bool()
  val is_error = out port Reg(Bool()) init(False)
  val a_input_data = in port NeighborsCommunication(address_width)
  val b_input_data = in port NeighborsCommunication(address_width)
  val a_output_data = out port NeighborsCommunication(address_width)
  val b_output_data = out port NeighborsCommunication(address_width)
  val weight_in = in UInt(link_bit_width bits)
  val boundary_condition_in = in port BoundaryCondition()
  val is_error_systolic = in Bool()
  val weight_out = out port Reg(UInt(link_bit_width bits)) init(0)
  val boundary_condition_out = out port Reg(BoundaryCondition()) init(
    BoundaryCondition.no_boundary)
  /* states */
  val growth = Reg(UInt(link_bit_width bits)) init(0)
  /* logic */
  // compute growth
  val s = UInt(link_bit_width + 1 bits)
  switch(boundary_condition_out) {
    is(BoundaryCondition.no_boundary) {
      s := (growth +^ U(a_increase)) + U(b_increase)
    }
    is(BoundaryCondition.a_boundary) {
      s := growth +^ U(a_increase)
    }
    default {
      s := 0
    }
  }
  when(global_stage === Stage.measurement_loading) {
    growth := 0
  } otherwise {
    when(s > weight_out) {
      growth := weight_out
    } otherwise {
      growth := s.resized
    }
  }
  // compute is_error
  switch(boundary_condition_out) {
    is(BoundaryCondition.no_boundary) {
      switch(global_stage) {
        is(Stage.measurement_loading) {
          is_error := False
        }
        is(Stage.result_valid) {
          is_error := is_error_systolic
        }
        default {
          is_error := a_is_error || b_is_error
        }
      }
    }
    is(BoundaryCondition.a_boundary) {
      switch(global_stage) {
        is(Stage.measurement_loading) {
          is_error := False
        }
        is(Stage.result_valid) {
          is_error := is_error_systolic
        }
        default {
          is_error := a_is_error
        }
      }
    }
    default {
      is_error := False
    }
  }
  fully_grown := growth >= weight_out
  is_boundary := (boundary_condition_out === BoundaryCondition.a_boundary) &&
    fully_grown
  when(boundary_condition_out === BoundaryCondition.no_boundary) {
    a_output_data := b_input_data
    b_output_data := a_input_data
  } otherwise {
    a_output_data.asBits := B(0)
    b_output_data.asBits := B(0)
  }
/*
  a_output_data := (boundary_condition_out === BoundaryCondition.no_boundary) ?
    b_input_data | 0
  b_output_data := (boundary_condition_out === BoundaryCondition.no_boundary) ?
    a_input_data | 0
*/
  when(global_stage === Stage.parameters_loading) {
    weight_out := weight_in
    boundary_condition_out := boundary_condition_in
  }
}

object NeighborLinkVerilog extends App {
  SpinalVerilog(new NeighborLink())
}
