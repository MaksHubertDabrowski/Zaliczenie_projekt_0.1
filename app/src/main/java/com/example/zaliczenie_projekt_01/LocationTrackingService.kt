package com.example.zaliczenie_projekt_01

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.zaliczenie_projekt_01.data.db.AppDatabase
import com.example.zaliczenie_projekt_01.data.db.CatchDao
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class LocationTrackingService : Service() {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private lateinit var catchDao: CatchDao

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        const val ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE"
        const val ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE"
        private const val NOTIFICATION_CHANNEL_ID = "LocationTrackingChannel"
        private const val NOTIFICATION_ID = 1
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 60000 // 1 minute
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS: Long = UPDATE_INTERVAL_IN_MILLISECONDS / 2
        private const val MIN_DISTANCE_THRESHOLD_METERS = 500 // 500 meters
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        catchDao = AppDatabase.getDatabase(applicationContext).catchDao()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND_SERVICE -> {
                startForegroundService()
                requestLocationUpdates()
            }
            ACTION_STOP_FOREGROUND_SERVICE -> {
                stopLocationUpdates()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Śledzenie lokalizacji",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Śledzenie lokalizacji")
            .setContentText("Aplikacja śledzi Twoją lokalizację w tle.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use an appropriate icon
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissions are not granted, cannot request location updates. This should be handled in the activity.
            return
        }

        val locationRequest = LocationRequest.create().apply {
            interval = UPDATE_INTERVAL_IN_MILLISECONDS
            fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    serviceScope.launch {
                        val catches = catchDao.getAllCatches()
                        for (catch in catches) {
                            val catchLocation = Location("catch").apply {
                                latitude = catch.latitude
                                longitude = catch.longitude
                            }
                            val distance = location.distanceTo(catchLocation)

                            if (distance < MIN_DISTANCE_THRESHOLD_METERS) {
                                showProximityNotification(catch.species ?: "nieznanej miejscówki", distance)
                            }
                        }
                    }
                }
            }
        }
        fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationClient?.removeLocationUpdates(locationCallback!!)
    }

    private fun showProximityNotification(species: String, distance: Float) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val distanceInMeters = distance.roundToInt()

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Powiadomienie o bliskości!")
            .setContentText("Jesteś blisko miejscówki $species! Odległość: ${distanceInMeters}m.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use an appropriate icon
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + species.hashCode(), notification) // Unique ID for each catch
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceJob.cancel()
    }
}
