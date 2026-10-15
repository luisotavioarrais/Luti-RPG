# 0.3.3 (15/10/2026)

- Corrigido catálogo de frutas/vegetais comestíveis na missão de alimentos.
- XP por vida máxima, confirmação do ganho no chat e crédito de atacante por UUID.
- HUD reconstruído no respawn e inventário acompanhado independentemente da prontidão da UI.
- Diário preservado na morte e descarte manual bloqueado.
- 205 regressões e self-tests com servidor oficial aprovados; sequência completa de respawn no cliente ainda pendente.

# 0.3.2 (15/10/2026)

- Corrigida desconexão CustomUI: largura da barra enviada como objeto Anchor completo.
- Teste de serialização do comando com API real. HUD confirmado no cliente: painel de missão e barra acima da vida/mana, sem desconexão.

# 0.3.1 (15/10/2026)

- Corrigida pedra recolhida do chão; objetivos em linhas separadas.
- XP por abates, barra acima da hotbar e livro Diário para abrir status.
- Navegação para alvos reais carregados e pontos de interesse oficiais; orientação textual quando não localizados.
- Atualização do instalador com backup das versões anteriores.
- Atalho Shift+C pendente por ausência de API de teclado no servidor.

# Changelog — 0.3.0 (15/10/2026)

- Adapter JavaPlugin real para Hytale Server 0.7.2, revisão d769dc3d57881261a62858e1394f0e4de9a21eca, bytecode Java 25.
- Comandos públicos de classe, status, investimento, diário e resgate; menu visual, HUD e textos pt-BR.
- Eventos ECS de fabricação concluída, alteração de inventário, ataque, dano, morte atribuída ao jogador; amostragem de posição/ambiente/instância no thread do mundo.
- Catálogo gerado do Assets.zip legítimo: 3.960 definições estáticas auditadas, famílias herdadas, objetivos, kits e recompensas.
- Kits persistidos na fila de entrega uma vez; um item de adagas nativo representa as duas lâminas. O arco atual usa Shortbow.
- Bônus ofensivo congelado no início do ataque, preservando os modificadores nativos e o equipamento do projétil no disparo.
- Vida por modificador nativo; resistência universal; proteção condicionada ao bloqueio com espada+escudo; mana condicionada a runa equipada.
- Acesso inicial do Mago via introdução e matriz de runas oficiais; outras classes mantêm o desbloqueio normal.
- Salvamento schema 3, gravação atômica, validação de saves, fatos de chefes persistentes e conclusão automática das etapas 59/60.
- Recompensas incertas após interrupção ficam em quarentena para não serem repetidas automaticamente.
- Corrigidos XP ao reduzir maxLevel, overflow de ganho, seletor UI com sublinhado e confirmação de evento UI inválido.
- Build PowerShell e Gradle; 201 verificações de regressão; testes com servidor real e teste de interface no cliente.

**Ainda não é versão final:** velocidade de ataque e Fé não têm efeito aplicado pelo adapter; conjuração e campanha completa até os chefes ainda exigem validação jogando. Ver relatório de testes.
