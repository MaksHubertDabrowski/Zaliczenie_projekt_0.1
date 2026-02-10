package com.example.zaliczenie_projekt_01

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast

class WifiConnectivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                // Wi-Fi is connected
                Log.d("WifiConnectivityReceiver", "Wi-Fi jest dostępne. Rozpoczęto synchronizację.")
                Toast.makeText(context, "Wi-Fi jest dostępne. Rozpoczęto synchronizację.", Toast.LENGTH_SHORT).show()
                // TODO: Implement actual synchronization logic here
            } else {
                // Wi-Fi is not connected or not the active network
                Log.d("WifiConnectivityReceiver", "Brak Wi-Fi. Synchronizacja wstrzymana.")
                Toast.makeText(context, "Brak Wi-Fi. Synchronizacja wstrzymana.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}