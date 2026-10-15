# Luti RPG — integração 0.3.3 para Hytale 0.7.2

**ESTADO: build de integração instalável para testes, ainda não é versão final.** JAR compilado e carregado no Hytale 0.7.2 instalado. Painel, escolha de Mago, kit, reentrada, primeiras missões, pontos, vida, mana e resgate foram conferidos no cliente. Velocidade de ataque e efeito de Fé ainda não estão aplicados; a conjuração e a campanha completa até os chefes não foram aprovadas em uma partida. Consulte `RELATORIO_DE_TESTES.md`.

O projeto permanece independente de outros mods. O servidor e os assets oficiais usados localmente não são incluídos no JAR.

## Regras fechadas em conversa

- Mundo original do Hytale, progressão de missões sem NPCs novos. Diário único (UI ou `/rpg quests`) acompanha objetivos automaticamente; XP + itens úteis ao terminar. Recompensas pendentes quando inventário está cheio.
- Cinco classes **Guerreiro/Bárbaro, Paladino, Mago, Assassino e Arqueiro/Atirador**. Sem restrição de uso de arma, armadura ou magia: QUALQUER classe pode equipar o que encontrar.
- Especializações ofensivas: Guerreiro somente **armas de duas mãos**; Paladino **espada de uma mão + escudo**; Mago **magias de runas**; Assassino **adagas**; Arqueiro **arcos e bestas**. Equipamento fora da especialização usa dano/comportamento **padrão do Hytale**, sem buffs ofensivos RPG da classe. A detecção de arma deve ser baseada nos dados reais do item (e mãos / combo), não no nome exibido.
- Atributos universais (todas classes): **Força, Vitalidade, Resistência**. Classe libera **mais dois**: Guerreiro Fúria e Potência; Paladino Proteção e Fé; Mago Poder Arcano e Mana; Assassino Agilidade e Precisão; Arqueiro Destreza e Pontaria.
- Força universal existe no painel, porém bônus ofensivo escalonado para armas só entra em combate de especialização (força não transforma uma espada qualquer em arma poderosa). Vitalidade (vida) e Resistência (mitigação) operam com todas armas. Magia de outros personagens usa apenas efeito original sem bônus de atributos mágicos.
- 5 pontos *livres entre os cinco atributos permitidos* após cada subida de nível. Nível 1 ao 50, **245 pontos** totais por level-up. Sem bônus de pontos automáticos por classe. Limite de nível será configurável para futura expansão preservando saves.
- Kits iniciais: Guerreiro machado **duas mãos** básico; Paladino espada básica + escudo básico; Arqueiro arco básico + 50 flechas; Assassino duas adagas; Mago uma runa inicial. Kit entregue **uma única vez** por personagem, após seleção da classe. A runa do mago precisa existir na versão instalada e respeitar a estação/ativação oficial; se não puder equipar de início, entregar runa válida e instruir o jogador.
- Mago começa com acesso à sua runa. Outras classes *também podem obter runas* por exploração/progressão e usar magias nativas do Hytale; sem perks exclusivos de mago.
- 60 missões planejadas, da sobrevivência até o encerramento do Capítulo 1; etapas e IDs de conteúdo avançado devem ser confirmados contra o jogo instalado. Sem adicionar outros mods; nada de NPC. Recompensas proporcionais ao progresso (evitar pular tiers) e sem farm ilimitado.
- Classes podem ter estética/identidade própria; **o que altera poder são pontos aplicados nos atributos permitidos e a especialização de equipamento**, não bônus automáticos de classe.

## Integração atual

`LutiRpgPlugin` registra comandos e sete sistemas ECS reais. `RpgService` controla progresso e fila de entregas; `RpgSystems` integra combate, fabricação, inventário e ambientes; `RpgPage`/`RpgHud` fornecem interface. `RpgCore` preserva as regras das cinco classes. Saves schema 2 continuam legíveis; novas gravações usam schema 3 com fatos e entregas incertas.

`tools/audit-assets.ps1` gera `items.properties`, `targets.properties`, `families.tsv` e `canonical-items.txt` diretamente do Assets.zip legítimo. As famílias herdadas dos itens determinam a especialização, sem analisar o nome traduzido. Os 60 registros originais de `quests.tsv` foram preservados. O arco básico atual é `Weapon_Shortbow_Crude`; um único `Weapon_Daggers_Crude` é o par de duas lâminas. Recompensas tardias de runa foram mapeadas para fragmentos para evitar distribuir runas completas repetidas.

Coleta significa possuir a quantidade requerida, sem somar novamente ao mover a mesma pilha. Forno e fogueira não têm fabricação atribuída ao jogador; barras de cobre/ferro e comida preparada são reconhecidas pela presença do resultado processado. Exploração usa ambientes e instâncias oficiais; as metas de deslocamento exigem 64 e 1.024 blocos desde a ativação, no mesmo mundo. A equivalência entre alguns ambientes e os títulos do rascunho ainda depende de conferir a jornada completa.

O Mago recebe a Bola de Fogo uma vez. `/rpg runas` abre a introdução e matriz oficiais usando `AbilityBenchIntro` e `OpenAbilityBenchInteraction`; esse fluxo e a colocação da runa no slot foram testados. Outras classes mantêm a descoberta oficial no Templo Esquecido e podem usar qualquer runa obtida normalmente. A conjuração efetiva ainda não foi confirmada.

Entregas têm verificação de espaço e transação nativa all-or-nothing. Kit e prêmios ficam pendentes até o resgate. Uma interrupção entre inventário e save não tem transação atômica conjunta na API: a entrega reservada fica em `uncertain.N` e não é repetida automaticamente. O administrador precisa conferir o inventário/backup antes de recuperar essa entrega; não há promessa de entrega exatamente uma vez em queda de energia.

## Atualização 0.3.3

- Alimentos: maçãs, frutas vermelhas, outras frutas e vegetais comestíveis agora usam a marca Consumable herdada dos assets oficiais; sementes e plantas não comestíveis não contam. Eram ignorados pelo filtro de nomes Food_.
- XP por abate: animais recebem 10% da vida máxima (mínimo 5); hostis do catálogo recebem 25% (mínimo 10). Arredondamento ao inteiro mais próximo, máximo 250 por criatura comum. Chefes reconhecidos continuam em 500 e transições do Gobliterator em zero. Vida máxima ausente/inválida usa referência 100. Vida máxima é uma aproximação da força, não mede dano/IA. Ganho efetivo também aparece no chat.
- Crédito do atacante capturado por UUID em golpes válidos, consumido uma vez na morte e expirado após 30 segundos. Referências de entidade substituídas no respawn não são usadas como identidade persistente. Outro atacante direto ou dano ambiental limpa o crédito anterior.
- Após respawn, força reconstrução do HUD; a leitura de inventário independe da prontidão da interface do cliente. Nenhum reset de save é feito.
- Diário: DropOnDeath=false e cancelamento de DropItemEvent.PlayerRequest e DropItemEvent.Drop para esse item. Outros itens continuam com o comportamento normal. Transferências para baús não são bloqueadas.
- 205 verificações de regressão e testes com API real aprovados. Esta rodada ainda não confirmou a sequência completa morrer/renascer/matar no cliente; o relato de falha pós-morte motivou o reforço de identidade e ciclo do HUD, mas não deve ser tratado como reprodução conclusiva.
## Atualização 0.3.2

- Pedrinhas do chão (`Rubble`, exceto gelo) e rocha contam para Pedra. A missão continua sequencial: 2/60 significa missão atual número 2, não quantidade de pedras.
- Objetivos aparecem em linhas separadas, no formato `1/20 Pedra`, sem alterar as 60 missões nem acrescentar um objetivo de Pele inexistente.
- Abate atribuído ao jogador concede XP mesmo fora da missão: 25 por hostil do catálogo, 10 por outras criaturas e 500 pelos chefes finais reconhecidos. Transições do Gobliterator não dão XP. Projéteis conservam atribuição nativa ao atirador. Mantidos 5 pontos por nível.
- Barra dourada de XP central acima da hotbar, com XP do nível atual/próximo nível.
- Um livro **Diário Luti RPG** é enfileirado uma vez para personagens novos e existentes. Resgate por `/rpg resgatar`, coloque na hotbar e clique com qualquer botão para abrir o painel. Não dá atributos nem altera o kit de classe.
- Seta relativa à câmera para itens soltos e inimigos da missão carregados até 128 blocos; ponto de interesse usa coordenadas oficiais do mapa. Objetivos sem alvo localizado exibem orientação textual. Ainda não procura blocos de minério/pedra colocados, biomas ou masmorras não localizadas.
- Shift+C é a preferência solicitada, mas não está implementado: a API do servidor instalada não expõe registro de atalhos arbitrários. Use o livro ou `/rpg`.

## Uso e instalação

O arquivo é `build/libs/LutiRPG-0.3.3.jar`. Com o mundo/servidor fechado:

```powershell
./install.ps1 -ModsDirectory 'CAMINHO_DO_MUNDO/mods' -Enable
```

Sem `-Enable`, habilite o mod em **Editar Mundo > Mods**. O instalador faz backup do JAR/configuração que substituir. Não instale duas versões simultâneas. A build recusa versões diferentes de 0.7.2 no manifest.

Comandos: `/rpg`, `/rpg classe NOME`, `/rpg status`, `/rpg add ATRIBUTO QUANTIDADE`, `/rpg missao`, `/rpg resgatar` e `/rpg runas`. O menu aplica um ponto por clique. O reset requer a permissão `lutirpg.admin.reset` e a confirmação literal `APAGAR-CLASSE-PONTOS-MISSOES`; não concede outro kit.

Configuração: `mods/br.com.lutiplay_LutiRPG/config.properties`, com `maxLevel`, `damageScale`, `defenseScale` e `hud`. As escalas aceitam 0 a 1, preservando os tetos originais. Os cinco pontos por nível, atributos permitidos e kit único são regras fixas. Saves ficam em `players/UUID.properties` sob essa mesma pasta.

## Compilação e teste

Servidor identificado: **0.7.2**, revisão `d769dc3d57881261a62858e1394f0e4de9a21eca`, Java 25. `libs/HytaleServer.jar` vem da instalação local e não deve ser redistribuído. JDK usado em `tools/jdk/` é ferramenta local, não parte do mod.

```powershell
./tools/audit-assets.ps1
./build.ps1 -JavaHome 'CAMINHO_DO_JDK_25'
./test-server.ps1 -JavaHome 'CAMINHO_DO_JDK_25'
# Alternativa: Gradle 9.2.0 com JAVA_HOME no JDK 25
gradle --no-daemon check jar
```

Logs em `build/reports/`. `test-server.ps1` usa um servidor separado em `test-server/` e encerra após os testes; não abre os mundos do usuário. A validação visual foi feita somente no mundo **LutiRPG - Teste Codex**. Os mundos existentes não foram editados.

Referências oficiais: [API Java](https://docs.hytale.com/api/), [documentação](https://docs.hytale.com/) e assinaturas/assets da instalação legítima. Não foi copiado código de outros mods RPG.

## Atualização 0.3.4 — urso e diagnóstico após respawn
- Bear_Grizzly, Bear_Polar e Bear_Voidtaken incluídos no catálogo kill.hostile_basic a partir dos IDs oficiais. Não entram no objetivo específico das cavernas. Marcadores invisíveis continuam excluídos. Urso pardo com vida máxima 124 concede 31 XP pela regra existente.
- Diagnóstico no log LutiRPG.Diagnostics: RESPAWN identifica renascimento; DEATH registra role, jogador atribuído, crédito em cache e classificação hostil; PROGRESS registra avanço de objetivo.
- 205 verificações de regressão e teste isolado com servidor/assets oficiais aprovados, incluindo classificação de ursos e XP. Não reproduzida nem declarada resolvida a falha intermitente após morrer. Nenhum save alterado. A sequência morrer/renascer/abater/coletar permanece em teste no cliente.

