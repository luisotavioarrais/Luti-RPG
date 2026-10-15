# CODEX — FINALIZE O MOD LUTI RPG PARA HYTALE

Leia `README.md` INTEIRO antes de modificar. O usuário quer um `.jar` instalável independente de outros mods. O projeto contém MOTOR JAVA testável, mas ainda não é jogável.

## Regras obrigatórias
1. Cinco classes: GUERREIRO (bárbaro 2 mãos), PALADINO (espada 1 mão+escudo), MAGO (runestones), ASSASSINO (duas adagas), ARQUEIRO (arco/besta). **Nunca proibir** outras armas/magias/equipamentos.
2. Apenas atributos permitidos de cada classe aceitam pontos: universais FORCA, VITALIDADE, RESISTENCIA + dois exclusivos (ver `RpgCore.ALLOWED`). **5 pontos por subida de nível**, 1 a 50, maxLevel externo/configurável. Classes não concedem status extra automaticamente.
3. Bônus ofensivos APENAS para a categoria de equipamento especializada (ver `RpgCore.SPECIALTY`). Qualquer outra arma mantém valores nativos. FORCA não dá bônus de dano em armas de fora. VIDA/RESISTÊNCIA são universais. Paladino exige espada+escudo simultaneamente para perks; Assassino bônus com adagas (testar mão única vs dual e documentar), Arqueiro arco/besta, Guerreiro duas mãos, Mago runas. Mana/poder arcano só bônus com magia e validando sistema oficial.
4. Kits UMA VEZ: Guerreiro machado 2 mãos, Paladino espada+escudo, Arqueiro arco+50 flechas, Assassino duas adagas, Mago runa utilizável desde o começo se permitido pela API. Sem mods externos. Qualquer jogador pode conseguir runas pela progressão normal.
5. 60 missões progressivas sem NPCs, do início até conclusão do Capítulo 1 (quest TSV é rascunho; confirmar etapa final no cliente instalado). HUD/diário simples com objetivo e recompensa; detecção automática no servidor. Recompensa item+XP, pendência segura para inventário cheio, anti-duplicação.
6. Escolha inicial persistente, salvar nível, XP, classe, investimentos, progresso, itens pendentes e status de entrega do kit. Bloquear troca de classe por padrão; admin pode resetar com aviso explícito. Testar reentrada/respawn.

## Fazer agora
- Identificar SDK/API oficial da versão instalada e JDK exigido; **não inventar** classes/métodos/IDs. `libs/HytaleServer.jar` deve vir da instalação legítima local. Ajustar `manifest.json`, `build.gradle.kts`.
- Implementar adapter real `JavaPlugin`: eventos de coleta/mineração, crafting, exploração/entrada em áreas, mortes atribuídas ao jogador, chefes, interações com runas, movimentação e dano. Eventos precisam ser seguros por jogador (UUID) e no thread correto.
- Mapear todos IDs lógicos em `quests.tsv` e `StarterKits` para IDs canonizados do conteúdo atual. Rebalancear recompensas para não saltar tiers. Eliminar objetivos impossíveis, automatizar marco final sem comando manual; deixar documentado o que não é detectável na API.
- Aplicar bonusesFor apenas no tipo de arma correta, respeitando limites e sem sobrescrever dano do jogo em outros equipamentos. Testar velocidade de ataques sem quebrar animações; verificar mana e runa na versão de jogo. Configurar stats em arquivo se possível.
- UI `/rpg`, `/rpg classe`, `/rpg status`, `/rpg add`, `/rpg missao`, `/rpg resgatar`; menu visual de classes, stats e diário; texto pt-BR. Sem NPCs.
- Escrever testes unitários e testes in-game. Compilar `.jar`, fornecer PowerShell de instalação, relatório de testes, changelog. Manter projeto-fonte; não afirmar sucesso in-game sem validar.

O conteúdo do Capítulo 1 e IDs associados podem variar. Pesquise a API/arquivos legítimos do usuário; jamais forjar compatibilidade ou copiar código dos mods RPG Leveling e KyuubiSoft.
