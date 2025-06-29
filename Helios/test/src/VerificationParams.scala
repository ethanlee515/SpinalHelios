import spinal.core._
import spinal.lib._

object TestParams {
  val reference_stage_ids = Map(
    Stage.measurement_loading -> 1,
    Stage.grow -> 2,
    Stage.merge -> 3,
    Stage.peeling -> 4,
    Stage.result_valid -> 5,
    Stage.measurement_preparing -> 7
  )
  val num_shots = 100
}