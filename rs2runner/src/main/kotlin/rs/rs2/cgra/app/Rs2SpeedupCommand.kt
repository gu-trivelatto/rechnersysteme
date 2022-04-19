package rs.rs2.cgra.app

import com.beust.jcommander.Parameters

@Parameters(commandNames = ["speedup"], commandDescription = "run 2 times, once without acceleration, once with. Report speedup")
class Rs2SpeedupCommand: BaseRunnerCommand(), Runnable {

    override fun run() {
        TODO("not implemented")
    }

}