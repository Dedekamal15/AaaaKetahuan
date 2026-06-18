package com.example.aaaaketahuan.di

import android.content.Context
import com.example.aaaaketahuan.data.repository.TransaksiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTransaksiRepository(
        @ApplicationContext context: Context
    ): TransaksiRepository {
        return TransaksiRepository(context)
    }
}
