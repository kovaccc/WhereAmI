package com.example.whereami.viewmodels

import android.graphics.Bitmap
import android.location.Address
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.whereami.model.PlaceDetail
import com.example.whereami.R
import com.example.whereami.common.enums.Marker
import com.example.whereami.sounds.AudioPlayer
import com.example.whereami.util.SingleEventLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
        private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _addressMLD = MutableLiveData<PlaceDetail>()
    val addressLD: LiveData<PlaceDetail>
        get() = _addressMLD

    private val _markerMLD = MutableLiveData<Marker>()
    val markerLD: LiveData<Marker>
        get() = _markerMLD

    val centerLocationLiveData = SingleEventLiveData<Boolean>()
    val saveMapImageLiveData = SingleEventLiveData<Boolean>()
    val currentScreenshotMLD = MutableLiveData<Bitmap>()

    private val markers: MutableList<Marker> = mutableListOf(Marker.FITNESS, Marker.FLAG)

    fun updateAddress(address: Address) {
        address.apply {
            _addressMLD.value = PlaceDetail(latitude = latitude, longitude = longitude,
                    address = getAddressLine(0).substringBefore(","),
                    city = locality, country = countryName)
        }
    }

    private fun changeMarkerIcon() {
        _markerMLD.let {
            it.value = markers.firstOrNull { marker -> marker != it.value }
        }
        audioPlayer.playAddMarkerSound()
    }

    fun onViewClicked(v: View) {
        when (v.id) {
            R.id.actionButtonCenter -> centerLocationLiveData.value = true
            R.id.btnChangeMarker -> changeMarkerIcon()
            R.id.btnSave -> saveMapImageLiveData.value = true
        }
    }
}