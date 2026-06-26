package com.example.financeiro.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.financeiro.data.local.database.AppDatabase
import com.example.financeiro.data.local.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE transacoes ADD COLUMN recorrencia_id TEXT")
            database.execSQL("ALTER TABLE transacoes ADD COLUMN recorrencia_indice INTEGER")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transacoes_recorrencia_id ON transacoes(recorrencia_id)")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "saldo_tenho.db"
        ).addMigrations(MIGRATION_1_2).build()

    @Provides
    fun provideContaDao(db: AppDatabase): ContaDao = db.contaDao()

    @Provides
    fun provideCartaoDao(db: AppDatabase): CartaoDao = db.cartaoDao()

    @Provides
    fun provideCategoriaDao(db: AppDatabase): CategoriaDao = db.categoriaDao()

    @Provides
    fun provideSubcategoriaDao(db: AppDatabase): SubcategoriaDao = db.subcategoriaDao()

    @Provides
    fun provideTransacaoDao(db: AppDatabase): TransacaoDao = db.transacaoDao()

    @Provides
    fun provideParcelamentoDao(db: AppDatabase): ParcelamentoDao = db.parcelamentoDao()
}
