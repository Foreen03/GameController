package com.hanyi.gamecontroller.data.sensor

class SensorCoordinator(
    private val stepRepo: StepDetectorRepository,
    private val accelRepo: AccelerometerRepository
) {

    fun startAll(){
        stepRepo.start()
        accelRepo.start()
    }

    fun stopAll(){
        stepRepo.stop()
        accelRepo.stop()
    }

}