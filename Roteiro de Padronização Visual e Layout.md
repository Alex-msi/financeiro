# Roteiro de Padronização Visual e Layout
## Aplicativo AppBNCC Computação
### Fundamentação Teórica para TCC

> *“A interface do aplicativo foi desenvolvida com base nas diretrizes do Material Design, associadas aos princípios ergonômicos da ISO 9241, aos critérios de acessibilidade da WCAG e às recomendações visuais do Design System Gov.br, visando garantir padronização visual, legibilidade, usabilidade, acessibilidade e identidade institucional no contexto educacional.”*

Este roteiro define uma base de padronização para o desenvolvimento da interface do aplicativo AppBNCC Computação, considerando:

- Material Design 3;
- International Organization for Standardization ISO 9241;
- WCAG (acessibilidade);
- padrões modernos de UX/UI para Android;
- Design System Gov.br;
- identidade visual inspirada nas cores institucionais do Ministério da Educação e do portal oficial da BNCC.

---

# 1. Objetivo do Layout

O layout deve:

- facilitar a navegação;
- permitir leitura confortável;
- reduzir esforço cognitivo;
- manter padronização visual;
- garantir acessibilidade;
- funcionar em diferentes tamanhos de tela;
- transmitir aparência institucional e educacional;
- aproximar visualmente o aplicativo dos sistemas oficiais educacionais brasileiros.

---

# 2. Diretrizes Gerais

## Base Visual

Utilizar:

- Material Design 3;
- layout responsivo;
- navegação simples;
- poucos elementos por tela;
- hierarquia visual clara;
- separação visual suave;
- aparência institucional baseada no portal da BNCC e no padrão Gov.br.

## Princípios do Gov.br Design System

Priorizar:

- simplicidade visual;
- clareza das informações;
- foco em leitura e consulta;
- acessibilidade;
- consistência visual;
- organização hierárquica do conteúdo;
- feedback visual claro para ações do usuário.

---

# 3. Paleta de Cores

## Referência Visual MEC e BNCC

As cores institucionais utilizadas como referência são:

- azul institucional;
- verde institucional;
- dourado institucional;
- branco;
- cinza neutro.

## Paleta Recomendada

| Função | Cor | Hex |
|--------|-----|-----|
| Primária | Azul MEC | #0D47A1 |
| Primária Clara | Azul médio | #1565C0 |
| Secundária | Verde MEC | #2E7D32 |
| Fundo | Branco | #FFFFFF |
| Superfície | Cinza claro | #F5F5F5 |
| Texto Principal | Preto suave | #212121 |
| Texto Secundário | Cinza escuro | #616161 |
| Erro | Vermelho | #D32F2F |
| Sucesso | Verde | #388E3C |
| Divisórias | Cinza | #E0E0E0 |

## Cores das Etapas Educacionais

As cores das etapas foram inspiradas no portal oficial da BNCC e no padrão visual institucional do MEC.

```xml
<color name="etapa_ei">#2E7D32</color>
<color name="etapa_ef">#1565C0</color>
<color name="etapa_em">#F9A825</color>
```

| Etapa | Cor |
|---|---|
| Educação Infantil | Verde institucional |
| Ensino Fundamental | Azul institucional |
| Ensino Médio | Dourado institucional |

---

# 4. Regras de Contraste (WCAG)

## Contraste mínimo

| Tipo | Contraste |
|------|-----------|
| Texto normal | 4.5:1 |
| Texto grande | 3:1 |

## Regras importantes

### Nunca usar:

- texto azul sobre fundo verde;
- cinza claro em fundo branco;
- fonte pequena em cores claras;
- excesso de cores simultaneamente.

### Preferir:

- fundo claro com texto escuro;
- destaque visual apenas no essencial;
- aparência limpa e institucional;
- contraste elevado para leitura.

---

# 5. Tipografia

## Fonte Recomendada

Android:

- Roboto;
- ou Noto Sans.

## Tamanhos Padronizados

| Elemento | Tamanho |
|----------|---------|
| Título principal | 24sp |
| Título de seção | 20sp |
| Subtítulo | 18sp |
| Texto comum | 16sp |
| Texto secundário | 14sp |
| Texto auxiliar | 12sp |
| Caption | 11sp |
| Botões | 14sp–16sp |

## Peso das Fontes

| Uso | Peso |
|-----|------|
| Título | Bold |
| Subtítulo | Medium |
| Texto comum | Regular |
| Destaques | SemiBold |

---

# 6. Espaçamentos

## Base Material Design

Utilizar múltiplos de:

- 4dp;
- 8dp.

## Padrões Recomendados

| Elemento | Medida |
|----------|--------|
| Padding lateral da tela | 16dp |
| Espaço entre cards | 12dp |
| Espaço entre seções | 24dp |
| Padding interno card | 16dp |
| Espaço entre ícone e texto | 8dp |

---

# 7. Componentes

## Botões

### Medidas

| Item | Medida |
|------|--------|
| Altura mínima | 48dp |
| Raio borda | 12dp |
| Padding horizontal | 16dp |

### Diretrizes

- utilizar feedback visual ao toque;
- manter contraste adequado;
- evitar excesso de botões na mesma tela;
- utilizar azul como principal;
- utilizar verde apenas para confirmações e destaques positivos.

## Cards

### Estrutura recomendada

- bordas arredondadas;
- sombra leve;
- padding interno;
- título destacado;
- informações organizadas verticalmente;
- separação visual suave.

### Medidas

| Item | Valor |
|------|-------|
| Radius | 16dp |
| Elevação | 2dp–4dp |
| Padding interno | 16dp |

### Recomendações Gov.br

- preferir sombras suaves;
- utilizar bordas discretas;
- evitar excesso de profundidade visual.

## Ícones

| Item | Medida |
|------|--------|
| Ícone padrão | 24dp |
| Ícone pequeno | 20dp |
| Ícone grande | 32dp |

---

# 8. Estrutura das Telas

## Tela Inicial

### Componentes

- logo/título;
- descrição curta;
- opções principais;
- navegação simples;
- aparência institucional;
- organização hierárquica.

## Cabeçalho (AppBar)

| Item | Valor |
|------|-------|
| Altura | 56dp |
| Ícone voltar | 24dp |
| Título | 20sp |

### Diretrizes

- utilizar azul institucional;
- evitar excesso de ícones;
- manter título claro da seção atual;
- seguir padrão Android de navegação.

---

# 9. Navegação

## Fluxo Principal

Fluxo recomendado:

```text
Etapa → Série → Eixo → Objeto de Conhecimento → Habilidade
```

No Ensino Médio:

```text
Etapa → Competência Específica → Habilidade
```

## Regras de Navegação

O usuário deve identificar rapidamente:

- onde está;
- o que pode clicar;
- qual é a ação principal;
- como voltar.

## Estados Visuais

Implementar:

- feedback de clique;
- estado selecionado;
- loading padronizado;
- mensagens de vazio;
- mensagens de erro amigáveis.

Exemplo:

```text
“Nenhuma habilidade encontrada.”
```

---

# 10. Acessibilidade

## ISO 9241 + WCAG

O aplicativo deve:

- possuir textos legíveis;
- possuir contraste adequado;
- evitar excesso de informação;
- ter elementos clicáveis grandes;
- permitir leitura confortável;
- evitar poluição visual;
- permitir navegação acessível.

## Área de Toque

| Item | Mínimo |
|------|--------|
| Área clicável | 48dp x 48dp |

## Recomendações

### Sempre:

- usar descrição em ícones;
- usar feedback visual;
- indicar seleção ativa;
- manter padrão visual;
- permitir aumento de fonte;
- utilizar labels acessíveis;
- manter ordem correta de foco;
- garantir compatibilidade com TalkBack.

### Evitar:

- textos longos centralizados;
- excesso de cores;
- menus escondidos;
- excesso de informações na mesma tela.

---

# 11. Responsividade

## Compatibilidade

O layout deve funcionar em:

- celulares pequenos;
- celulares grandes;
- tablets;
- modo paisagem.

## Estratégias

Utilizar:

- ConstraintLayout;
- ScrollView;
- RecyclerView;
- dimens em dp/sp.

---

# 12. Organização Visual Recomendada

## Hierarquia

### Ordem de destaque

1. título;
2. conteúdo principal;
3. ações;
4. informações secundárias.

## Regra visual

A interface deve priorizar:

- clareza;
- leitura;
- simplicidade;
- organização;
- aparência institucional.

---

# 13. Recomendações Específicas para o AppBNCC

## Ideal para o projeto

Utilizar:

- cards para habilidades;
- chips para eixos;
- cores suaves;
- azul institucional como principal;
- verde apenas para destaques positivos;
- listas organizadas;
- RecyclerView em praticamente todas as telas.

## Sugestão Visual

| Elemento | Cor |
|----------|-----|
| Cabeçalho/AppBar | Azul MEC |
| Botão principal | Azul MEC |
| Botão de confirmação | Verde MEC |
| Fundo | Branco |
| Cards | Branco com sombra leve |
| Texto | Preto suave |

## Fundamentação Institucional

A proposta visual busca aproximar a aplicação dos padrões institucionais adotados em sistemas governamentais brasileiros, favorecendo familiaridade visual, padronização, acessibilidade e organização hierárquica da informação.

O padrão adotado foi inspirado:

- no portal oficial da BNCC;
- no Design System Gov.br;
- no Material Design 3;
- nas recomendações WCAG;
- nos princípios ergonômicos da ISO 9241.

---

## Estado atual implementado

Atualizacao vigente do AppBNCC:

- O app principal e `AppComBncc`, em Kotlin Android, com arquitetura simples em camadas: UI, ViewModel, Repository, DAO/Room e integracoes Firebase.
- O banco Room esta na versao 7.
- A sincronizacao dos dados normativos da BNCC e publica e ocorre na primeira abertura do app, sem depender de login.
- O login Google/Firebase e exigido para criar, salvar, editar, remover ou acessar praticas do usuario, nao para consultar a BNCC.
- Ao sair da conta, a sessao do usuario e limpa, mas os dados BNCC sincronizados permanecem disponiveis para consulta publica.
- A tela autenticada usa o bloco `Perfil`, nao mostra ID do usuario, e exibe inicialmente apenas `Editar perfil` e `Sair da conta`; ao editar, mostra apenas `Salvar`.
- A area de praticas autenticadas exibe `Minhas praticas` e `Compartilhadas`; ao abrir, carrega `Minhas praticas`.
- Praticas usam UUID string, metadados de dono Firebase, soft delete, versionamento e `SyncStatus`.
- A regra vigente separa praticas do usuario em `praticas` e praticas compartilhadas curadas em `praticas_compartilhadas`.
- Nao existe mais compartilhamento individual por e-mail ou UID. Os campos `compartilhadoComEmails` e `compartilhadoComUids` nao fazem parte do fluxo atual.
- O arquivo de referencia principal atualizado e `docs/Mapa tecnico.md`.


