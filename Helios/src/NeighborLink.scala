import spinal.core._
import spinal.lib._
import HeliosParams._

class NeighborLink(
  weight: Int,
  boundary_condition: Boundary.Value
) extends Component {
  assert(weight <= max_weight)
  /* IO */
  val global_stage = in port Stage()
  val fully_grown = out Bool()
  val a_increase, b_increase = in Bool()
  val is_boundary = out Bool()
  val a_is_error, b_is_error = in Bool()
  val is_error = out port Reg(Bool()) init(False)
  val a_input_data, b_input_data = in port NeighborsCommunication()
  val a_output_data, b_output_data = out port NeighborsCommunication()
  /* states */
  // `+ 1` to store from 0 to max_weight *inclusive*
  val link_bit_width = log2Up(max_weight + 1)
  val growth = Reg(UInt(link_bit_width bits)) init(0)
  /* logic */
  // compute growth
  val s = UInt(link_bit_width + 1 bits)
  boundary_condition match {
    case Boundary.no_boundary => {
      s := (growth +^ U(a_increase)) + U(b_increase)
    }
    case Boundary.a_boundary => {
      s := growth +^ U(a_increase)
    }
    case _ => {
      s := 0
    }
  }
  val growth_new = UInt(link_bit_width bits)
  when(s > weight) {
    growth_new := weight
  } otherwise {
    growth_new := s.resized
  }

  when(global_stage === Stage.measurement_loading) {
    growth := 0
  } otherwise {
    growth := growth_new
  }
  // compute is_error
  boundary_condition match {
    case Boundary.no_boundary => {
      switch(global_stage) {
        is(Stage.measurement_loading) {
          is_error := False
        }
        default {
          is_error := a_is_error || b_is_error
        }
      }
    }
    case Boundary.a_boundary => {
      switch(global_stage) {
        is(Stage.measurement_loading) {
          is_error := False
        }
        default {
          is_error := a_is_error
        }
      }
    }
    case _ => {
      is_error := False
    }
  }
  fully_grown := (growth >= weight)
  is_boundary := {
    if(boundary_condition == Boundary.a_boundary)
      fully_grown
    else
      False
  }
  if(boundary_condition == Boundary.no_boundary) {
    a_output_data := b_input_data
    b_output_data := a_input_data
  } else {
    a_output_data.assignFromBits(B(0, address_width + 7 bits))
    b_output_data.assignFromBits(B(0, address_width + 7 bits))
  }
}
