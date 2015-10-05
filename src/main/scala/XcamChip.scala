// See LICENSE for license details.

package rocketchip

import Chisel._
import junctions._
import uncore._
import rocket._
import zscale._

//case object UseZscale extends Field[Boolean]
//case object BuildZscale extends Field[(Bool) => Zscale]
case object IBRAMCapacity extends Field[Int]
case object DBRAMCapacity extends Field[Int]

class XcamTop extends Module {
    val io = new Bundle {
        val reset = Bool(INPUT)
        val leds = Bits(OUTPUT, 8)
        val switches = Bits(INPUT, 8)
        val ubus = new HASTIMasterIO
    }

    val core = params(BuildZscale)(io.reset)

    val imem_afn = (addr: UInt) => addr(31, 14) === UInt(0)
    val ibus = Module(new HASTIBus(Seq(imem_afn)))

    ibus.io.master <> core.io.imem

    val imem = Module(new HASTISRAM(params(IBRAMCapacity)/4))

    imem.io <> ibus.io.slaves(0)

    val dmem_afn = (addr: UInt) => addr(31,14) === UInt(0)
    val pbus_afn = (addr: UInt) => addr(31,14) === UInt(1)
    val uart_afn = (addr: UInt) => addr(31,14) === UInt(2)
    val led_afn =  (addr: UInt) => addr(31,14) === UInt(1) && addr(13, 12) === UInt(0)
    val dbus = Module(new HASTIBus(Seq(dmem_afn, pbus_afn, uart_afn)))
    val padapter = Module(new HASTItoPOCIBridge)
    val pbus = Module(new POCIBus(Seq(led_afn)))

    dbus.io.master <> core.io.dmem

    padapter.io.in <> dbus.io.slaves(1)
    pbus.io.master <> padapter.io.out

    val gpio = Module(new POCIGPIO(8))
    pbus.io.slaves(0) <> gpio.io.bus
    gpio.io.pin.i <> io.switches
    gpio.io.pin.o <> io.leds

    val dmem = Module(new HASTISRAM(params(DBRAMCapacity)/4))
    dmem.io <> dbus.io.slaves(0)

    val uadapter = Module(new HASTISlaveToMaster)
    uadapter.io.in <> dbus.io.slaves(2)
    uadapter.io.out <> io.ubus
}
