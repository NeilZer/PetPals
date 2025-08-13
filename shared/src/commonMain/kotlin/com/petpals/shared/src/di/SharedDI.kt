package com.petpals.shared.src.di

import com.petpals.shared.src.data.PostsRepository
import com.petpals.shared.src.data.providePostsRepository
import com.petpals.shared.src.device.LocationProvider
import com.petpals.shared.src.device.provideLocationProvider

object SharedDI {
    val posts: PostsRepository by lazy { providePostsRepository() }
    val location: LocationProvider by lazy { provideLocationProvider() }
}
