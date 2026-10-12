# language: pt
Funcionalidade: Transcrição offline de gravações
  Como usuário do Gravasom, quero transcrever minhas gravações sem internet,
  escolhendo o idioma, podendo cancelar quando quiser e vendo o texto
  junto da gravação — tudo dentro do aparelho.

  Contexto:
    Dado que o app não tem permissão INTERNET no manifesto
    E a transcrição roda com o modelo do idioma escolhido já baixado no aparelho

  # --- História 1: transcrever sem internet ---------------------------------

  Cenário: usuário liga "Transcrever ao gravar", grava e vê o texto
    Dado que estou na tela Gravar
    E o toggle "Transcrever ao gravar" está desligado
    Quando eu ligo o toggle "Transcrever ao gravar"
    E gravo 15 segundos
    E paro a gravação
    Então a gravação aparece na lista com o rótulo "Transcrevendo…"
    E quando a transcrição termina
    Então a gravação passa a mostrar o rótulo "Transcrita"
    E ao abrir a gravação existe a aba "Transcrição"
    E o texto transcrito aparece em português

  Cenário: transcrição não usa rede em nenhum momento
    Dado que o aparelho está em modo avião
    E o modelo de português já está baixado
    Quando eu transcrevo uma gravação de 1 minuto
    Então a transcrição termina com sucesso
    E nenhuma requisição de rede é feita pelo app

  Cenário: modelo não baixado bloqueia a transcrição com aviso claro
    Dado que nenhum modelo de idioma foi baixado
    Quando eu ligo o toggle "Transcrever ao gravar"
    Então o app mostra um diálogo pedindo para baixar o modelo de português
    E a transcrição não começa até o download terminar

  # --- História 2: escolher o idioma ----------------------------------------

  Cenário: usuário escolhe o idioma da transcrição
    Dado que estou em Ajustes → Transcrição
    E os modelos de português e inglês estão baixados
    Quando eu escolho "Inglês (Estados Unidos)" como idioma padrão
    E transcrevo uma gravação
    Então a transcrição usa o modelo de inglês
    E o campo "language" da transcrição é "en-US"

  Cenário: gerenciar modelos mostra o que está baixado e o que falta
    Dado que estou em Ajustes → Transcrição → Gerenciar modelos
    Então a lista mostra "Português (Brasil)", "Inglês (Estados Unidos)", "Espanhol (Espanha)" e "Francês (França)"
    E cada idioma mostra o tamanho do modelo e se está baixado ou não
    E o botão "Baixar" de cada idioma mostra o progresso enquanto baixa

  Cenário: mesmo áudio em dois idiomas
    Dado que uma gravação já tem transcrição em português
    Quando eu peço para transcrever essa gravação também em inglês
    Então as duas transcrições ficam guardadas
    E a aba "Transcrição" permite alternar entre os dois idiomas

  # --- História 3: cancelar a transcrição em andamento ----------------------

  Cenário: usuário cancela a transcrição em andamento
    Dado que uma transcrição de 10 minutos está em andamento
    E a notificação mostra "Transcrevendo…"
    Quando eu abro a notificação e toco em "Cancelar"
    Então a transcrição para em até 2 segundos
    E o rótulo volta para "Não transcrita"
    E o áudio original continua intacto e tocável
    E nenhum texto parcial fica salvo no banco

  Cenário: cancelar durante o processamento não corrompe outras transcrições
    Dado que existem 3 transcrições na fila
    Quando eu cancelo a do meio
    Então as outras duas continuam e terminam normalmente
    E a do meio fica com status "CANCELLED"
    E nenhuma linha em `transcripts` fica com texto parcial da cancelada

  Cenário: matar o app no meio da transcrição retoma do último flush
    Dado que uma transcrição está em andamento
    E o worker já salvou 10 frases no banco
    Quando o sistema encerra o processo do app
    E eu abro o app de novo
    Então a transcrição aparece como "Interrompida"
    E ao retomar, ela recomeça do último flush (não do zero)
    E as 10 frases salvas continuam corretas

  # --- Acessibilidade e privacidade -----------------------------------------

  Cenário: TalkBack anuncia o estado da transcrição
    Dado que estou na tela de detalhe com o TalkBack ligado
    Quando a transcrição está em andamento
    Então o TalkBack anuncia "Transcrevendo, X por cento"
    E quando termina anuncia "Transcrição concluída"

  Cenário: transcrição não sai do aparelho
    Dado que uma transcrição foi concluída
    Quando eu inspeciono o tráfego de rede do app
    Então nenhum byte com áudio ou texto é enviado
    E o app não tem permissão INTERNET no manifesto
