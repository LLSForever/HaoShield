package com.haoshield.domain.di

import javax.inject.Qualifier

/**
 * The scope that outlives any one screen. Lives here rather than in the app module so that code
 * shared with a second platform can ask for it without depending on Android's wiring.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
