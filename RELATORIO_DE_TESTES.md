# Rodada 0.3.3 — 15/10/2026

Compilação e 205 verificações de regressão aprovadas. Servidor oficial carregou e aprovou os testes nativos: frutas reconhecidas, sementes excluídas, livro com dropsOnDeath=false, cancelamento de descarte do livro sem afetar pedra, XP maior para mais vida máxima e limites de ganho. Ledger testado com troca de identidade de entidade, consumo único, expiração e remoção por outro atacante.

Evidência do relato: missão Q05 tinha 7/12 alimentos, exatamente 7 carnes/peixe, enquanto 4 maçãs e 11 frutas vermelhas eram ignoradas. Não houve exceção do RPG no log consultado. A causa exata do relato de XP após respawn não foi reproduzida no cliente; foram reforçados crédito por UUID, resolução do jogador atual e recriação do HUD. Não foi editado nenhum save para conceder XP ou avançar missão. A correção do catálogo contabilizará os itens existentes ao entrar.

Teste completo morrer/renascer/abater/coletar no cliente continua pendente. As pendências de Fé, velocidade de ataque, conjuração e campanha permanecem.

Artefato instalado: build/libs/LutiRPG-0.3.3.jar, com backup da versão anterior.

# Relatório de testes — 0.3.2 — 15/10/2026

- Compilação Java 25 contra servidor instalado Hytale 0.7.2: aprovada.
- Motor: smoke test e 201 verificações de regressão aprovadas.
- Servidor isolado com API/assets oficiais: testes de armas, equipamento especializado, runa, inventário cheio, dano, vida, mapeamento Rubble, XP por abate e limites de nível, direções da seta e serialização do Anchor.
- Cliente: após a correção, observado dentro do mundo com personagem, livro Diário na hotbar, painel de missão em linhas e barra de XP central acima das barras de vida/mana. O erro anterior identificava precisamente #XpFill.Anchor.Width; a correção usa #XpFill.Anchor e objeto completo.
- Validação anterior da 0.3.0: escolha de Mago, kit, reentrada, resgate sem repetição, primeiras missões por inventário, distribuição de pontos, máximos de vida/mana e colocação de runa na matriz nativa.
- Não foram validados nesta rodada: abate e ganho de XP em combate no cliente, coleta real de Rubble até concluir a missão, direção da seta durante movimento e campanha completa. A amostragem de HUD não equivale a uma campanha jogada.
- Pendências gerais mantidas: velocidade de ataque, efeito de Fé, conjuração efetiva, respawn e campanha até os chefes. Shift+C não implementado por ausência de API de atalhos arbitrários no servidor.

Artefato: build/libs/LutiRPG-0.3.2.jar. Instalada cópia em UserData/Mods com backup da versão anterior. Nenhum reset de classe, pontos ou missões foi executado.

## Atualização 0.3.4 — urso e diagnóstico após respawn
- Bear_Grizzly, Bear_Polar e Bear_Voidtaken incluídos no catálogo kill.hostile_basic a partir dos IDs oficiais. Não entram no objetivo específico das cavernas. Marcadores invisíveis continuam excluídos. Urso pardo com vida máxima 124 concede 31 XP pela regra existente.
- Diagnóstico no log LutiRPG.Diagnostics: RESPAWN identifica renascimento; DEATH registra role, jogador atribuído, crédito em cache e classificação hostil; PROGRESS registra avanço de objetivo.
- 205 verificações de regressão e teste isolado com servidor/assets oficiais aprovados, incluindo classificação de ursos e XP. Não reproduzida nem declarada resolvida a falha intermitente após morrer. Nenhum save alterado. A sequência morrer/renascer/abater/coletar permanece em teste no cliente.


Retorno do usuário: falha percebida apenas no contador de missão; coletar pedra após renascer contou normalmente. Sem reprodução conclusiva de travamento geral pós-respawn. Versão 0.3.4 instalada após saída do mundo, com backup da 0.3.3 e sem alterar saves. Abate de urso com catálogo corrigido ainda depende de confirmação em jogo.
