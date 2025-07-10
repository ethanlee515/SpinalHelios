package helios
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

class DecodingGraph(params: HeliosParams) extends Component {
  import params._
  /* IO */
  val measurements = in port Vec.fill(grid_width_x, grid_width_z)(Bool()) 
  val global_stage = in port Stage()
  val odd_clusters =
    out port Vec.fill(grid_width_u, grid_width_x, grid_width_z)(Bool())
  val busy = out port Vec.fill(grid_width_u, grid_width_x, grid_width_z)(Bool())
  val correction = out port Correction(params)
  /* logic */
  // Setting up grid of processing units
  val processing_unit = Seq.tabulate(grid_width_u, grid_width_x, grid_width_z) {
    (k, i, j) => {
    val address = (k << (x_bit_width + z_bit_width)) + (i << z_bit_width) + j
    val neighbor_count = 6
    val pu = new ProcessingUnit(address, params)
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
    val link = new NeighborLink(weight_in, Boundary.no_boundary, params)
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
    val link = new NeighborLink(weight_in, boundary_condition, params)
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
      (k, s, t) => new Area {
    val is_error_out = Bool()
    val weight_in = weight_ns(k, s, t)
    val link_0 = neighbor_link_0(is_error_out, weight_in) _
    val link_single = neighbor_link_single(is_error_out, weight_in) _
    // first row
    if (s == 0 && t < grid_width_z) {
      link_single(s, t, k, NeighborID.north, Boundary.nexist_edge)
    }
    // Last row
    if(s == grid_width_x && t < grid_width_z) {
      link_single(s - 1, t, k, NeighborID.south, Boundary.nexist_edge)
    }
    // Odd rows which are always internal
    if(s < grid_width_x && s > 0 && s % 2 == 1 && t > 0) {
      link_0(s - 1, t - 1, k, s, t - 1, k, NeighborID.south, NeighborID.north)
    }
    // First element of even rows
    if(s < grid_width_x && s > 0 && s % 2 == 0 && t == 0) {
      link_single(s, t, k, NeighborID.north, Boundary.nexist_edge)
    }
    // "Middle element of even rows"
    if(s < grid_width_x && s > 0 && s % 2 == 0 && t > 0 && t < grid_width_z) {
      link_0(s - 1, t - 1, k, s, t, k, NeighborID.south, NeighborID.north)
    }
    // "Last element of even rows"
    if(s < grid_width_x && s > 0 && s % 2 == 0 && t == grid_width_z) {
      link_single(s - 1, t - 1, k, NeighborID.south, Boundary.a_boundary)
    }
  }}
  val ew = Seq.tabulate(
    grid_width_u, grid_width_x + 1, grid_width_z + 1
  ) { (k, s, t) => new Area {
    val is_error_out = Bool()
    val weight_in = weight_ew(k, s, t)
    val link_0 = neighbor_link_0(is_error_out, weight_in) _
    val link_single = neighbor_link_single(is_error_out, weight_in) _
    // First row
    if(s == 0 && t < grid_width_z) {
      link_single(s, t, k, NeighborID.east, Boundary.nexist_edge)
    }
    // Last row
    if(s == grid_width_x && t < grid_width_z) {
      link_single(s - 1, t, k, NeighborID.west, Boundary.nexist_edge)
    }
    // even rows which are always internal
    if(s < grid_width_x && s > 0 && s % 2 == 0 && t < grid_width_z) {
      link_0(s, t, k, s - 1, t, k, NeighborID.east, NeighborID.west)
    }
    // First element of odd rows
    if(s < grid_width_x && s > 0 && s % 2 == 1 && t == 0) {
      link_single(s - 1, t, k, NeighborID.west, Boundary.a_boundary)
    }
    // Middle elements of odd rows
    if(s < grid_width_x && s > 0 && s % 2 == 1 && t > 0 && t < grid_width_z) {
      link_0(s, t - 1, k, s - 1, t, k, NeighborID.east, NeighborID.west)
    }
    // Last element of odd rows excluding last row
    if(s < grid_width_x - 1 && s > 0 && s % 2 == 1 && t == grid_width_z) {
      link_single(s, t - 1, k, NeighborID.east, Boundary.nexist_edge)
    }
    // Last element of last odd row
    if(s == grid_width_x - 1 && t == grid_width_z) {
      link_single(s, t - 1, k, NeighborID.east, Boundary.a_boundary)
    }
  }}
  val ud = Seq.tabulate(grid_width_u + 1, grid_width_x, grid_width_z) {
      (k, s, t) => new Area {
    val is_error_out = Bool()
    val weight_in = weight_ud(k, s, t)
    val link_0 = neighbor_link_0(is_error_out, weight_in) _
    val link_single = neighbor_link_single(is_error_out, weight_in) _
    if(k == 0) {
      link_single(s, t, k, NeighborID.down, Boundary.a_boundary)
    } else if(k == grid_width_u) {
      link_single(s, t, k - 1, NeighborID.up, Boundary.nexist_edge)
    } else {
      link_0(s, t, k - 1, s, t, k, NeighborID.up, NeighborID.down)
    }
  }}
  // correction outputs
  for(k <- 0 until grid_width_u;
      s <- 1 until grid_width_x;
      t <- 1 to grid_width_z) {
    correction.ns(k, s, t) := ns(k)(s)(t).is_error_out
  }
  for(k <- 0 until grid_width_u;
      s <- 1 until grid_width_x;
      t <- 0 until grid_width_z) {
    correction.ew(k, s, t) := ew(k)(s)(t).is_error_out
  }
  for(k <- 0 until grid_width_u) {
    correction.ew(k, grid_width_x - 1, grid_width_z) :=
      ew(k)(grid_width_x - 1)(grid_width_z).is_error_out
  }
  for(k <- 0 until grid_width_u;
      s <- 0 until grid_width_x;
      t <- 0 until grid_width_z) {
    correction.ud(k, s, t) := ud(k)(s)(t).is_error_out
  }
}