package com.example.zaliczenie_projekt_01

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.fragment.findNavController
import com.example.zaliczenie_projekt_01.data.db.AppDatabase
import com.example.zaliczenie_projekt_01.data.db.Catch
import com.example.zaliczenie_projekt_01.data.db.CatchDao
import com.example.zaliczenie_projekt_01.databinding.DialogAddCatchBinding
import com.example.zaliczenie_projekt_01.databinding.FragmentFirstBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class FirstFragment : Fragment(), MapListener, SensorEventListener {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private lateinit var map: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var catchDao: CatchDao

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val CAMERA_PERMISSION_REQUEST_CODE = 1002
    private val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1003

    private var currentPhotoPath: String? = null

    // Sensor variables
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var currentAzimuth = 0f
    private var targetMarker: GeoPoint? = null // The marker we want to point to

    private val takePictureLauncher: ActivityResultLauncher<Uri> = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                val currentLocation = myLocationOverlay.myLocation
                if (currentLocation != null) {
                    showAddCatchDialog(currentLocation.latitude, currentLocation.longitude, path)
                } else {
                    Toast.makeText(requireContext(), "Nie można uzyskać aktualnej lokalizacji.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Nie zrobiono zdjęcia.", Toast.LENGTH_SHORT).show()
            currentPhotoPath = null
            File(currentPhotoPath ?: "").delete() // Delete temp file if photo wasn't saved
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()))

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        catchDao = AppDatabase.getDatabase(requireContext()).catchDao()

        map = MapView(requireContext())
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
        map.addMapListener(this)

        binding.mapContainer.addView(map)

        requestLocationPermissions()

        // Obsługa długiego kliknięcia na mapie
        map.overlays.add(object : Overlay() {
            override fun onLongPress(event: android.view.MotionEvent?, mapView: MapView?): Boolean {
                val geoPoint = mapView?.projection?.fromPixels(event!!.x.toInt(), event.y.toInt()) as GeoPoint
                showAddCatchDialog(geoPoint.latitude, geoPoint.longitude, null)
                return true
            }
        })

        loadCatchesFromDatabase()

        binding.buttonViewCatches.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_CatchListFragment)
        }

        binding.buttonAddCatchWithPhoto.setOnClickListener {
            requestCameraPermissionAndTakePicture()
        }

        binding.buttonStartService.setOnClickListener {
            startLocationTrackingService()
        }

        binding.buttonStopService.setOnClickListener {
            stopLocationTrackingService()
        }

        // Initialize sensors
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometer == null || magnetometer == null) {
            Toast.makeText(requireContext(), "Brak czujników kompasu w urządzeniu.", Toast.LENGTH_LONG).show()
            binding.imageViewCompassArrow.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        if (::myLocationOverlay.isInitialized) {
            myLocationOverlay.onResume()
        }
        // Register sensor listeners
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager?.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        if (::myLocationOverlay.isInitialized) {
            myLocationOverlay.onPause()
        }
        // Unregister sensor listeners
        sensorManager?.unregisterListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startLocationTrackingService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
                )
                return
            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val serviceIntent = Intent(requireContext(), LocationTrackingService::class.java)
            serviceIntent.action = LocationTrackingService.ACTION_START_FOREGROUND_SERVICE
            ContextCompat.startForegroundService(requireContext(), serviceIntent)
            Toast.makeText(requireContext(), "Rozpoczęto śledzenie lokalizacji.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Wymagane uprawnienia do lokalizacji.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationTrackingService() {
        val serviceIntent = Intent(requireContext(), LocationTrackingService::class.java)
        serviceIntent.action = LocationTrackingService.ACTION_STOP_FOREGROUND_SERVICE
        requireContext().stopService(serviceIntent)
        Toast.makeText(requireContext(), "Zatrzymano śledzenie lokalizacji.", Toast.LENGTH_SHORT).show()
    }

    private fun requestCameraPermissionAndTakePicture() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            dispatchTakePictureIntent()
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Toast.makeText(requireContext(), "Błąd tworzenia pliku dla zdjęcia", Toast.LENGTH_SHORT).show()
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireActivity().applicationContext.packageName}.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureLauncher.launch(photoURI)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun showAddCatchDialog(latitude: Double, longitude: Double, imagePath: String?) {
        val dialogBinding = DialogAddCatchBinding.inflate(LayoutInflater.from(requireContext()))
        AlertDialog.Builder(requireContext())
            .setTitle("Dodaj nowe złapanie")
            .setView(dialogBinding.root)
            .setPositiveButton("Dodaj") { dialog, _ ->
                val species = dialogBinding.editTextSpecies.text.toString()
                val weightText = dialogBinding.editTextWeight.text.toString()
                val weight = weightText.toDoubleOrNull()

                val newCatch = Catch(
                    latitude = latitude,
                    longitude = longitude,
                    date = Date().time,
                    imagePath = imagePath,
                    species = species.ifEmpty { "Nieznany gatunek" },
                    weight = weight
                )

                CoroutineScope(Dispatchers.IO).launch {
                    catchDao.insertCatch(newCatch)
                    requireActivity().runOnUiThread {
                        addMarker(newCatch)
                        targetMarker = GeoPoint(newCatch.latitude, newCatch.longitude) // Set this as the target
                        binding.imageViewCompassArrow.visibility = View.VISIBLE // Show compass
                        Toast.makeText(requireContext(), "Dodano pinezkę", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Anuluj") { dialog, _ ->
                if (imagePath != null) {
                    currentPhotoPath = null
                    File(imagePath).delete()
                }
                dialog.cancel()
            }
            .show()
    }

    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            setupLocationOverlay()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupLocationOverlay()
                } else {
                    Toast.makeText(requireContext(), "Uprawnienia do lokalizacji są wymagane do wyświetlenia Twojej pozycji.", Toast.LENGTH_LONG).show()
                }
            }
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent()
                } else {
                    Toast.makeText(requireContext(), "Uprawnienia do aparatu są wymagane do robienia zdjęć.", Toast.LENGTH_LONG).show()
                }
            }
            BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationTrackingService() // Try starting service again now that permission is granted
                } else {
                    Toast.makeText(requireContext(), "Uprawnienia do lokalizacji w tle są wymagane do śledzenia lokalizacji.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupLocationOverlay() {
        val gpsLocationProvider = GpsMyLocationProvider(requireContext())
        myLocationOverlay = MyLocationNewOverlay(gpsLocationProvider, map)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()

        map.overlays.add(myLocationOverlay)
        map.controller.setZoom(15.0)

        myLocationOverlay.runOnFirstFix {
            requireActivity().runOnUiThread {
                map.controller.animateTo(myLocationOverlay.myLocation)
            }
        }
        map.invalidate()
    }

    private fun addMarker(catch: Catch) {
        val geoPoint = GeoPoint(catch.latitude, catch.longitude)
        val marker = Marker(map)
        marker.position = geoPoint
        marker.title = "Złapano: ${catch.species ?: "Nieznany gatunek"}"
        marker.snippet = "Data: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(catch.date))}\nWaga: ${catch.weight?.toString() ?: "N/A"}g"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(marker)
        map.invalidate()
    }

    private fun loadCatchesFromDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            val catches = catchDao.getAllCatches()
            requireActivity().runOnUiThread {
                for (catch in catches) {
                    addMarker(catch)
                }
                // Set the last loaded catch as the target if any exist
                catches.lastOrNull()?.let { lastCatch ->
                    targetMarker = GeoPoint(lastCatch.latitude, lastCatch.longitude)
                    binding.imageViewCompassArrow.visibility = View.VISIBLE
                }
            }
        }
    }

    // --- SensorEventListener implementation ---
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }

        updateOrientation()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used for this implementation
    }

    private fun updateOrientation() {
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            currentAzimuth = (azimuth + 360) % 360 // Normalize to 0-360

            updateCompassArrow()
        }
    }

    private fun updateCompassArrow() {
        val currentLocation = myLocationOverlay.myLocation

        if (targetMarker != null && currentLocation != null) {
            val bearingToTarget = calculateBearing(currentLocation, targetMarker!!)
            val rotation = (bearingToTarget - currentAzimuth + 360) % 360
            binding.imageViewCompassArrow.rotation = rotation
        } else {
            binding.imageViewCompassArrow.visibility = View.GONE
        }
    }

    private fun calculateBearing(startPoint: GeoPoint, endPoint: GeoPoint): Float {
        val lat1 = Math.toRadians(startPoint.latitude)
        val lon1 = Math.toRadians(startPoint.longitude)
        val lat2 = Math.toRadians(endPoint.latitude)
        val lon2 = Math.toRadians(endPoint.longitude)

        val deltaLon = lon2 - lon1

        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
        var bearing = Math.toDegrees(atan2(y, x)).toFloat()

        // Normalize bearing to 0-360
        bearing = (bearing + 360) % 360
        return bearing
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        if (::myLocationOverlay.isInitialized && myLocationOverlay.isFollowLocationEnabled) {
            myLocationOverlay.disableFollowLocation()
            Toast.makeText(requireContext(), "Śledzenie lokalizacji wyłączone", Toast.LENGTH_SHORT).show()
        }
        return true
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        return false
    }
}