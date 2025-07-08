package helios
package test

import spinal.core._
import spinal.lib._
import utest._
import HeliosParams._
import spinal.core.sim._
import spinal.core.formal._
import spinal.core.assert
import TestParams._

class BoxedLink(params: HeliosParams) extends BlackBox {
  import params._
  val link_bit_width = log2Up(max_weight + 1)
  val io = new Bundle {
    val clk, reset = in port Bool()
    val global_stage = in port Bits(3 bits)
    val fully_grown = out port Bool()
    val a_increase, b_increase = in port Bool()
    val is_boundary = out port Bool()
    val a_is_error_in, b_is_error_in = in port Bool()
    val is_error = out port Bool()
    val a_input_data, b_input_data = in port Bits(address_width + 7 bits)
    val a_output_data, b_output_data = out port Bits(address_width + 7 bits)
    val weight_in = in port Bits(link_bit_width bits)
    val boundary_condition_in = in port Bits(2 bits)
    val is_error_systolic_in = in port Bool()
    val weight_out = out port Reg(Bits(link_bit_width bits))
    val boundary_condition_out = out port Bits(2 bits)
  }
  mapCurrentClockDomain(io.clk, io.reset)
  addGeneric("ADDRESS_WIDTH", address_width)
  noIoPrefix()
  addRTLPath("./ext/Helios_scalable_QEC/design/neighbor_link_internal_v2.v")
  addRTLPath("./ext/Helios_scalable_QEC/design/parameters.sv")
  setBlackBoxName("neighbor_link_internal")
}

class LinkEquivalenceChecker(boundary: Boundary.Value) extends Component {
  val weight = 2
  val params = HeliosParams()
  // Inputs
  val global_stage = in port Stage()
  val a_increase, b_increase = in Bool()
  val a_is_error, b_is_error = in Bool()
  val a_input_data, b_input_data = in port NeighborsCommunication(params)
  // Outputs eqs
  val is_error_eq = out port Bool()
  val is_boundary_eq = out port Bool()
  val fully_grown_eq = out port Bool()
  val a_output_data_eq = out port Bool()
  val b_output_data_eq = out port Bool()
  // instantiate
  val reference = new BoxedLink(params)
  // reference-only data
  reference.io.weight_in := B(weight)
  reference.io.boundary_condition_in := B(boundary match {
    case Boundary.no_boundary => 0
    case Boundary.a_boundary => 1
    case Boundary.nexist_edge => 2
  }, 2 bits)
  reference.io.is_error_systolic_in := False
  val link = new NeighborLink(weight, boundary, params)
  // replace first stage with loading
  val cycles = Reg(UInt(5 bits)) init(0)
  cycles := cycles + 1
  when(cycles === 0) {
    // reference
    // stage = "parameters loading"
    reference.io.global_stage := B(6)
    // Spinal
    // There is no "parameter loading" in the enum
    link.global_stage := Stage.measurement_loading
  } otherwise {
    switch(global_stage) {
      // reference
      for((stage, stage_id) <- reference_stage_ids) {
        is(stage) {
          reference.io.global_stage := B(stage_id)
        }
      }
    }
    // Spinal
    link.global_stage := global_stage
  }
  reference.io.a_increase := a_increase
  link.a_increase := a_increase
  reference.io.b_increase := b_increase
  link.b_increase := b_increase
  reference.io.a_is_error_in := a_is_error
  link.a_is_error := a_is_error
  reference.io.b_is_error_in := b_is_error
  link.b_is_error := b_is_error
  reference.io.a_input_data := a_input_data.asBits
  link.a_input_data := a_input_data
  reference.io.b_input_data := b_input_data.asBits
  link.b_input_data := b_input_data
  // check eqs
  is_error_eq := link.is_error === reference.io.is_error
  is_boundary_eq := link.is_boundary === reference.io.is_boundary
  fully_grown_eq := link.fully_grown === reference.io.fully_grown
  a_output_data_eq := link.a_output_data.asBits === reference.io.a_output_data
  b_output_data_eq := link.b_output_data.asBits === reference.io.b_output_data
}

class LinkVerifier(boundary: Boundary.Value) extends Component {
  val dut = FormalDut(new LinkEquivalenceChecker(boundary))
  assumeInitial(ClockDomain.current.isResetActive)
  anyseq(dut.global_stage)
  val valid_stages = Stage.elements.filter(_ != Stage.result_valid)
  assume(valid_stages.map(dut.global_stage === _).reduce(_ || _))
  anyseq(dut.a_increase)
  anyseq(dut.b_increase)
  anyseq(dut.a_is_error)
  anyseq(dut.b_is_error)
  anyseq(dut.a_input_data)
  anyseq(dut.b_input_data)
  assert(dut.cycles < 2 || dut.is_error_eq)
  assert(dut.cycles < 2 || dut.is_boundary_eq)
  assert(dut.cycles < 2 || dut.fully_grown_eq)
  assert(dut.cycles < 2 || dut.a_output_data_eq)
  assert(dut.cycles < 2 || dut.b_output_data_eq)
}

object LinkTest extends TestSuite {
  def tests = Tests {
    test("Formally verifying neighbor link with no boundary") {
      FormalConfig.withBMC(15).doVerify(new LinkVerifier(Boundary.no_boundary))
    }

    test("Formally verifying neighbor link with a boundary") {
      FormalConfig.withBMC(15).doVerify(new LinkVerifier(Boundary.a_boundary))
    }

    test("Formally verifying neighbor link with no edge") {
      FormalConfig.withBMC(15).doVerify(new LinkVerifier(Boundary.nexist_edge))
    }
  }
}