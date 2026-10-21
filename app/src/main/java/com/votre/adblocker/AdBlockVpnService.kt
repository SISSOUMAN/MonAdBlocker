package com.votre.adblocker

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import kotlin.concurrent.thread

class AdBlockVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    // Liste des domaines bloqués en mémoire (exemples de pub / trackers)
    private val blockedDomains = mutableSetOf(
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
            // Configuration de l'interface VPN
            val builder = Builder()
                .addAddress("10.0.0.2", 24)
                .addDnsServer("8.8.8.8") // Serveur DNS de secours
                .addRoute("8.8.8.8", 32)  // Redirige les requêtes DNS vers notre VPN

            vpnInterface = builder.setSession("MonAdBlocker").establish()

            // Thread de traitement des paquets réseau
            thread {
                processVpnTraffic()
            }

            Log.d("AdBlockVpnService", "VPN démarré et filtrage DNS actif.")
        } catch (e: Exception) {
            Log.e("AdBlockVpnService", "Erreur lors du démarrage du VPN", e)
        }
    }

    private fun processVpnTraffic() {
        val descriptor = vpnInterface?.fileDescriptor ?: return
        val inputStream = FileInputStream(descriptor)
        val outputStream = FileOutputStream(descriptor)
        val buffer = ByteArray(32767)

        while (isRunning) {
            val length = inputStream.read(buffer)
            if (length > 0) {
                // Analyse minimale du paquet IP / DNS
                val isDnsPacket = checkAndFilterDns(buffer, length)
                if (!isDnsPacket) {
                    // Transmettre les paquets légitimes
                    outputStream.write(buffer, 0, length)
                }
            }
        }
    }

    private fun checkAndFilterDns(packet: ByteArray, length: Int): Boolean {
        // Détection rapide des paquets UDP sur le port 53 (DNS)
        if (length < 28) return false // Taille minimale en-tête IP + UDP

        val isUdp = packet[9].toInt() == 17
        if (!isUdp) return false

        val destinationPort = ((packet[22].toInt() and 0xFF) shl 8) or (packet[23].toInt() and 0xFF)
        if (destinationPort != 53) return false

        // Extraction simple du nom de domaine dans la requête DNS
        val domain = extractDomainFromDnsPacket(packet, length)
        if (domain != null && isDomainBlocked(domain)) {
            Log.i("AdBlockVpnService", "Domaine bloqué : $domain")
            return true // Paquet ignoré/sinkholé
        }

        return false
    }

    private fun isDomainBlocked(domain: String): Boolean {
        return blockedDomains.any { blocked ->
            domain.equals(blocked, ignoreCase = true) || domain.endsWith(".$blocked", ignoreCase = true)
        }
    }

    private fun extractDomainFromDnsPacket(packet: ByteArray, length: Int): String? {
        return try {
            var position = 40 // Fin de l'en-tête IP (20B) + UDP (8B) + Header DNS (12B)
            val domainBuilder = StringBuilder()

            while (position < length) {
                val labelLength = packet[position].toInt() and 0xFF
                if (labelLength == 0) break
                
                if (domainBuilder.isNotEmpty()) {
                    domainBuilder.append(".")
                }

                for (i in 0 until labelLength) {
                    position++
                    if (position < length) {
                        domainBuilder.append(packet[position].toInt().toChar())
                    }
                }
                position++
            }
            domainBuilder.toString()
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        vpnInterface?.close()
        Log.d("AdBlockVpnService", "VPN arrêté.")
    }
}
