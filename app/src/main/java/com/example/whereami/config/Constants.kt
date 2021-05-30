package com.example.whereami.config

import android.Manifest

const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
const val LOCATION_CODE = 9004

const val WRITE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
const val WRITE_CODE = 9005


val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
const val CAMERA_CODE = 9002
const val REQUEST_IMAGE_CAPTURE = 1

const val CHANNEL_ID_NOTIFICATIONS = "98542"
const val CHANNEL_NAME = "WhereAmI"