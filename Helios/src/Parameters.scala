package helios
import spinal.core._

case class HeliosParams(
    code_distance: Int = 7,
    // iteration_counter_width: Int = 8,
    max_delay: Int = 3,
    neighbor_count: Int = 6,
    weight_ns: (Int, Int, Int) => Int = {(k: Int, i: Int, j: Int) => 2},
    weight_ew: (Int, Int, Int) => Int = {(k: Int, i: Int, j: Int) => 2},
    weight_ud: (Int, Int, Int) => Int = {(k: Int, i: Int, j: Int) => 2},
) {
  val grid_width_x = code_distance + 1
  val grid_width_z = code_distance / 2
  val grid_width_u = code_distance
  val grid_size = grid_width_u * grid_width_x * grid_width_z
  val x_bit_width = log2Up(grid_width_x)
  val z_bit_width = log2Up(grid_width_z)
  val u_bit_width = log2Up(grid_width_u)
  val address_width = x_bit_width + z_bit_width + u_bit_width
  // max weights
  val ns_indices = {
    for(k <- 0 until grid_width_u;
        i <- 1 until grid_width_x;
        j <- 1 to grid_width_z) yield (k, i, j)
  }
  val ew_indices = {
    val s1 = {
      for(k <- 0 until grid_width_u;
          i <- 1 until grid_width_x;
          j <- 0 until grid_width_z) yield (k, i, j)
    }
    val s2 = {
      val i = grid_width_x - 1
      val j = grid_width_z
      for(k <- 0 until grid_width_u) yield (k, i, j)
    }
    s1 ++ s2
  }
  val ud_indices = {
    for(k <- 0 until grid_width_u;
        i <- 0 until grid_width_x;
        j <- 0 until grid_width_z) yield (k, i, j)
  }
  val max_weight_ns = ns_indices.map(weight_ns.tupled).max
  val max_weight_ew = ew_indices.map(weight_ew.tupled).max
  val max_weight_ud = ud_indices.map(weight_ud.tupled).max
  val max_weight = List(max_weight_ns, max_weight_ew, max_weight_ud).max
}

object Stage extends SpinalEnum {
  val measurement_loading, grow, merge, peeling, result_valid, measurement_preparing = newElement()
}

object Boundary extends Enumeration {
  type Boundary = Value
  val no_boundary, a_boundary, nexist_edge = Value
}
