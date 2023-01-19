package com.guru.routeksp

import com.guru.annonation.Destination

@Destination(name = "android_sample")
data class Sample(
    val name: String,
    val age: Int,
    val address: String,
    val isAndroid: Boolean
)
