import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class KeyboardSpec extends AnyFlatSpec with ChiselScalatestTester {

  "Keyboard" should "pass" in {
    test(new Keyboard(32)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // Data = {A-1E, B-30, C-2E, D-20, E-12, F-21}
      val dataSet = Seq("b00111100011".U, "b00000110011".U, "b00111010011".U, "b00000010001".U, "b00100100011".U, "b01000010011".U)
      
      // Data = {A-1E pairity, B-38 pairity, C-2E start, D-20 start end, E-12 end, F-21 start end pairity}
      val errorSet = Seq("b00111100001".U, "b00001110011".U, "b10111010011".U, "b10000010000".U, "b00100100010".U, "b11000010000".U)
      
      val mixSet = Seq("b00111100011".U, "b00001110011".U, "b00111010011".U, "b10000010000".U, "b00100100010".U, "b01000010011".U)


      println("\nStart sending keyboard data")
      dut.clock.setTimeout(0)
      dut.io.data.poke(1.B)
      dut.io.step.poke(1.B)
      dut.io.rd.poke(0.B)
      for (dataVal <- dataSet) {
        for (i <- 0 until 11){
          dut.io.step.poke(1.B)
          dut.io.data.poke(dataVal(10-i))
          dut.clock.step(50)
          dut.io.step.poke(0.B)
          dut.clock.step(50)
        }
        dut.io.data.poke(1.B)
        dut.io.step.poke(1.B)
        dut.clock.step(100)
        val result = dut.io.dataOut.peek()
        val error = dut.io.error.peek().litValue//.toLong.toBinaryString
        val dataRes = result(8,1).litValue.toLong.toHexString
        println(" Data: " + dataRes + " - " + error)
      }


      println("\nStart sending wrong keyboard data")
      for (dataVal <- errorSet) {
        for (i <- 0 until 11){
          dut.io.step.poke(1.B)
          dut.io.data.poke(dataVal(10-i))
          dut.clock.step(50)
          dut.io.step.poke(0.B)
          dut.clock.step(50)
        }
        dut.io.data.poke(1.B)
        dut.io.step.poke(1.B)
        dut.clock.step(100)
        val result = dut.io.dataOut.peek()
        val error = dut.io.error.peek().litValue//.toLong.toBinaryString
        val dataRes = result(8,1).litValue.toLong.toHexString
        println(" Data: " + dataRes + " - " + error)
      }
      
      println("\nStart requesting data")
      for (i <- 0 until 12) {
        dut.io.rd.poke(1.B)
        dut.clock.step(5)
        val result = dut.io.rdData.peek()
        val dataRes = result.litValue.toLong.toHexString
        println(" Data: " + dataRes)
        dut.clock.step(5)
        dut.io.rd.poke(0.B)
        dut.clock.step(10)
      }


      println("\nStart sending mixed keyboard data")
      for (dataVal <- mixSet) {
        for (i <- 0 until 11){
          dut.io.step.poke(1.B)
          dut.io.data.poke(dataVal(10-i))
          dut.clock.step(50)
          dut.io.step.poke(0.B)
          dut.clock.step(50)
        }
        dut.io.data.poke(1.B)
        dut.io.step.poke(1.B)
        dut.clock.step(100)
        val result = dut.io.dataOut.peek()
        val error = dut.io.error.peek().litValue//.toLong.toBinaryString
        val dataRes = result(8,1).litValue.toLong.toHexString
        println(" Data: " + dataRes + " - " + error)
      }


      println("\nStart requesting data")
      for (i <- 0 until 12) {
        dut.io.rd.poke(1.B)
        dut.clock.step(5)
        val result = dut.io.rdData.peek()
        val dataRes = result.litValue.toLong.toHexString
        println(" Data: " + dataRes)
        dut.clock.step(5)
        dut.io.rd.poke(0.B)
        dut.clock.step(10)
      }
    }
  }
}
