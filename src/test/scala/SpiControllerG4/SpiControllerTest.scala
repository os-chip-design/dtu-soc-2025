package SpiControllerG4

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chiseltest._

// to run this write in the terminal: sbt "testOnly SpiControllerG4.SpiControllerTest"
class SpiControllerTest extends AnyFlatSpec with ChiselScalatestTester {
  "SPI Controller" should "transmit data correctly" in {
    test(new SpiControllerG4.SpiControllerG4).withAnnotations(Seq(WriteVcdAnnotation)) { c =>

    }
  }
}