import spinal.core._
import spinal.lib._
import utest._
import HeliosParams._
import spinal.core.sim._
import spinal.core.formal._
import spinal.core.assert

class BoxedLink extends BlackBox {
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
  addRTLPath("./ext/Helios_scalable_QEC/design/channels/neighbor_link_internal_v2.v")
  addRTLPath("./ext/Helios_scalable_QEC/parameters/parameters.sv")
  setBlackBoxName("neighbor_link_internal")
}

class WrappedLink(weight: Int, boundary: Boundary.Value) extends Component {
  // IO
  val global_stage = in port Stage()
  val fully_grown = out Bool()
  val a_increase, b_increase = in Bool()
  val is_boundary = out Bool()
  val a_is_error, b_is_error = in Bool()
  val is_error = out Bool()
  val a_input_data, b_input_data = in port NeighborsCommunication()
  val a_output_data, b_output_data = out port NeighborsCommunication()
  // instantiate and connect
  val link = new BoxedLink()
  val reference_stage_ids = Map(
    Stage.measurement_loading -> 1,
    Stage.grow -> 2,
    Stage.merge -> 3,
    Stage.peeling -> 4,
    Stage.result_valid -> 5,
    Stage.measurement_preparing -> 7
  )
  val parameters_loaded = Reg(Bool()) init(False)
  when(parameters_loaded) {
    switch(global_stage) {
      for((stage, stage_id) <- reference_stage_ids) {
        is(stage) {
          link.io.global_stage := B(stage_id)
        }
      }
    }
  } otherwise {
    // stage = "parameters loading"
    link.io.global_stage := B(6)
    parameters_loaded := True
  }
  fully_grown := link.io.fully_grown
  link.io.a_increase := a_increase
  link.io.b_increase := b_increase
  is_boundary := link.io.is_boundary
  link.io.a_is_error_in := a_is_error
  link.io.b_is_error_in := b_is_error
  is_error := link.io.is_error
  link.io.a_input_data := a_input_data.asBits
  link.io.b_input_data := b_input_data.asBits
  a_output_data.assignFromBits(link.io.a_output_data)
  b_output_data.assignFromBits(link.io.b_output_data)
  link.io.weight_in := B(weight)
  link.io.boundary_condition_in := B(boundary match {
    case Boundary.no_boundary => 0
    case Boundary.a_boundary => 1
    case Boundary.nexist_edge => 2
  }, 2 bits)
  link.io.is_error_systolic_in := False
}

class LinkEquivalenceChecker(boundary: Boundary.Value) extends Component {
  val weight = 2
  // Inputs
  val global_stage = in port Stage()
  val a_increase, b_increase = in Bool()
  val a_is_error, b_is_error = in Bool()
  val a_input_data, b_input_data = in port NeighborsCommunication()
  // Outputs eqs
  val is_error_eq = out port Bool()
  val is_boundary_eq = out port Bool()
  val fully_grown_eq = out port Bool()
  val a_output_data_eq = out port Bool()
  val b_output_data_eq = out port Bool()
  // wrapped link
  val reference = new WrappedLink(weight, boundary)
  reference.global_stage := global_stage
  reference.a_increase := a_increase
  reference.b_increase := b_increase
  reference.a_is_error := a_is_error
  reference.b_is_error := b_is_error
  reference.a_input_data := a_input_data
  reference.b_input_data := b_input_data
  // link
  val link = new NeighborLink(weight, boundary)
  val is_first_stage = Reg(Bool()) init(True)
  when(is_first_stage) {
    link.global_stage := Stage.measurement_loading
    is_first_stage := False
  } otherwise {
    link.global_stage := global_stage
  }
  link.a_increase := a_increase
  link.b_increase := b_increase
  link.a_is_error := a_is_error
  link.b_is_error := b_is_error
  link.a_input_data := a_input_data
  link.b_input_data := b_input_data
  // check eqs
  is_error_eq := link.is_error === reference.is_error
  is_boundary_eq := link.is_boundary === reference.is_boundary
  fully_grown_eq := link.fully_grown === reference.fully_grown
  a_output_data_eq := link.a_output_data === reference.a_output_data
  b_output_data_eq := link.b_output_data === reference.b_output_data
}

class LinkVerifier(boundary: Boundary.Value) extends Component {
  val dut = FormalDut(new LinkEquivalenceChecker(boundary))
  anyseq(dut.global_stage)
  anyseq(dut.a_increase)
  anyseq(dut.b_increase)
  anyseq(dut.a_is_error)
  anyseq(dut.b_is_error)
  anyseq(dut.a_input_data)
  anyseq(dut.b_input_data)
  assert(dut.is_error_eq)
  assert(dut.is_boundary_eq)
  assert(dut.fully_grown_eq)
  assert(dut.a_output_data_eq)
  assert(dut.b_output_data_eq)
}

object LinkTest extends TestSuite {
  def tests = Tests {
    test("Formally verifying neighbor link with no boundary") {
      FormalConfig.withBMC(15).doVerify(new LinkVerifier(Boundary.no_boundary))
    }
  }
}