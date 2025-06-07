import utest._

// TODO port the corresponding test
object HelloTest extends TestSuite {
  def tests = Tests {
    test("hello") {
      assert(55 + 45 == 100)
    }
  }
}
