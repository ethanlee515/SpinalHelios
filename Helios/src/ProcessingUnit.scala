package helios
import spinal.core._
import spinal.lib._

class ProcessingUnit(address: Int, params: HeliosParams) extends Component {
  import params._
  /* IO */
  val measurement = in Bool()
  val measurement_out = out port Reg(Bool()) init(False)
  val global_stage = in port Stage()
  val neighbor_fully_grown = in Bits(neighbor_count bits)
  val neighbor_increase = out Bool()
  val neighbor_is_boundary = in Bits(neighbor_count bits)
  val neighbor_is_error = out Bits(neighbor_count bits)
  val from_neighbor =
    in port Vec.fill(neighbor_count)(NeighborsCommunication(params))
  val to_neighbor =
    out port Vec.fill(neighbor_count)(NeighborsCommunication(params))
  val root = Reg(UInt(address_width bits))
  val odd = out port Reg(Bool())
  val busy = out port Reg(Bool()) init(False)
  /* logic */
  // unpacking `input_data`
  val neighbor_root = Vec(from_neighbor.map(_.root))
  val neighbor_parent_vector = Vec(from_neighbor.map(_.parent_vector)).asBits
  val parent_odd = Vec(from_neighbor.map(_.odd)).asBits
  val child_cluster_parity = Vec(from_neighbor.map(_.cluster_parity)).asBits
  val child_touching_boundary =
    Vec(from_neighbor.map(_.touching_boundary)).asBits
  val child_peeling_complete =
    Vec(from_neighbor.map(_.peeling_complete)).asBits
  val child_peeling_m = Vec(from_neighbor.map(_.peeling_m)).asBits
  val parent_peeling_parity_completed =
    Vec(from_neighbor.map(_.peeling_parity_completed)).asBits
  // unpacking `output_data`
  // output looks like a wire, but it's in fact backed by these registers.
  val parent_vector = Reg(Bits(neighbor_count bits))
  val odd_to_children = Reg(Bits(neighbor_count bits))
  val cluster_parity = Reg(Bool())
  val cluster_touching_boundary = Reg(Bool())
  val peeling_complete = Reg(Bool()) init(False)
  val peeling_m = Reg(Bool()) init(False)
  val peeling_parity_completed = Reg(Bool()) init(False)
  for(i <- 0 until neighbor_count) {
    to_neighbor(i).root := root
    to_neighbor(i).parent_vector := parent_vector(i)
    to_neighbor(i).odd := odd_to_children(i)
    to_neighbor(i).cluster_parity := cluster_parity
    to_neighbor(i).touching_boundary := cluster_touching_boundary
    to_neighbor(i).peeling_complete := peeling_complete
    to_neighbor(i).peeling_m := peeling_m
    to_neighbor(i).peeling_parity_completed := peeling_parity_completed
  }
  // book-keeping
  val stage = Reg(Stage()) init(Stage.measurement_preparing)
  val last_stage = Reg(Stage()) init(Stage.measurement_preparing)
  stage := global_stage
  last_stage := stage
  when(stage === Stage.measurement_loading) {
    measurement_out := measurement
  }
  neighbor_increase :=
    odd && (stage === Stage.grow) && (last_stage =/= Stage.grow)
  // solver stuff
  val solver = new MinValLess8xWithIndex(address_width, neighbor_count)
  for(i <- 0 until neighbor_count) {
    solver.values(i).payload := neighbor_root(i)
    solver.values(i).valid := neighbor_fully_grown(i) &&
      !neighbor_is_boundary(i)
  }
  switch(stage) {
    is(Stage.measurement_loading) {
      root := address
      parent_vector := B(0)
    }
    is(Stage.merge) {
      when(solver.valids.orR && (solver.result < root)) {
        root := solver.result
        parent_vector := solver.valids
      }
    }
  }
  // "Calculate the sub-tree parity and sub_tree touching boundary"
  val next_cluster_parity =
    (neighbor_parent_vector & child_cluster_parity).xorR ^ measurement_out
  val next_cluster_touching_boundary =
    (neighbor_parent_vector & child_touching_boundary).orR ||
    neighbor_is_boundary.orR
  switch(stage) {
    is(Stage.measurement_loading) {
      cluster_parity := measurement
      cluster_touching_boundary := False
    }
    is(Stage.merge) {
      cluster_parity := next_cluster_parity
      cluster_touching_boundary := next_cluster_touching_boundary
    }
  }
  // odd stuff
  switch(stage) {
    is(Stage.measurement_loading) {
      odd := measurement
      odd_to_children.setAllTo(measurement)
    }
    is(Stage.merge) {
      when(parent_vector.orR) {
        val o = (parent_vector & parent_odd).orR
        odd := o
        odd_to_children.setAllTo(o)
      } otherwise {
        val o = next_cluster_parity && !next_cluster_touching_boundary
        odd := o
        odd_to_children.setAllTo(o)
      }
    }
    is(Stage.peeling) {
      when(!parent_vector.orR) {
        when(!next_cluster_parity) {
          odd := False
          odd_to_children := B(0)
        } otherwise {
          odd := True
          odd_to_children := neighbor_is_boundary.orR ? B(0) |
            OHMasking.last(neighbor_parent_vector & child_touching_boundary)
        }
      } otherwise {
        odd := False
        when((parent_vector & parent_odd).orR) {
          when(neighbor_is_boundary.orR) {
            odd := True
            odd_to_children := B(0)
          } otherwise {
            odd := False
            odd_to_children := OHMasking.last(
              neighbor_parent_vector & child_touching_boundary)
          }
        } otherwise {
          odd_to_children := B(0)
        }
      }
    }
  }
  // compute `peeling_parity_completed`
  switch(stage) {
    is(Stage.measurement_loading) {
      peeling_parity_completed := False
    }
    is(Stage.peeling) {
      peeling_parity_completed := (!parent_vector.orR) ? True |
        (parent_vector & parent_peeling_parity_completed).orR
    }
  }
  // "Calculate peeling complete"
  val child_incomplete = (neighbor_parent_vector &
    ~child_peeling_complete).orR || (!peeling_parity_completed)
  switch(stage) {
    is(Stage.measurement_loading) {
      peeling_complete := False
    }
    is(Stage.peeling) {
      peeling_complete := !child_incomplete
    }
  }
  // "If peeling complete, absorb children's m while marking the errors"
  when(stage === Stage.peeling && last_stage =/= Stage.peeling) {
    peeling_m := measurement_out
  } otherwise {
    when(stage === Stage.peeling && !child_incomplete) {
      peeling_m := measurement_out ^ ((neighbor_parent_vector & child_peeling_m).xorR) ^ odd
    }
  }
  // Calculate `neighbor_is_error`
  val neighbor_is_error_internal = Bits(neighbor_count bits)
  neighbor_is_error_internal :=
    (stage === Stage.peeling && !child_incomplete) ?
    (neighbor_parent_vector & child_peeling_m) | B(0)
  val neighbor_is_error_border = Bits(neighbor_count bits)
  neighbor_is_error_border :=
    (stage === Stage.peeling && !child_incomplete && odd) ?
    OHMasking.last(neighbor_is_boundary) | B(0)
  neighbor_is_error := neighbor_is_error_internal | neighbor_is_error_border
  // Calculate `busy`
  switch(stage) {
    is(Stage.merge) {
      busy := (solver.valids.orR && solver.result < root) ||
        next_cluster_parity =/= cluster_parity ||
        next_cluster_touching_boundary =/= cluster_touching_boundary ||
        (parent_vector.orR && ((parent_vector & parent_odd).orR =/= odd)) ||
        (!(parent_vector.orR) &&
          ((next_cluster_parity & !next_cluster_touching_boundary) =/= odd))
    }
    is(Stage.peeling) {
      busy := child_incomplete
    }
  }
}
