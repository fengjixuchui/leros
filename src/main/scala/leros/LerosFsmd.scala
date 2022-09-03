package leros

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

import StateFsmd._

/**
 * Leros top level.
 *
 * Sequential implementation with two states.
 */
class LerosFsmd(size: Int, memSize: Int, prog: String, fmaxReg: Boolean) extends Leros(size, memSize, prog, fmaxReg) {


  val stateReg = RegInit(sFeDec)

  val decReg = RegInit(DecodeFsmdOut.default)

  // Decode
  val dec = Module(new DecodeFsmd())
  dec.io.din := instr(15, 8)
  val decout = dec.io.dout

  // Operand - should move into decode
  when(decout.nosext) {
    operand := instr(7, 0)
  } .elsewhen(decout.enahi) {
    operand := instrSignExt(23, 0).asUInt ## accu(7, 0)
  } .elsewhen(decout.enah2i) {
    operand := instrSignExt(15, 0).asUInt ## accu(15, 0)
  } .elsewhen(decout.enah3i) {
    operand := instr(7, 0) ## accu(23, 0)
  } .otherwise {
    operand := instrSignExt.asUInt
  }

  val effAddr = (addrReg.asSInt + instrSignExt).asUInt
  val effAddrWord = (effAddr >> 2).asUInt

  val address = Mux(decout.isLoadInd, effAddrWord, instr(7, 0))
  val dataRead = dataMem.read(address)

  // defaults should be:
  alu.io.enaMask := 0.U
  alu.io.enaByte := false.B

  alu.io.op := decReg.op
  alu.io.enaByte := decReg.isLoadIndB
  alu.io.off := RegNext(effAddr(1, 0))
  alu.io.din := Mux(decReg.isLoadInd || decReg.isRegOpd, dataRead, opdReg)

  switch(stateReg) {
    is (sFeDec) {
      decReg := decout
      opdReg := operand
      // move to decout when load high part is in decode
      // opdReg := decout.operand
      stateReg := decout.next
    }

    is (sAlu) {
      pcReg := pcNext
      alu.io.enaMask := decReg.enaMask
      alu.io.din := dataRead
      stateReg := sFeDec
    }

    is (sAluI) {
      pcReg := pcNext
      alu.io.enaMask := decReg.enaMask
      alu.io.din := opdReg
      stateReg := sFeDec
    }

    is (exe) {
      pcReg := pcNext
      alu.io.enaMask := decReg.enaMask
      when (decReg.isLoadAddr) {
        addrReg := accu
        alu.io.enaMask := 0.U
      }
      when (decReg.isLoadInd) {
        // nothing to be done here
      }
      when (decReg.isStore || decReg.isStoreInd) {
        val writeAddress = Mux(decReg.isStoreInd, effAddrWord, instrLowReg)
        dataMem.write(writeAddress, accu)
        alu.io.enaMask := 0.U
      }
      stateReg := sFeDec
    }

  }

  exit := RegNext(decReg.exit)
}

