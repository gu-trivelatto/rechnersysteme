package rs.rs2.cgra.app

import de.tu_darmstadt.rs.jcommander.JCommanderRunner

fun main(args: Array<String>) {

    JCommanderRunner.create()
        .setProgramName("rs2Runner")
        .addCommand(Rs2SimulateCommand())
        .addCommand(Rs2SpeedupCommand())
        .run(args)
}