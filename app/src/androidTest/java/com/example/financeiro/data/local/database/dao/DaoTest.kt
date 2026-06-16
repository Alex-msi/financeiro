package com.example.financeiro.data.local.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.financeiro.data.local.database.AppDatabase
import com.example.financeiro.data.local.database.entity.CartaoEntity
import com.example.financeiro.data.local.database.entity.CategoriaEntity
import com.example.financeiro.data.local.database.entity.ContaEntity
import com.example.financeiro.data.local.database.entity.ParcelamentoEntity
import com.example.financeiro.data.local.database.entity.SubcategoriaEntity
import com.example.financeiro.data.local.database.entity.TransacaoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var db: AppDatabase
    private lateinit var contaDao: ContaDao
    private lateinit var cartaoDao: CartaoDao
    private lateinit var categoriaDao: CategoriaDao
    private lateinit var subcategoriaDao: SubcategoriaDao
    private lateinit var transacaoDao: TransacaoDao
    private lateinit var parcelamentoDao: ParcelamentoDao

    // Mês de referência: janeiro 2024
    private val inicioMes = 1704067200000L  // 2024-01-01 00:00:00 UTC
    private val fimMes    = 1706745600000L  // 2024-02-01 00:00:00 UTC
    private val dentroMes = 1704153600000L  // 2024-01-02

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        contaDao = db.contaDao()
        cartaoDao = db.cartaoDao()
        categoriaDao = db.categoriaDao()
        subcategoriaDao = db.subcategoriaDao()
        transacaoDao = db.transacaoDao()
        parcelamentoDao = db.parcelamentoDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private suspend fun inserirConta() =
        contaDao.insert(ContaEntity(nome = "Nubank", tipo = "corrente", saldoInicial = 1000.0, saldoAtual = 1000.0))

    private suspend fun inserirCartao() =
        cartaoDao.insert(CartaoEntity(nome = "Itaú", limiteTotal = 5000.0, diaFechamento = 10, diaVencimento = 25))

    private suspend fun inserirCategoria() =
        categoriaDao.insert(CategoriaEntity(nome = "Alimentação", tipo = "despesa"))

    private fun transacao(
        valor: Double = 100.0,
        tipo: String = "despesa",
        data: Long = dentroMes,
        cartaoId: Long? = null,
        contaId: Long? = null,
        categoriaId: Long? = null
    ) = TransacaoEntity(
        valor = valor,
        dataCompetencia = data,
        tipo = tipo,
        formaPagamento = if (cartaoId != null) "cartao" else "conta",
        categoriaId = categoriaId,
        subcategoriaId = null,
        cartaoId = cartaoId,
        contaId = contaId
    )

    // ─── ContaDao ───────────────────────────────────────────────────────────

    @Test
    fun contaDao_insertAndGetById() = runBlocking {
        val id = inserirConta()
        val result = contaDao.getById(id)
        assertNotNull(result)
        assertEquals("Nubank", result?.nome)
    }

    @Test
    fun contaDao_getAll_retornaTodasContas() = runBlocking {
        contaDao.insert(ContaEntity(nome = "Nubank", tipo = "corrente", saldoInicial = 500.0, saldoAtual = 500.0))
        contaDao.insert(ContaEntity(nome = "Bradesco", tipo = "poupanca", saldoInicial = 300.0, saldoAtual = 300.0))
        assertEquals(2, contaDao.getAll().first().size)
    }

    @Test
    fun contaDao_getSaldoTotal_somaCorreta() = runBlocking {
        contaDao.insert(ContaEntity(nome = "C1", tipo = "corrente", saldoInicial = 1000.0, saldoAtual = 1000.0))
        contaDao.insert(ContaEntity(nome = "C2", tipo = "poupanca", saldoInicial = 500.0, saldoAtual = 500.0))
        assertEquals(1500.0, contaDao.getSaldoTotal().first() ?: 0.0, 0.0)
    }

    @Test
    fun contaDao_delete_removeCorretamente() = runBlocking {
        val id = inserirConta()
        contaDao.delete(contaDao.getById(id)!!)
        assertNull(contaDao.getById(id))
    }

    @Test
    fun contaDao_update_atualizaCorretamente() = runBlocking {
        val id = inserirConta()
        contaDao.update(contaDao.getById(id)!!.copy(nome = "New", saldoAtual = 200.0))
        val atualizada = contaDao.getById(id)
        assertEquals("New", atualizada?.nome)
        assertEquals(200.0, atualizada?.saldoAtual ?: 0.0, 0.0)
    }

    // ─── CartaoDao ──────────────────────────────────────────────────────────

    @Test
    fun cartaoDao_insertAndGetById() = runBlocking {
        val id = inserirCartao()
        assertNotNull(cartaoDao.getById(id))
    }

    @Test
    fun cartaoDao_getAll_retornaTodosCartoes() = runBlocking {
        inserirCartao()
        cartaoDao.insert(CartaoEntity(nome = "Nubank", limiteTotal = 3000.0, diaFechamento = 3, diaVencimento = 10))
        assertEquals(2, cartaoDao.getAll().first().size)
    }

    @Test
    fun cartaoDao_delete_removeCorretamente() = runBlocking {
        val id = inserirCartao()
        cartaoDao.delete(cartaoDao.getById(id)!!)
        assertNull(cartaoDao.getById(id))
    }

    // ─── CategoriaDao ───────────────────────────────────────────────────────

    @Test
    fun categoriaDao_insertAndGetById() = runBlocking {
        val id = inserirCategoria()
        assertEquals("Alimentação", categoriaDao.getById(id)?.nome)
    }

    @Test
    fun categoriaDao_getByTipo_filtraCorretamente() = runBlocking {
        categoriaDao.insert(CategoriaEntity(nome = "Alimentação", tipo = "despesa"))
        categoriaDao.insert(CategoriaEntity(nome = "Transporte", tipo = "despesa"))
        categoriaDao.insert(CategoriaEntity(nome = "Salário", tipo = "receita"))
        assertEquals(2, categoriaDao.getByTipo("despesa").first().size)
        assertEquals(1, categoriaDao.getByTipo("receita").first().size)
    }

    @Test
    fun categoriaDao_delete_removeCorretamente() = runBlocking {
        val id = inserirCategoria()
        categoriaDao.delete(categoriaDao.getById(id)!!)
        assertNull(categoriaDao.getById(id))
    }

    // ─── SubcategoriaDao ────────────────────────────────────────────────────

    @Test
    fun subcategoriaDao_insertAndGetById() = runBlocking {
        val catId = inserirCategoria()
        val id = subcategoriaDao.insert(SubcategoriaEntity(nome = "Supermercado", categoriaId = catId))
        assertEquals("Supermercado", subcategoriaDao.getById(id)?.nome)
    }

    @Test
    fun subcategoriaDao_getByCategoriaId_filtraCorretamente() = runBlocking {
        val catId1 = inserirCategoria()
        val catId2 = categoriaDao.insert(CategoriaEntity(nome = "Transporte", tipo = "despesa"))
        subcategoriaDao.insert(SubcategoriaEntity(nome = "Supermercado", categoriaId = catId1))
        subcategoriaDao.insert(SubcategoriaEntity(nome = "Restaurante", categoriaId = catId1))
        subcategoriaDao.insert(SubcategoriaEntity(nome = "Uber", categoriaId = catId2))
        val result = subcategoriaDao.getByCategoriaId(catId1).first()
        assertEquals(2, result.size)
        assertTrue(result.all { it.categoriaId == catId1 })
    }

    @Test
    fun subcategoriaDao_delete_removeCorretamente() = runBlocking {
        val catId = inserirCategoria()
        val id = subcategoriaDao.insert(SubcategoriaEntity(nome = "Farmácia", categoriaId = catId))
        subcategoriaDao.delete(subcategoriaDao.getById(id)!!)
        assertNull(subcategoriaDao.getById(id))
    }

    // ─── TransacaoDao ───────────────────────────────────────────────────────

    @Test
    fun transacaoDao_insertAndGetById() = runBlocking {
        val contaId = inserirConta()
        val id = transacaoDao.insert(transacao(contaId = contaId))
        assertNotNull(transacaoDao.getById(id))
    }

    @Test
    fun transacaoDao_getByMes_retornaApenasDoMes() = runBlocking {
        val contaId = inserirConta()
        transacaoDao.insert(transacao(data = dentroMes, contaId = contaId))
        transacaoDao.insert(transacao(data = fimMes + 1000, contaId = contaId)) // fora do mês
        val result = transacaoDao.getByMes(inicioMes, fimMes).first()
        assertEquals(1, result.size)
    }

    @Test
    fun transacaoDao_getByCartao_filtraCorretamente() = runBlocking {
        val cartaoId = inserirCartao()
        val contaId = inserirConta()
        transacaoDao.insert(transacao(cartaoId = cartaoId))
        transacaoDao.insert(transacao(contaId = contaId))
        val result = transacaoDao.getByCartao(cartaoId).first()
        assertEquals(1, result.size)
        assertEquals(cartaoId, result[0].cartaoId)
    }

    @Test
    fun transacaoDao_getByCategoria_filtraCorretamente() = runBlocking {
        val catId = inserirCategoria()
        val contaId = inserirConta()
        transacaoDao.insert(transacao(contaId = contaId, categoriaId = catId))
        transacaoDao.insert(transacao(contaId = contaId, categoriaId = null))
        val result = transacaoDao.getByCategoria(catId).first()
        assertEquals(1, result.size)
    }

    @Test
    fun transacaoDao_getSomaReceitasPeriodo_calculaCorretamente() = runBlocking {
        val contaId = inserirConta()
        transacaoDao.insert(transacao(valor = 500.0, tipo = "receita", data = dentroMes, contaId = contaId))
        transacaoDao.insert(transacao(valor = 300.0, tipo = "receita", data = dentroMes, contaId = contaId))
        transacaoDao.insert(transacao(valor = 100.0, tipo = "despesa", data = dentroMes, contaId = contaId))
        val soma = transacaoDao.getSomaReceitasPeriodo(inicioMes, fimMes).first()
        assertEquals(800.0, soma ?: 0.0, 0.0)
    }

    @Test
    fun transacaoDao_getSomaDespesasPeriodo_calculaCorretamente() = runBlocking {
        val contaId = inserirConta()
        transacaoDao.insert(transacao(valor = 200.0, tipo = "despesa", data = dentroMes, contaId = contaId))
        transacaoDao.insert(transacao(valor = 150.0, tipo = "despesa", data = dentroMes, contaId = contaId))
        transacaoDao.insert(transacao(valor = 500.0, tipo = "receita", data = dentroMes, contaId = contaId))
        val soma = transacaoDao.getSomaDespesasPeriodo(inicioMes, fimMes).first()
        assertEquals(350.0, soma ?: 0.0, 0.0)
    }

    @Test
    fun transacaoDao_getSaldoPeriodo_calculaCorretamente() = runBlocking {
        val contaId = inserirConta()
        transacaoDao.insert(transacao(valor = 1000.0, tipo = "receita", data = dentroMes, contaId = contaId))
        transacaoDao.insert(transacao(valor = 400.0, tipo = "despesa", data = dentroMes, contaId = contaId))
        val saldo = transacaoDao.getSaldoPeriodo(inicioMes, fimMes).first()
        assertEquals(600.0, saldo ?: 0.0, 0.0)
    }

    @Test
    fun transacaoDao_delete_removeCorretamente() = runBlocking {
        val contaId = inserirConta()
        val id = transacaoDao.insert(transacao(contaId = contaId))
        transacaoDao.delete(transacaoDao.getById(id)!!)
        assertNull(transacaoDao.getById(id))
    }

    // ─── ParcelamentoDao ────────────────────────────────────────────────────

    @Test
    fun parcelamentoDao_insertAndGetById() = runBlocking {
        val contaId = inserirConta()
        val transacaoId = transacaoDao.insert(transacao(contaId = contaId))
        val id = parcelamentoDao.insert(
            ParcelamentoEntity(
                transacaoPrincipalId = transacaoId,
                valorParcela = 100.0,
                totalParcelas = 12,
                dataPrimeiraParcela = dentroMes,
                cartaoId = null
            )
        )
        assertNotNull(parcelamentoDao.getById(id))
    }

    @Test
    fun parcelamentoDao_getParcelasFuturasPorMes_filtraCorretamente() = runBlocking {
        val contaId = inserirConta()
        val transacaoId = transacaoDao.insert(transacao(contaId = contaId))
        // dentro do mês, em aberto
        parcelamentoDao.insert(
            ParcelamentoEntity(
                transacaoPrincipalId = transacaoId,
                valorParcela = 100.0,
                totalParcelas = 12,
                parcelasPagas = 0,
                dataPrimeiraParcela = dentroMes,
                cartaoId = null
            )
        )
        // fora do mês
        parcelamentoDao.insert(
            ParcelamentoEntity(
                transacaoPrincipalId = transacaoId,
                valorParcela = 200.0,
                totalParcelas = 6,
                parcelasPagas = 0,
                dataPrimeiraParcela = fimMes + 1000,
                cartaoId = null
            )
        )
        // quitado (dentro do mês mas pagas == total)
        parcelamentoDao.insert(
            ParcelamentoEntity(
                transacaoPrincipalId = transacaoId,
                valorParcela = 50.0,
                totalParcelas = 3,
                parcelasPagas = 3,
                dataPrimeiraParcela = dentroMes,
                cartaoId = null
            )
        )
        val result = parcelamentoDao.getParcelasFuturasPorMes(inicioMes, fimMes).first()
        assertEquals(1, result.size)
        assertEquals(100.0, result[0].valorParcela, 0.0)
    }

    @Test
    fun parcelamentoDao_getSomaParcelasFuturasPorMes_calculaCorretamente() = runBlocking {
        val contaId = inserirConta()
        val transacaoId = transacaoDao.insert(transacao(contaId = contaId))
        parcelamentoDao.insert(
            ParcelamentoEntity(
                transacaoPrincipalId = transacaoId,
                valorParcela = 300.0,
                totalParcelas = 10,
                parcelasPagas = 2,
                dataPrimeiraParcela = dentroMes,
                cartaoId = null
            )
        )
        parcelamentoDao.insert(
            ParcelamentoEntity(
                transacaoPrincipalId = transacaoId,
                valorParcela = 150.0,
                totalParcelas = 6,
                parcelasPagas = 0,
                dataPrimeiraParcela = dentroMes,
                cartaoId = null
            )
        )
        val soma = parcelamentoDao.getSomaParcelasFuturasPorMes(inicioMes, fimMes).first()
        assertEquals(450.0, soma ?: 0.0, 0.0)
    }

    @Test
    fun parcelamentoDao_delete_removeCorretamente() = runBlocking {
        val contaId = inserirConta()
        val transacaoId = transacaoDao.insert(transacao(contaId = contaId))
        val id = parcelamentoDao.insert(
            ParcelamentoEntity(
                transacaoPrincipalId = transacaoId,
                valorParcela = 100.0,
                totalParcelas = 5,
                dataPrimeiraParcela = dentroMes,
                cartaoId = null
            )
        )
        parcelamentoDao.delete(parcelamentoDao.getById(id)!!)
        assertNull(parcelamentoDao.getById(id))
    }
}