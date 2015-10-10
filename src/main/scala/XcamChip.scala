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
        val ddr = new DDRIO
        val imem = new BRAMIO
        val dmem = new BRAMIO
        val imemaddr = UInt(OUTPUT, 32)
        val imemdata = UInt(OUTPUT, 32)
        val ihtrans = UInt(OUTPUT, 8)
        val ihsize = UInt(OUTPUT, 12)
        val ihwrite = UInt(OUTPUT, 4)
        val ihready = UInt(OUTPUT, 4)
        val dmemaddr = UInt(OUTPUT, 32)
        val dmemrdata = UInt(OUTPUT, 32)
        val dmemwdata = UInt(OUTPUT, 32)
        val dhtrans = UInt(OUTPUT, 8)
        val dhsize = UInt(OUTPUT, 12)
        val dhwrite = UInt(OUTPUT, 4)
        val dhready = UInt(OUTPUT, 4)
        val istate = UInt(OUTPUT, 4)
        val dstate = UInt(OUTPUT, 4)
    }

    val core = params(BuildZscale)(io.reset)

    val imem_afn = (addr: UInt) => addr(31, 14) === UInt(0)
    val ibus = Module(new HASTIBus(Seq(imem_afn)))

    ibus.io.master <> core.io.imem

    //val imem = Module(new HASTISRAM(params(IBRAMCapacity)/4))
    val imem = Module(new HASTIBRAM)
    imem.io.in <> ibus.io.slaves(0)
    imem.io.out <> io.imem

    val dmem_afn = (addr: UInt) => addr(31,14) === UInt(0)
    val pbus_afn = (addr: UInt) => addr(31,14) === UInt(1)
    val led_afn  = (addr: UInt) => addr(31,14) === UInt(1) && addr(13, 12) === UInt(0)
    val uart_afn = (addr: UInt) => addr(31,14) === UInt(2)
    val ddr_afn  = (addr: UInt) => addr(31,28) === UInt(1)

    val dbus = Module(new HASTIBus(Seq(dmem_afn, pbus_afn, uart_afn, ddr_afn)))
    dbus.io.master <> core.io.dmem

    //val dmem = Module(new HASTISRAM(params(DBRAMCapacity)/4))
    val dmem = Module(new HASTIBRAM)
    dmem.io.in <> dbus.io.slaves(0)
    dmem.io.out <> io.dmem

    val padapter = Module(new HASTItoPOCIBridge)
    val pbus = Module(new POCIBus(Seq(led_afn)))
    padapter.io.in <> dbus.io.slaves(1)
    pbus.io.master <> padapter.io.out

    val gpio = Module(new POCIGPIO(8))
    pbus.io.slaves(0) <> gpio.io.bus
    gpio.io.pin.i <> io.switches
    gpio.io.pin.o <> io.leds

    val uadapter = Module(new HASTISlaveToMaster)
    uadapter.io.in <> dbus.io.slaves(2)
    uadapter.io.out <> io.ubus

    val ddr = Module(new HASTIDDR)
    ddr.io.in <> dbus.io.slaves(3)
    ddr.io.out <> io.ddr

    // debug
    io.imemaddr := core.io.imem.haddr
    io.imemdata := core.io.imem.hrdata
    io.ihtrans := Cat(UInt(0, width=3), core.io.imem.htrans(1), UInt(0, width=3), core.io.imem.htrans(0))
    io.ihsize := Cat(UInt(0, width=3), core.io.imem.hsize(2), UInt(0, width=3), core.io.imem.hsize(1), UInt(0, width=3), core.io.imem.hsize(0))
    io.ihwrite := Cat(UInt(0, width=3), core.io.imem.hwrite)
    io.ihready := Cat(UInt(0, width=3), core.io.imem.hready)
    io.dmemaddr := core.io.dmem.haddr
    io.dmemwdata := core.io.dmem.hwdata
    io.dmemrdata := core.io.dmem.hrdata
    io.dhtrans := Cat(UInt(0, width=3), core.io.dmem.htrans(1), UInt(0, width=3), core.io.dmem.htrans(0))
    io.dhsize := Cat(UInt(0, width=3), core.io.dmem.hsize(2), UInt(0, width=3), core.io.dmem.hsize(1), UInt(0, width=3), core.io.dmem.hsize(0))
    io.dhwrite := Cat(UInt(0, width=3), core.io.dmem.hwrite)
    io.dhready := Cat(UInt(0, width=3), core.io.dmem.hready)
    //io.dstate := Cat(UInt(0, width=1), dmem.io.bypass, dmem.io.ren, dmem.io.state)
    io.dstate := UInt(0, width=4)
    //io.istate := Cat(UInt(0, width=2), imem.io.ren, imem.io.state)
    io.istate := UInt(0, width=4)
}
