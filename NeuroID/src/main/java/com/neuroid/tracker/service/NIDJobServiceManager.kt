package com.neuroid.tracker.service

import android.app.Application
import com.neuroid.tracker.callbacks.NIDSensorHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

object NIDJobServiceManager {

    private var jobCaptureEvents: Job? = null
    var isSendEventsNowEnabled = true

    @Volatile
    var userActive = true
    private var clientKey = ""
    private var application: Application? = null
    private var endpoint: String = ""

    @Synchronized
    fun startJob(
        application: Application,
        clientKey: String,
        endpoint: String
    ) {
        this.clientKey = clientKey
        this.endpoint = endpoint
        this.application = application
        jobCaptureEvents = createJobServer()
        NIDSensorHelper.initSensorHelper(application)
    }

    @Synchronized
    fun restart() {
        NIDSensorHelper.restartSensors()
        jobCaptureEvents?.cancel()
        jobCaptureEvents = createJobServer()
    }

    private fun createJobServer(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (userActive && isActive) {
                delay(5000L)
                sendEventsNow()
            }
        }
    }

    suspend fun sendEventsNow() {
        if (isSendEventsNowEnabled) {
            application?.let {
                val response = NIDServiceTracker.sendEventToServer(clientKey, endpoint, it)
                if (response.second) {
                    userActive = false
                }
            } ?: run {
                userActive = false
            }
        }
    }

    @Synchronized
    fun stopJob() {
        NIDSensorHelper.stopSensors()
        jobCaptureEvents?.cancel()
        jobCaptureEvents = null
    }

    @Synchronized
    fun isStopped(): Boolean {
        return jobCaptureEvents?.isActive != true
    }
}