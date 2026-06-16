package com.example.financeiro.di

import com.example.financeiro.data.local.repository.*
import com.example.financeiro.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindContaRepository(impl: ContaRepositoryImpl): ContaRepository

    @Binds @Singleton
    abstract fun bindCartaoRepository(impl: CartaoRepositoryImpl): CartaoRepository

    @Binds @Singleton
    abstract fun bindCategoriaRepository(impl: CategoriaRepositoryImpl): CategoriaRepository

    @Binds @Singleton
    abstract fun bindTransacaoRepository(impl: TransacaoRepositoryImpl): TransacaoRepository

    @Binds @Singleton
    abstract fun bindParcelamentoRepository(impl: ParcelamentoRepositoryImpl): ParcelamentoRepository
}