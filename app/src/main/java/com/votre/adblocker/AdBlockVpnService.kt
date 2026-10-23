package com.votre.adblocker

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class AdBlockVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            stopVpn()
        } else {
            startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        val builder = Builder()
            .addAddress("10.0.0.2", 24)
            .addDnsServer("8.8.8.8") // Google DNS
            .addDnsServer("1.1.1.1") // Cloudflare DNS
            .addRoute("0.0.0.0", 0)  // Routage de tout le trafic IPv4

        try {
            // Empêche la boucle infinie en excluant l'application elle-même
            builder.addDisallowedApplication(packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        vpnInterface = builder.setSession("MonAdBlocker").establish()
    }

    private fun stopVpn() {
        vpnInterface?.close()
        vpnInterface = null
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
