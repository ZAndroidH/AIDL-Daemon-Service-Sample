package com.dev

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import java.util.*

abstract class HeartBeatService : Service() {
    private val timer = Timer()

    private val timerTask = object : TimerTask() {
        override fun run() {
            onHeartBeat()
        }
    }

    private val aidl = object : ServiceManager.Stub() {
        override fun startService() {
        }

        override fun stopService() {
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, iBinder: IBinder?) {
            try {
                iBinder?.linkToDeath({
                    aidl.startService()
                    startBindService()
                }, 1)
            } catch (ex: RemoteException) {
                ex.printStackTrace()
            }

        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            try {
                aidl.stopService()
            } catch (ex: RemoteException) {
                ex.printStackTrace()
            }
        }

        override fun onBindingDied(name: ComponentName?) {
            onServiceDisconnected(name)
        }
    }

    override fun onCreate() {
        super.onCreate()
        onStartService()
        startBindService()
        if (getHeartBeatMillis() > 0) {
            timer.schedule(timerTask, getDelayExecutedMillis(), getHeartBeatMillis())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return aidl
    }

    override fun onDestroy() {
        super.onDestroy()
        onStopService()
        unbindService(serviceConnection)

        // TODO: DaemonHolder.restartService(getApplicationContext(), DaemonHolder.mService)

        timer.apply {
            cancel()
            purge()
        }
        timerTask.cancel()
    }

    abstract fun onStartService()

    abstract fun onStopService()

    abstract fun getDelayExecutedMillis(): Long

    abstract fun getHeartBeatMillis(): Long

    abstract fun onHeartBeat()

    private fun startBindService() {
        val intent = Intent(this, DaemonService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, BIND_IMPORTANT)
    }
}