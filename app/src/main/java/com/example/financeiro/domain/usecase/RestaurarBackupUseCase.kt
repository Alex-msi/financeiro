package com.example.financeiro.domain.usecase

import androidx.room.withTransaction
import com.example.financeiro.data.local.database.AppDatabase
import com.example.financeiro.data.local.database.entity.CartaoEntity
import com.example.financeiro.data.local.database.entity.CategoriaEntity
import com.example.financeiro.data.local.database.entity.ContaEntity
import com.example.financeiro.data.local.database.entity.ParcelamentoEntity
import com.example.financeiro.data.local.database.entity.SubcategoriaEntity
import com.example.financeiro.data.local.database.entity.TransacaoEntity
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONObject

class RestaurarBackupUseCase @Inject constructor(
    private val database: AppDatabase
) {
    suspend operator fun invoke(conteudo: String): Result<Unit> = runCatching {
        val json = JSONObject(conteudo)
        require(json.optString("app") == "Financeiro") { "Arquivo de backup inválido." }
        require(json.optInt("versao_backup", 0) in 1..1) { "Versão de backup incompatível." }

        val contas = json.getJSONArrayObrigatorio("contas").mapJson { item ->
            ContaEntity(
                id = item.getLong("id"),
                nome = item.getString("nome"),
                tipo = item.getString("tipo"),
                saldoInicial = item.getDouble("saldo_inicial"),
                saldoAtual = item.getDouble("saldo_atual"),
                ativa = item.optBoolean("ativa", true),
                criadoEm = item.getLong("criado_em")
            )
        }
        val cartoes = json.getJSONArrayObrigatorio("cartoes").mapJson { item ->
            CartaoEntity(
                id = item.getLong("id"),
                nome = item.getString("nome"),
                limiteTotal = item.getDouble("limite_total"),
                diaFechamento = item.getInt("dia_fechamento"),
                diaVencimento = item.getInt("dia_vencimento"),
                ativo = item.optBoolean("ativo", true),
                criadoEm = item.getLong("criado_em")
            )
        }
        val categorias = json.getJSONArrayObrigatorio("categorias").mapJson { item ->
            CategoriaEntity(
                id = item.getLong("id"),
                nome = item.getString("nome"),
                tipo = item.getString("tipo"),
                cor = item.optString("cor", "#607D8B"),
                criadoEm = item.getLong("criado_em")
            )
        }
        val subcategorias = json.getJSONArrayObrigatorio("subcategorias").mapJson { item ->
            SubcategoriaEntity(
                id = item.getLong("id"),
                nome = item.getString("nome"),
                categoriaId = item.getLong("categoria_id"),
                ativa = item.optBoolean("ativa", true),
                criadoEm = item.getLong("criado_em")
            )
        }
        val transacoes = json.getJSONArrayObrigatorio("transacoes").mapJson { item ->
            TransacaoEntity(
                id = item.getLong("id"),
                valor = item.getDouble("valor"),
                dataCompetencia = item.getLong("data_competencia"),
                tipo = item.getString("tipo"),
                categoriaId = item.optLongOrNull("categoria_id"),
                subcategoriaId = item.optLongOrNull("subcategoria_id"),
                formaPagamento = item.getString("forma_pagamento"),
                cartaoId = item.optLongOrNull("cartao_id"),
                contaId = item.optLongOrNull("conta_id"),
                parcelado = item.optBoolean("parcelado", false),
                numeroParcelas = item.optInt("numero_parcelas", 1),
                parcelaAtual = item.optInt("parcela_atual", 1),
                observacao = item.optStringOrNull("observacao"),
                recorrenciaId = item.optStringOrNull("recorrencia_id"),
                recorrenciaIndice = item.optIntOrNull("recorrencia_indice"),
                criadoEm = item.getLong("criado_em")
            )
        }
        val parcelamentos = json.getJSONArrayObrigatorio("parcelamentos").mapJson { item ->
            ParcelamentoEntity(
                id = item.getLong("id"),
                transacaoPrincipalId = item.getLong("transacao_principal_id"),
                valorParcela = item.getDouble("valor_parcela"),
                totalParcelas = item.getInt("total_parcelas"),
                parcelasPagas = item.optInt("parcelas_pagas", 0),
                dataPrimeiraParcela = item.getLong("data_primeira_parcela"),
                cartaoId = item.optLongOrNull("cartao_id"),
                criadoEm = item.getLong("criado_em")
            )
        }

        validarReferencias(contas, cartoes, categorias, subcategorias, transacoes, parcelamentos)

        database.withTransaction {
            database.parcelamentoDao().deleteAll()
            database.transacaoDao().deleteAll()
            database.subcategoriaDao().deleteAll()
            database.categoriaDao().deleteAll()
            database.cartaoDao().deleteAll()
            database.contaDao().deleteAll()

            database.contaDao().insertAll(contas)
            database.cartaoDao().insertAll(cartoes)
            database.categoriaDao().insertAll(categorias)
            database.subcategoriaDao().insertAll(subcategorias)
            database.transacaoDao().insertAll(transacoes)
            database.parcelamentoDao().insertAll(parcelamentos)
        }
    }

    private fun validarReferencias(
        contas: List<ContaEntity>,
        cartoes: List<CartaoEntity>,
        categorias: List<CategoriaEntity>,
        subcategorias: List<SubcategoriaEntity>,
        transacoes: List<TransacaoEntity>,
        parcelamentos: List<ParcelamentoEntity>
    ) {
        val contaIds = contas.map { it.id }.toSet()
        val cartaoIds = cartoes.map { it.id }.toSet()
        val categoriaIds = categorias.map { it.id }.toSet()
        val subcategoriaIds = subcategorias.map { it.id }.toSet()
        val transacaoIds = transacoes.map { it.id }.toSet()

        require(subcategorias.all { it.categoriaId in categoriaIds }) {
            "Backup possui subcategoria sem categoria correspondente."
        }
        require(transacoes.all { it.contaId == null || it.contaId in contaIds }) {
            "Backup possui transação sem conta correspondente."
        }
        require(transacoes.all { it.cartaoId == null || it.cartaoId in cartaoIds }) {
            "Backup possui transação sem cartão correspondente."
        }
        require(transacoes.all { it.categoriaId == null || it.categoriaId in categoriaIds }) {
            "Backup possui transação sem categoria correspondente."
        }
        require(transacoes.all { it.subcategoriaId == null || it.subcategoriaId in subcategoriaIds }) {
            "Backup possui transação sem subcategoria correspondente."
        }
        require(parcelamentos.all { it.transacaoPrincipalId in transacaoIds }) {
            "Backup possui parcelamento sem transação correspondente."
        }
        require(parcelamentos.all { it.cartaoId == null || it.cartaoId in cartaoIds }) {
            "Backup possui parcelamento sem cartão correspondente."
        }
    }

    private fun JSONObject.getJSONArrayObrigatorio(nome: String): JSONArray {
        require(has(nome)) { "Backup incompleto: $nome não encontrado." }
        return getJSONArray(nome)
    }

    private fun <T> JSONArray.mapJson(transform: (JSONObject) -> T): List<T> =
        List(length()) { index -> transform(getJSONObject(index)) }

    private fun JSONObject.optLongOrNull(nome: String): Long? =
        if (!has(nome) || isNull(nome)) null else getLong(nome)

    private fun JSONObject.optIntOrNull(nome: String): Int? =
        if (!has(nome) || isNull(nome)) null else getInt(nome)

    private fun JSONObject.optStringOrNull(nome: String): String? =
        if (!has(nome) || isNull(nome)) null else getString(nome)
}
