package com.example.whereami.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.example.whereami.R
import com.example.whereami.common.enums.MapError
import com.example.whereami.common.enums.Marker
import com.example.whereami.common.extensions.hasPermissions
import com.example.whereami.common.extensions.toLatLng
import com.example.whereami.common.extensions.toScaleAspectRation
import com.example.whereami.common.extensions.toast
import com.example.whereami.config.*
import com.example.whereami.databinding.ActivityMapsBinding
import com.example.whereami.viewmodels.MainViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback
import com.google.android.gms.maps.model.*
import dagger.hilt.android.AndroidEntryPoint
import java.io.*
import java.util.*


private const val TAG = "MapsActivity"

@AndroidEntryPoint
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMapsBinding

    private lateinit var currentPhotoUri: Uri

    private fun onMapInitialized(map: GoogleMap) {}
    private fun onMapError(error: MapError) {}

    private var initialLocationSet: Boolean = false

    private val shouldUpdateLocation = true
    private val shouldShowUser = true
    private val locationUpdateInterval = 500L
    private var lastLocationUpdateTime = 0L

    private lateinit var locationProvider: FusedLocationProviderClient
    private lateinit var map: GoogleMap

    private var currentLocation: Location? = null
        set(value) {
            value?.let {
                field = it
                onUserLocationUpdated(it)
            }
        }

    private var isInitialSetup = true
    private var locationIsUpdating = false

    private var currentMarker: Marker? = null

    private val mObserverCenterLocation = Observer<Boolean> { isCentered ->
        if (isCentered &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED
        ) {
            currentLocation?.let { location ->
                centerLocation(location.toLatLng())
            }
        }
    }

    private val mSaveMapImageLiveData = Observer<Boolean> { isSaved ->
        if (isSaved) {
            checkForCameraPermissionAndTakePhoto()
        }
    }

    private fun checkForCameraPermissionAndTakePhoto() {
        if (!this@MapsActivity.hasPermissions(CAMERA_PERMISSION)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(CAMERA_PERMISSION, CAMERA_CODE)
            }
        } else {
           dispatchTakePictureIntent()
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val photoUri = getImageUri()
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "dispatchTakePictureIntent $e")
        }
    }

    private fun getImageUri() : Uri? {
        val filename = "${currentLocation}.jpg"

       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let {
                    currentPhotoUri = it
                }
                return imageUri
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
            val image = File(imagesDir, filename)
            val photoURI = FileProvider.getUriForFile(this, this.applicationContext.packageName.toString() + ".provider", image)
            currentPhotoUri = photoURI
            return photoURI
        }
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView<ActivityMapsBinding>(this, R.layout.activity_maps)
                .apply {
                    viewModel = mainViewModel
                    lifecycleOwner = this@MapsActivity
                    executePendingBindings()
                }

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<AppCompatImageView>(R.id.actionButtonCenter).bringToFront()

        locationProvider = LocationServices.getFusedLocationProviderClient(this)

        mainViewModel.centerLocationLiveData.observe(this, this.mObserverCenterLocation)
        mainViewModel.saveMapImageLiveData.observe(this, this.mSaveMapImageLiveData)
        mainViewModel.markerLD.observe(this, { marker ->
            currentMarker = marker
            currentLocation?.let {
                addMarkerToLocation(latLng = it.toLatLng(), resource = marker.resource)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (!locationIsUpdating && !isInitialSetup) checkForLocationPermissionAndSetLocation()
    }

    override fun onPause() {
        super.onPause()
        if (locationIsUpdating) stopLocationListener()
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.let {
            this.map = it
            onMapInitialized(it)
            checkForLocationPermissionAndSetLocation()
        } ?: onMapError(MapError.MAP_LOAD)

        if (!initialLocationSet)
            setInitialLocation()

        Handler().postDelayed({
            if (!initialLocationSet)
                setInitialLocation()
        }, 6000)
    }

    private fun checkForLocationPermissionAndSetLocation() {
        if (!this@MapsActivity.hasPermissions(arrayOf(LOCATION_PERMISSION))) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(LOCATION_PERMISSION), LOCATION_CODE)
            }
        } else {
            setInitialLocation()
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    toast(R.string.accept_permissions)
                    onMapError(MapError.LOCATION)
                } else {
                    setInitialLocation()
                }
            }

            WRITE_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    toast(R.string.accept_permissions)
                    mainViewModel.currentScreenshotMLD.value = null
                } else {
                    mainViewModel.currentScreenshotMLD.value?.let {
                        saveMediaToStorage(it)
                    }
                }
            }

            CAMERA_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    toast(R.string.accept_permissions)
                } else {
                    dispatchTakePictureIntent()
                }
            }
        }
    }

    private fun setInitialLocation() {
        try {
            if (shouldShowUser) {
                map.isMyLocationEnabled = true
            }
            map.uiSettings.isMyLocationButtonEnabled = false
            map.uiSettings.isZoomGesturesEnabled = true
            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isMapToolbarEnabled = false
            getDeviceLocation()
        } catch (e: SecurityException) {
            onMapError(MapError.PERMISSIONS)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        val locationTask = locationProvider.lastLocation
        locationTask.addOnSuccessListener { location ->
            location?.let {
                currentLocation = location
                if (shouldShowUser) {
                    initialSetMapToLocation(location.toLatLng())
                }
            }
        }
        startLocationListener()
    }

    private fun startLocationListener() {
        if (shouldUpdateLocation) {
            val request = LocationRequest().apply {
                this.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                this.interval = locationUpdateInterval
            }

            locationIsUpdating = try {
                locationProvider.requestLocationUpdates(request, locationCallback, null)
                if (isInitialSetup) isInitialSetup = false
                true
            } catch (e: SecurityException) {
                false
            }
        }
    }

    private fun stopLocationListener() {
        if (shouldUpdateLocation) {
            locationIsUpdating = false
            locationProvider.removeLocationUpdates(locationCallback)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            result?.let {
                if (it.lastLocation.accuracy > 20
                        || currentLocation == it.lastLocation
                        || System.currentTimeMillis() - lastLocationUpdateTime < 5000
                )
                    return

                currentLocation = it.lastLocation
                lastLocationUpdateTime = System.currentTimeMillis()
            }
        }
    }

    private fun initialSetMapToLocation(latLng: LatLng) {
        initialLocationSet = true
        addMarkerToLocation(latLng = latLng, initialLocationSet = true)
    }

    private fun addMarkerToLocation(latLng: LatLng, initialLocationSet: Boolean? = null, resource: Int? = null) {
        map.clear()
        val options = MarkerOptions().position(latLng).title(getString(R.string.marker_location_title)).snippet(getString(R.string.marker_location_description))
        if (resource != null) {
            val icon = bitmapDescriptorFromVector(
                    this,
                    resource,
                    resources.getDimensionPixelSize(R.dimen.size24),
                    resources.getDimensionPixelSize(R.dimen.size24))
            options.icon(icon)
        }
        map.addMarker(options)
        initialLocationSet?.let {
            if (it) {
                centerLocation(latLng)
            }
        }
    }

    private fun centerLocation(latLng: LatLng) {
        val builder = LatLngBounds.Builder()
        builder.include(latLng)
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 20))
    }

    private fun bitmapDescriptorFromVector(
            context: Context,
            vectorResId: Int,
            desiredWidth: Int = 0,
            desiredHeight: Int = 0,
            desiredColor: Int? = null
    ): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        desiredColor?.let {
            vectorDrawable?.setTint(desiredColor)
        }
        vectorDrawable!!.setBounds(
                0,
                0,
                vectorDrawable.intrinsicWidth,
                vectorDrawable.intrinsicHeight
        )
        val bitmap =
                Bitmap.createBitmap(
                        vectorDrawable.intrinsicWidth,
                        vectorDrawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return if (desiredWidth > 0 && desiredHeight > 0) {
            BitmapDescriptorFactory.fromBitmap(
                    bitmap.toScaleAspectRation(
                            desiredWidth,
                            desiredHeight
                    )
            )
        } else {
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    private fun onUserLocationUpdated(location: Location) {
        findAddress(location)
        addMarkerToLocation(latLng = location.toLatLng(), resource = if (currentMarker != null) currentMarker?.resource else null)
    }

    private fun findAddress(location: Location) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val address =
                    geocoder.getFromLocation(location.latitude, location.longitude, 1).firstOrNull()
            address?.let {
                mainViewModel.updateAddress(address = address)
            }
        } catch (e: Exception) {
            Log.e(TAG, "findAddress error occurred $e")
        }
    }

    private fun captureScreen() {
        val snapshotCallback = SnapshotReadyCallback { snapshot ->
            snapshot?.let { bitmap ->
                mainViewModel.currentScreenshotMLD.value = bitmap
                checkForWritingStoragePermissionAndSaveScreenshot(bitmap)
            }
        }
        map.snapshot(snapshotCallback)
    }

    private fun checkForWritingStoragePermissionAndSaveScreenshot(bitmap: Bitmap) {
        if (!this@MapsActivity.hasPermissions(arrayOf(WRITE_PERMISSION))) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(WRITE_PERMISSION), WRITE_CODE)
            }
        } else {
            saveMediaToStorage(bitmap)
        }
    }

    private fun saveMediaToStorage(bitmap: Bitmap) {
        val filename = "${currentLocation}.jpg"
        var fos: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let {
                    fos = resolver.openOutputStream(it)
                    showNotification(this, getString(R.string.app_name), getString(R.string.notification_body), it)
                }
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
            val photoURI = FileProvider.getUriForFile(this, this.applicationContext.packageName.toString() + ".provider", image)
            showNotification(this, getString(R.string.app_name), getString(R.string.notification_body), photoURI)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            it.flush()
            it.close()
        }
        mainViewModel.currentScreenshotMLD.value = null
    }

    private fun showNotification(context: Context, title: String?, body: String?, imageId: Uri) {
        val imageIntent = openInGallery(imageId)

        val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                imageIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_NOTIFICATIONS)
                .setSmallIcon(R.drawable.ic_baseline_flag_24)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        NotificationManagerCompat.from(context).notify(0, notification)
    }

    private fun openInGallery(imageUri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW, imageUri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    override fun onDestroy() {
        super.onDestroy()
        mainViewModel.centerLocationLiveData.removeObserver(this.mObserverCenterLocation)
        mainViewModel.saveMapImageLiveData.removeObserver(this.mSaveMapImageLiveData)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            showNotification(this, getString(R.string.app_name), getString(R.string.notification_body), currentPhotoUri)
        }
    }
}