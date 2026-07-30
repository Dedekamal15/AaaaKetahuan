package com.example.aaaaketahuan.di

import android.content.Context
import com.example.aaaaketahuan.data.remote.DriveSharingHelper
import com.example.aaaaketahuan.data.remote.GoogleSheetsHelper
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
    fun provideGoogleSheetsHelper(
        @ApplicationContext context: Context
    ): GoogleSheetsHelper {
        return GoogleSheetsHelper(context)
    }

    @Provides
    @Singleton
    fun provideTransaksiRepository(
        @ApplicationContext context: Context,
        sheetsHelper: GoogleSheetsHelper,
        driveSharingHelper: DriveSharingHelper
    ): TransaksiRepository {
        return TransaksiRepository(context, sheetsHelper, driveSharingHelper)
    }
}
