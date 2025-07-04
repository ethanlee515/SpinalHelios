import spinal.core._
import spinal.lib._
import HeliosParams._

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

case class Correction() extends Bundle {
  // Some 0-th slots are unused and squashed
  val ns_tail = Vec.fill(grid_width_u, grid_width_x - 1, grid_width_z)(Bool())
  val ew_tail = Vec.fill(grid_width_u, grid_width_x - 1, grid_width_z)(Bool())
  val ew_last = Bits(grid_width_u bits)
  val ud_tail = Vec.fill(grid_width_u, grid_width_x, grid_width_z)(Bool())
  // Dealing with 1-indexing correspondingly
  def ns(k: Int, i: Int, j: Int) = ns_tail(k)(i - 1)(j - 1)
  def ew(k: Int, i: Int, j: Int) = {
    if(j != grid_width_z) {
      ew_tail(k)(i - 1)(j)
    } else {
      assert(i == grid_width_x - 1)
      ew_last(k)
    }
  }
  def ud(k: Int, i: Int, j: Int) = ud_tail(k)(i)(j)
}

class DecodingGraph() extends Component {
  /* IO */
  val measurements = in port Vec.fill(grid_width_x, grid_width_z)(Bool()) 
  val global_stage = in port Stage()
  val odd_clusters =
    out port Vec.fill(grid_width_u, grid_width_x, grid_width_z)(Bool())
  val busy = out port Vec.fill(grid_width_u, grid_width_x, grid_width_z)(Bool())
  val correction = out port Correction()
  /* logic */
  // Setting up grid of processing units
  val processing_unit = Seq.tabulate(grid_width_u, grid_width_x, grid_width_z) {
    (k, i, j) => {
    val address = (k << (x_bit_width + z_bit_width)) + (i << z_bit_width) + j
    val neighbor_count = 6
    val pu = new ProcessingUnit(address)
    pu.global_stage := global_stage
    odd_clusters(k)(i)(j) := pu.odd
    busy(k)(i)(j) := pu.busy
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
  // Setting up neighbors
  def neighbor_link_0(
      is_error_out: Bool,
      weight_in: Int)(
      ai: Int, aj: Int, ak: Int,
      bi: Int, bj: Int, bk: Int,
      adir: NeighborID, bdir: NeighborID) = {
    val link = new NeighborLink(weight_in, Boundary.no_boundary)
    val unit_a = processing_unit(ak)(ai)(aj)
    val unit_b = processing_unit(bk)(bi)(bj)
    link.global_stage := global_stage
    unit_a.neighbor_fully_grown(adir.id) := link.fully_grown
    unit_b.neighbor_fully_grown(bdir.id) := link.fully_grown
    link.a_increase := unit_a.neighbor_increase
    link.b_increase := unit_b.neighbor_increase
    unit_a.neighbor_is_boundary(adir.id) := link.is_boundary
    unit_b.neighbor_is_boundary(bdir.id) := link.is_boundary
    link.a_is_error := unit_a.neighbor_is_error(adir.id)
    link.b_is_error := unit_b.neighbor_is_error(bdir.id)
    is_error_out := link.is_error
    link.a_input_data := unit_a.to_neighbor(adir.id)
    link.b_input_data := unit_b.to_neighbor(bdir.id)
    unit_a.from_neighbor(adir.id) := link.a_output_data
    unit_b.from_neighbor(bdir.id) := link.b_output_data
    link
  }
  def neighbor_link_single(
      is_error_out: Bool,
      weight_in: Int)(
      i: Int, j: Int, k: Int, dir: NeighborID,
      boundary_condition: Boundary.Value) = {
    val link = new NeighborLink(weight_in, boundary_condition)
    val unit = processing_unit(k)(i)(j)
    link.global_stage := global_stage
    unit.neighbor_fully_grown(dir.id) := link.fully_grown
    link.a_increase := unit.neighbor_increase
    link.b_increase := False
    unit.neighbor_is_boundary(dir.id) := link.is_boundary
    link.a_is_error := unit.neighbor_is_error(dir.id)
    link.b_is_error := False
    is_error_out := link.is_error
    link.a_input_data := unit.to_neighbor(dir.id)
    link.b_input_data.assignFromBits(B(0, address_width + 7 bits))
    unit.from_neighbor(dir.id) := link.a_output_data
    link
  }
  val ns = Seq.tabulate(grid_width_u, grid_width_x + 1, grid_width_z + 1) {
      (k, i, j) => new Area {
    val is_error_out = Bool()
    val weight_in = weight_ns
    val link_0 = neighbor_link_0(is_error_out, weight_in) _
    val link_single = neighbor_link_single(is_error_out, weight_in) _
    if (i == 0 && j < grid_width_z) {
      // "first row"
      link_single(i, j, k, NeighborID.north, Boundary.nexist_edge)
    } else if(i == grid_width_x && j < grid_width_z) {
      link_single(i - 1, j, k, NeighborID.south, Boundary.nexist_edge)
    } else if(i < grid_width_x && i > 0 && i % 2 == 1 && j > 0) {
      // "odd rows which are always internal"
      link_0(i - 1, j - 1, k, i, j - 1, k, NeighborID.south, NeighborID.north)
    } else if(i < grid_width_x && i > 0 && i % 2 == 0 && j == 0) {
      // "First element of even rows"
      link_single(i, j, k, NeighborID.north, Boundary.nexist_edge)
    } else if(i < grid_width_x && i > 0 && i % 2 == 0 && j == grid_width_z) {
      // "Last element of even rows"
      link_single(i - 1, j - 1, k, NeighborID.south, Boundary.a_boundary)
    } else if(
      i < grid_width_x && i > 0 && i % 2 == 0 && j > 0 && j < grid_width_z
    ) {
      // "Middle element of even rows"
      link_0(i - 1, j - 1, k, i, j, k, NeighborID.south, NeighborID.north)
    }
  }}
  val ew = Seq.tabulate(
    grid_width_u, grid_width_x + 1, grid_width_z + 1
  ) { (k, i, j) => new Area {
    val is_error_out = Bool()
    val weight_in = weight_ew
    val link_0 = neighbor_link_0(is_error_out, weight_in) _
    val link_single = neighbor_link_single(is_error_out, weight_in) _
    if(i == 0 && j < grid_width_z) {
      // "First row"
      link_single(i, j, k, NeighborID.east, Boundary.nexist_edge)
    } else if(i == grid_width_x && j < grid_width_z) {
      // "Last row"
      link_single(i - 1, j, k, NeighborID.west, Boundary.nexist_edge)
    } else if(i < grid_width_x && i > 0 && i % 2 == 0 && j < grid_width_z) {
      // "even rows which are always internal"
      link_0(i, j, k, i - 1, j, k, NeighborID.east, NeighborID.west)
    } else if(i < grid_width_x && i > 0 && i % 2 == 1 && j == 0) {
      // "First element of odd rows"
      link_single(i - 1, j, k, NeighborID.west, Boundary.a_boundary)
    } else if(
      i < grid_width_x - 1 && i > 0 && i % 2 == 1 && j == grid_width_z
    ) {
      // "Last element of odd rows excluding last row"
      link_single(i, j - 1, k, NeighborID.east, Boundary.nexist_edge)
    } else if(i == grid_width_x - 1 && j == grid_width_z) {
      // Last element of last odd row
      link_single(i, j - 1, k, NeighborID.east, Boundary.a_boundary)
    } else if(
      i < grid_width_x && i > 0 && i % 2 == 1 && j > 0 && j < grid_width_z
    ) {
      // Middle elements of odd rows
      link_0(i, j - 1, k, i - 1, j, k, NeighborID.east, NeighborID.west)
    }
  }}
  val ud = Seq.tabulate(grid_width_u + 1, grid_width_x, grid_width_z) {
      (k, i, j) => new Area {
    val is_error_out = Bool()
    val weight_in = weight_ud
    val link_0 = neighbor_link_0(is_error_out, weight_in) _
    val link_single = neighbor_link_single(is_error_out, weight_in) _
    if(k == 0) {
      link_single(i, j, k, NeighborID.down, Boundary.a_boundary)
    } else if(k == grid_width_u) {
      link_single(i, j, k - 1, NeighborID.up, Boundary.nexist_edge)
    } else {
      link_0(i, j, k - 1, i, j, k, NeighborID.up, NeighborID.down)
    }
  }}
  // correction outputs
  for(k <- 0 until grid_width_u;
      i <- 1 until grid_width_x;
      j <- 1 to grid_width_z) {
    correction.ns(k, i, j) := ns(k)(i)(j).is_error_out
  }
  for(k <- 0 until grid_width_u;
      i <- 1 until grid_width_x;
      j <- 0 until grid_width_z) {
    correction.ew(k, i, j) := ew(k)(i)(j).is_error_out
  }
  for(k <- 0 until grid_width_u) {
    correction.ew(k, grid_width_x - 1, grid_width_z) :=
      ew(k)(grid_width_x - 1)(grid_width_z).is_error_out
  }
  for(k <- 0 until grid_width_u;
      i <- 0 until grid_width_x;
      j <- 0 until grid_width_z) {
    correction.ud(k, i, j) := ud(k)(i)(j).is_error_out
  }
}