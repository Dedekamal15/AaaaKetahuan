package com.example.aaaaketahuan.forecast.di

import com.example.aaaaketahuan.data.repository.TransaksiRepository
import com.example.aaaaketahuan.forecast.ForecastRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module untuk dependency injection modul forecasting.
 *
 * Dipisahkan dari [AppModule] agar concern forecasting tidak tercampur
 * dengan module utama yang menangani data/sync/Google Sheets.
 *
 * Seluruh provider di sini scoped sebagai [Singleton] mengikuti pola
 * yang sudah ada di [com.example.aaaaketahuan.di.AppModule].
 */
@Module
@InstallIn(SingletonComponent::class)
object ForecastModule {

    /**
     * Menyediakan [ForecastRepository] singleton.
     *
     * [ForecastRepository] membutuhkan [TransaksiRepository] untuk mengakses
     * data transaksi — tidak perlu akses file JSON secara langsung karena
     * semua I/O sudah di-handle oleh [TransaksiRepository].
     */
    @Provides
    @Singleton
    fun provideForecastRepository(
        transaksiRepository: TransaksiRepository
    ): ForecastRepository {
        return ForecastRepository(transaksiRepository)
    }
}
