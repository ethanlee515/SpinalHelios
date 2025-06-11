// Ported from "design/wrappers/single_FPGA_decoding_graph_dynamic_rsc.sv"

import spinal.core._
import spinal.lib._

object NeighborID extends Enumeration {
  type NeighborID = Value
  val north = Value(0)
  val south = Value(1)
  val west = Value(2)
  val east = Value(3)
  val down = Value(4)
  val up = Value(5)
}

import NeighborID._

class DecodingGraph (
  grid_width_x: Int = 4,
  grid_width_z: Int = 1, // is this ever not 1?
  grid_width_u: Int = 3,
  max_weight: Int = 2
) extends Component {
  /* derived params */
  val x_bit_width = log2Up(grid_width_x)
  val z_bit_width = log2Up(grid_width_z)
  val u_bit_width = log2Up(grid_width_u)
  val address_width = x_bit_width + z_bit_width + u_bit_width
  val pu_count_per_round = grid_width_x * grid_width_z
  val pu_count = pu_count_per_round * grid_width_u
  // this is bonkers
  /*
  val ns_error_count_per_round = (grid_width_x - 1) * grid_width_z
  // why is there a "+ 1" at the end of this?
  val ew_error_count_per_round = (grid_width_x - 1) * grid_width_z + 1
  val ud_error_count_per_round = grid_width_x * grid_width_z
  val correction_count_per_round = ns_error_count_per_round +
    ew_error_count_per_round + ud_error_count_per_round
  */
  /* IO */
  val measurements = in port Vec.fill(grid_width_x)(Bits(grid_width_z bits)) 
  val global_stage = in port Stage()
  // TODO Drive
  /*
  val odd_clusters = out port Bits(pu_count bits)
  val roots = out port Vec.fill(pu_count)(UInt(address_width bits))
  val busy = out port Vec.fill(pu_count)(Bool())
  val ns_correction = out port Vec.fill(grid_width_x - 1)(
    Bits(grid_width_z bits))
  val ew_correction = out port Vec.fill(grid_width_x - 1)(
    Bits(grid_width_z bits))
  val ud_correction = out port Vec.fill(grid_width_x)(Bits(grid_width_z bits))
  */
  /* logic */
  val processing_unit = Seq.tabulate(grid_width_u, grid_width_x, grid_width_z) {
    (k, i, j) => {
    val pu = new ProcessingUnit()
    pu.global_stage := global_stage
    pu
  }}
  for {
    k <- 0 until grid_width_u
    i <- 0 until grid_width_x
    j <- 0 until grid_width_z
  } {
    processing_unit(k)(i)(j).measurement := {
      if(k == grid_width_u - 1)
        measurements(i)(j)
      else
        processing_unit(k + 1)(i)(j).measurement_out
    }
  } 
  val weight_ns = 2
  val weight_ew = 2
  val weight_ud = 2
  def neighbor_link_0(
    ai: Int, aj: Int, ak: Int,
    bi: Int, bj: Int, bk: Int,
    adir: NeighborID, bdir: NeighborID) = {
    val link = new NeighborLink(address_width, max_weight)
    val unit_a = processing_unit(ak)(ai)(aj)
    val unit_b = processing_unit(bk)(bi)(bj)
    link.global_stage := global_stage
    link.a_increase := unit_a.neighbor_increase
    link.b_increase := unit_b.neighbor_increase
    link.a_is_error := unit_a.neighbor_is_error(adir.id)
    link.b_is_error := unit_b.neighbor_is_error(bdir.id)
    link.a_input_data := unit_a.to_neighbor(adir.id)
    link.b_input_data := unit_b.to_neighbor(bdir.id)
    unit_a.from_neighbor(adir.id) := link.a_output_data
    unit_b.from_neighbor(bdir.id) := link.b_output_data
    link.boundary_condition_in := BoundaryCondition.no_boundary
    unit_a.neighbor_fully_grown(adir.id) := link.fully_grown
    unit_b.neighbor_fully_grown(bdir.id) := link.fully_grown
    unit_a.neighbor_is_boundary(adir.id) := link.is_boundary
    unit_b.neighbor_is_boundary(bdir.id) := link.is_boundary
    link
  }
  def neighbor_link_single(i: Int, j: Int, k: Int, dir: NeighborID,
    boundary_condition: BoundaryCondition.E) = {
    val link = new NeighborLink(address_width, max_weight)
    val unit = processing_unit(k)(i)(j)
    link.global_stage := global_stage
    unit.neighbor_fully_grown(dir.id) := link.fully_grown
    link.a_increase := unit.neighbor_increase
    unit.neighbor_is_boundary(dir.id) := link.is_boundary
    link.a_is_error := unit.neighbor_is_error(dir.id)
    link.a_input_data := unit.to_neighbor(dir.id)
    unit.from_neighbor(dir.id) := link.a_output_data
    link.boundary_condition_in := boundary_condition
    link
  }
  // TODO
}
  
object DecodingGraphVerilog extends App {
  SpinalVerilog(new DecodingGraph())
}
