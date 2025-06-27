import spinal.core._
import spinal.lib._
import utest._
import HeliosParams._
import spinal.core.sim._
import spinal.core.formal._
import spinal.core.assert
import ReferenceParams._

class BoxedUnit(address: Int) extends BlackBox {
  val io = new Bundle {
    val clk, reset = in port Bool()
    val measurement = in port Bool()
    val measurement_out = out port Bool()
    val global_stage = in port Bits(3 bits)
    val neighbor_fully_grown = in port Bits(neighbor_count bits)
    val neighbor_increase = out port Bool()
    val neighbor_is_boundary = in port Bits(neighbor_count bits)
    val neighbor_is_error = out port Bits(neighbor_count bits)
    val input_data = in port Bits(neighbor_count * (address_width + 7) bits)
    val output_data = out port Bits(neighbor_count * (address_width + 7) bits)
    val root = out port Bits(address_width bits)
    val odd, busy = out port Bool()
  }
  mapCurrentClockDomain(io.clk, io.reset)
  addGeneric("ADDRESS_WIDTH", address_width)
  addGeneric("ADDRESS", address)
  noIoPrefix()
  addRTLPath("./ext/Helios_scalable_QEC/design/processing_unit_single_FPGA_v2.v")
  addRTLPath("./ext/Helios_scalable_QEC/design/parameters.sv")
  addRTLPath("./ext/Helios_scalable_QEC/design/tree_compare_solver.sv")
  setBlackBoxName("processing_unit")
}

class UnitEquivChecker extends Component {
  // inputs
  val measurement = in Bool()
  val global_stage = in port Stage()
  val neighbor_fully_grown = in Bits(neighbor_count bits)
  val neighbor_is_boundary = in Bits(neighbor_count bits)
  val from_neighbor =
    in port Vec.fill(neighbor_count)(NeighborsCommunication())
  // output eqs
  val meas_out_eq, neighbor_inc_eq, neighbor_err_eq, to_neighbor_eq, odd_eq, busy_eq = out Bool()
  // instantiate
  val address = 0b01011010
  val reference = new BoxedUnit(address)
  val unit = new ProcessingUnit(address)
  // feeding inputs
  reference.io.measurement := measurement
  unit.measurement := measurement
  // reference
  switch(global_stage) {
    for((stage, stage_id) <- reference_stage_ids) {
      is(stage) {
        reference.io.global_stage := B(stage_id)
      }
    }
  }
  unit.global_stage := global_stage
  reference.io.neighbor_fully_grown := neighbor_fully_grown
  unit.neighbor_fully_grown := neighbor_fully_grown
  reference.io.neighbor_is_boundary := neighbor_is_boundary
  unit.neighbor_is_boundary := neighbor_is_boundary
  for(i <- 0 until neighbor_count) {
    val s = address_width + 7
    val lower = s * i
    val upper = lower + s - 1
    reference.io.input_data(upper downto lower) := from_neighbor(i).asBits
  }
  unit.from_neighbor := from_neighbor
  // check outputs eq
  meas_out_eq := (reference.io.measurement_out === unit.measurement_out)
  neighbor_inc_eq := (reference.io.neighbor_increase === unit.neighbor_increase)
  neighbor_err_eq := (reference.io.neighbor_is_error === unit.neighbor_is_error)
  to_neighbor_eq := (reference.io.output_data === unit.to_neighbor.asBits)
  odd_eq := (reference.io.odd === unit.odd)
  busy_eq := (reference.io.busy === unit.busy)
}

class UnitVerifier extends Component {
  val dut = FormalDut(new UnitEquivChecker())
  assumeInitial(ClockDomain.current.isResetActive)
  anyseq(dut.measurement)
  anyseq(dut.global_stage)
  anyseq(dut.neighbor_fully_grown)
  anyseq(dut.neighbor_is_boundary)
  anyseq(dut.from_neighbor)
  assert(dut.meas_out_eq)
  assert(dut.neighbor_inc_eq)
  assert(dut.neighbor_err_eq)
  assert(dut.to_neighbor_eq)
  assert(dut.odd_eq)
  assert(dut.busy_eq)
}

object ProcessingUnitTest extends TestSuite {
  def tests = Tests {
    test("Formally verifying processing unit") {
      FormalConfig.withBMC(15).doVerify { new UnitVerifier }
    }
  }

}
