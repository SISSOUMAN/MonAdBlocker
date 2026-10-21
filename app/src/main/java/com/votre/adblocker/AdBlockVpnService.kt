package com.votre.adblocker

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread

class AdBlockVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    @Volatile private var isRunning = false

    // Liste basique de domaines à filtrer
    private val blockedDomains = setOf(
        "pagead2.googlesyndication.com",
        "adservice.google.com",
        "doubleclick.net",
        "google-analytics.com",
        "ads.facebook.com"
    )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            setupVpn()
        }
        return START_STICKY
    }

    private fun setupVpn() {
        try {
            val builder = Builder()
                .addAddress("10.0.0.2", 32)
                .addDnsServer("8.8.8.8")
                .addDnsServer("1.1.1.1")
                // Exclure les applications du système si besoin ou laisser passer tout le trafic non-DNS
                .addRoute("0.0.0.0", 0)

            vpnInterface = builder.setSession("MonAdBlocker").establish()

            thread {
                processTraffic()
            }

            Log.d("AdBlockVpn", "VPN démarré avec succès")
        } catch (e: Exception) {
            Log.e("AdBlockVpn", "Erreur d'initialisation du VPN", e)
        }
    }

    private fun processTraffic() {
        val descriptor = vpnInterface?.fileDescriptor ?: return
        val inputStream = FileInputStream(descriptor)
        val outputStream = FileOutputStream(descriptor)
        val buffer = ByteArray(32767)

        while (isRunning) {
            try {
                val length = inputStream.read(buffer)
                if (length > 0) {
                    // Transmettre directement les paquets pour ne pas bloquer le réseau
                    outputStream.write(buffer, 0, length)
                }
            } catch (e: Exception) {
                if (!isRunning) break
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e("AdBlockVpn", "Erreur lors de la fermeture", e)
        }
    }
}
