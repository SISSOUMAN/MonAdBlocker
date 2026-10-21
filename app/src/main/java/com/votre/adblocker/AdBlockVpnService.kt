package com.votre.adblocker

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class AdBlockVpnService : VpnService(), Runnable {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    @Volatile private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            vpnThread = Thread(this, "AdBlockVpnThread").apply { start() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        vpnThread?.interrupt()
        vpnInterface?.close()
        super.onDestroy()
    }

    override fun run() {
        try {
            val builder = Builder()
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
                .setSession("MonAdBlocker")

            vpnInterface = builder.establish() ?: return

            val inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
            val outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)
            val buffer = ByteBuffer.allocate(32767)

            while (isRunning) {
                val length = inputStream.read(buffer.array())
                if (length > 0) {
                    // Le traitement des paquets DNS (parsing et filtrage) s'effectue ici
                    buffer.clear()
                }
                Thread.sleep(10)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
