package com.example.financeiro.ui.relatorio

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.core.content.FileProvider
import com.example.financeiro.R
import com.example.financeiro.databinding.FragmentRelatoriosBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class RelatoriosFragment : Fragment() {

    private var _binding: FragmentRelatoriosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RelatoriosViewModel by viewModels()
    private val adapter = CategoriasRelatorioAdapter()
    private var estadoAtual = RelatoriosUiState()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRelatoriosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerCategorias.adapter = adapter
        configurarGrafico()
        binding.btnVoltar.setOnClickListener { findNavController().navigateUp() }
        binding.btnMesAnterior.setOnClickListener { viewModel.irParaMesAnterior() }
        binding.btnProximoMes.setOnClickListener { viewModel.irParaProximoMes() }
        binding.btnExportar.setOnClickListener { escolherExportacao() }
        observarEstado()
    }

    private fun escolherExportacao() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Exportar")
            .setItems(arrayOf("Relatório do mês", "Backup completo")) { _, which ->
                if (which == 0) exportarCsv() else exportarBackup()
            }
            .show()
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    estadoAtual = state
                    binding.progressBar.isVisible = state.isLoading
                    binding.contentGroup.isVisible = !state.isLoading
                    binding.tvMes.text = state.labelMes
                    binding.tvReceitas.text = state.receitasFormatadas
                    binding.tvDespesas.text = state.despesasFormatadas
                    binding.tvSaldo.text = state.saldoFormatado
                    binding.tvSaldo.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (state.saldo >= 0) R.color.receita else R.color.despesa
                        )
                    )
                    binding.tvSemDespesas.isVisible = state.semDespesas
                    binding.recyclerCategorias.isVisible = !state.semDespesas
                    adapter.submitList(state.categorias)
                    atualizarGrafico(state.evolucao)
                }
            }
        }
    }

    private fun exportarCsv() {
        val state = estadoAtual
        if (state.isLoading) return
        val pasta = File(requireContext().cacheDir, "relatorios").apply { mkdirs() }
        val arquivo = File(pasta, "relatorio_${state.ano}_${state.mes + 1}.csv")
        val conteudo = buildString {
            appendLine("Relatório;${state.labelMes}")
            appendLine("Receitas;${numeroCsv(state.receitas)}")
            appendLine("Despesas;${numeroCsv(state.despesas)}")
            appendLine("Saldo;${numeroCsv(state.saldo)}")
            appendLine()
            appendLine("Data;Tipo;Descrição;Categoria;Forma de pagamento;Conta ou cartão;Valor")
            state.linhasExportacao.forEach { linha ->
                appendLine(
                    listOf(
                        linha.data,
                        linha.tipo,
                        linha.descricao,
                        linha.categoria,
                        linha.formaPagamento,
                        linha.origem,
                        numeroCsv(linha.valor)
                    ).joinToString(";") { campoCsv(it) }
                )
            }
        }
        arquivo.writeText("\uFEFF$conteudo", Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            arquivo
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Relatório financeiro - ${state.labelMes}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartilhar relatório"))
    }

    private fun exportarBackup() {
        val state = estadoAtual
        if (state.isLoading) return
        val pasta = File(requireContext().cacheDir, "backups").apply { mkdirs() }
        val agora = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val arquivo = File(pasta, "financeiro_backup_$agora.json")
        arquivo.writeText(criarJsonBackup(state.backup).toString(2), Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            arquivo
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Backup financeiro")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartilhar backup"))
    }

    private fun criarJsonBackup(backup: BackupDadosUi): JSONObject =
        JSONObject()
            .put("app", "Financeiro")
            .put("versao_backup", 1)
            .put("gerado_em", System.currentTimeMillis())
            .put("contas", JSONArray().apply {
                backup.contas.forEach { conta ->
                    put(
                        JSONObject()
                            .put("id", conta.id)
                            .put("nome", conta.nome)
                            .put("tipo", conta.tipo)
                            .put("saldo_inicial", conta.saldoInicial)
                            .put("saldo_atual", conta.saldoAtual)
                            .put("ativa", conta.ativa)
                            .put("criado_em", conta.criadoEm)
                    )
                }
            })
            .put("cartoes", JSONArray().apply {
                backup.cartoes.forEach { cartao ->
                    put(
                        JSONObject()
                            .put("id", cartao.id)
                            .put("nome", cartao.nome)
                            .put("limite_total", cartao.limiteTotal)
                            .put("dia_fechamento", cartao.diaFechamento)
                            .put("dia_vencimento", cartao.diaVencimento)
                            .put("ativo", cartao.ativo)
                            .put("criado_em", cartao.criadoEm)
                    )
                }
            })
            .put("categorias", JSONArray().apply {
                backup.categorias.forEach { categoria ->
                    put(
                        JSONObject()
                            .put("id", categoria.id)
                            .put("nome", categoria.nome)
                            .put("tipo", categoria.tipo)
                            .put("cor", categoria.cor)
                            .put("criado_em", categoria.criadoEm)
                    )
                }
            })
            .put("subcategorias", JSONArray().apply {
                backup.subcategorias.forEach { subcategoria ->
                    put(
                        JSONObject()
                            .put("id", subcategoria.id)
                            .put("nome", subcategoria.nome)
                            .put("categoria_id", subcategoria.categoriaId)
                            .put("ativa", subcategoria.ativa)
                            .put("criado_em", subcategoria.criadoEm)
                    )
                }
            })
            .put("transacoes", JSONArray().apply {
                backup.transacoes.forEach { transacao ->
                    put(
                        JSONObject()
                            .put("id", transacao.id)
                            .put("valor", transacao.valor)
                            .put("data_competencia", transacao.dataCompetencia)
                            .put("tipo", transacao.tipo)
                            .put("categoria_id", transacao.categoriaId ?: JSONObject.NULL)
                            .put("subcategoria_id", transacao.subcategoriaId ?: JSONObject.NULL)
                            .put("forma_pagamento", transacao.formaPagamento)
                            .put("cartao_id", transacao.cartaoId ?: JSONObject.NULL)
                            .put("conta_id", transacao.contaId ?: JSONObject.NULL)
                            .put("parcelado", transacao.parcelado)
                            .put("numero_parcelas", transacao.numeroParcelas)
                            .put("parcela_atual", transacao.parcelaAtual)
                            .put("observacao", transacao.observacao ?: JSONObject.NULL)
                            .put("recorrencia_id", transacao.recorrenciaId ?: JSONObject.NULL)
                            .put("recorrencia_indice", transacao.recorrenciaIndice ?: JSONObject.NULL)
                            .put("criado_em", transacao.criadoEm)
                    )
                }
            })
            .put("parcelamentos", JSONArray().apply {
                backup.parcelamentos.forEach { parcelamento ->
                    put(
                        JSONObject()
                            .put("id", parcelamento.id)
                            .put("transacao_principal_id", parcelamento.transacaoPrincipalId)
                            .put("valor_parcela", parcelamento.valorParcela)
                            .put("total_parcelas", parcelamento.totalParcelas)
                            .put("parcelas_pagas", parcelamento.parcelasPagas)
                            .put("data_primeira_parcela", parcelamento.dataPrimeiraParcela)
                            .put("cartao_id", parcelamento.cartaoId ?: JSONObject.NULL)
                            .put("criado_em", parcelamento.criadoEm)
                    )
                }
            })

    private fun campoCsv(valor: Any): String {
        val texto = valor.toString().replace("\"", "\"\"")
        return "\"$texto\""
    }

    private fun numeroCsv(valor: Double): String =
        "%.2f".format(valor).replace(".", ",")

    private fun configurarGrafico() {
        binding.chartEvolucao.apply {
            description.isEnabled = false
            legend.isEnabled = true
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            axisLeft.textColor = ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
            setTouchEnabled(true)
            setPinchZoom(false)
            setScaleEnabled(false)
            setNoDataText("Sem dados no período")
            setNoDataTextColor(Color.GRAY)
        }
    }

    private fun atualizarGrafico(itens: List<EvolucaoMesUi>) {
        val receitas = itens.mapIndexed { index, item -> BarEntry(index.toFloat(), item.receitas) }
        val despesas = itens.mapIndexed { index, item -> BarEntry(index.toFloat(), item.despesas) }
        val dataReceitas = BarDataSet(receitas, "Receitas").apply {
            color = ContextCompat.getColor(requireContext(), R.color.receita)
            setDrawValues(false)
        }
        val dataDespesas = BarDataSet(despesas, "Despesas").apply {
            color = ContextCompat.getColor(requireContext(), R.color.despesa)
            setDrawValues(false)
        }
        val larguraBarra = 0.36f
        val espacoBarra = 0.05f
        val espacoGrupo = 0.18f
        binding.chartEvolucao.apply {
            data = BarData(dataReceitas, dataDespesas).apply { barWidth = larguraBarra }
            xAxis.valueFormatter = IndexAxisValueFormatter(itens.map { it.label })
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = itens.size.toFloat()
            groupBars(0f, espacoGrupo, espacoBarra)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
