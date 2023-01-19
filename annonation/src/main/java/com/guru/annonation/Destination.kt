// ktlint-disable
package com.guru.annonation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Destination(
    val name: String
)