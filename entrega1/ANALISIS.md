# Entrega 1 – Análisis y Diseño
## Simulador distribuido de Estadio de Fútbol

---

## 1. Descripción general

El sistema simula un estadio de fútbol durante un partido. El estadio está **distribuido en 3 servidores**, uno por piso:

| Servidor | Zona | Recursos |
|---|---|---|
| **S1 – Planta baja** | Cancha + gradas bajas (boleto **General**) | Entrada (torniquetes), asientos numerados, tienda (mostrador + cajas), baño general, escaleras |
| **S2 – Palcos** | Palcos privados (boleto **Palco**) | Acceso con guardia, palcos con baño individual, cocina, bandejas, montacargas |
| **S3 – Grada alta** | Zona popular (boleto **Popular**) | Asientos numerados, tienda (mostrador + cajas), baño general |

La **única entrada** al estadio está en S1. Los aficionados con boleto Palco o Popular suben por la escalera y **migran** (por sockets TCP) al servidor de su piso. Al salir, bajan y migran de regreso a S1.

El sistema recibe diferentes tipos de agentes: **aficionados, guardias, cocineros, meseros, vendedores de gradas** y un **reloj del partido**. El número de cada uno se configura al iniciar.

### Procesos que se simulan (contadores visibles)
- Cuántos aficionados están en fila de la entrada y cuántos ya entraron.
- Cuántos boletos fueron rechazados.
- Cuántos aficionados hay en cada piso, sentados viendo el partido.
- Cuántos están usando el baño y cuántos esperan por él.
- Cuántas porciones de comida hay en cada mostrador y cuántos esperan comprar.
- Cuántos pedidos de palco están pendientes y cuántos meseros están entregando.
- Cuántos aficionados están en la escalera y en qué sentido.
- Marcador y tiempo del partido.

> Igual que en el ejemplo de patinaje, cada aficionado **cambia de estado** durante la simulación: a veces es espectador, a veces va al baño, a veces compra, y al final se va.

---

## 2. Búferes (recursos compartidos)

| Letra | Búfer | Piso | Capacidad | Tipo | Mecanismo |
|---|---|---|---|---|---|
| **T** | Torniquetes de entrada | S1 | T (configurable) | Zona crítica búfer > 1 | `Semaphore(T, true)` |
| **E** | Escalera | S1↔S2, S1↔S3 | E personas, un sentido a la vez | Zona crítica búfer > 1 con condición | Monitor: `ReentrantLock` + `Condition` |
| **A** | Asientos numerados | S1, S3 | 1 por asiento | Sección crítica búfer 1 (por asiento) | `synchronized` sobre cada asiento |
| **Bg** | Baño general (cubículos) | S1, S3 | N cubículos | Zona crítica búfer > 1 | `Semaphore(N, true)` |
| **Bp** | Baño de palco | S2 | **1** por palco | **Sección crítica búfer 1** | `ReentrantLock` / `synchronized` |
| **M** | Mostrador de comida | S1, S2, S3 | K porciones | Productor-consumidor | Monitor propio: `synchronized` + `wait()/notifyAll()` |
| **Cj** | Cajas de cobro | S1, S3 | C cajas | Zona crítica búfer > 1 | `Semaphore(C)` |
| **Pd** | Cola de pedidos de palco | S2 | P pedidos | Productor-consumidor | Monitor propio |
| **Ll** | Cola de llamados al vendedor | S1, S3 | L llamados | Productor-consumidor | Monitor propio |
| **Bj** | Bandejas de meseros | S2 | B bandejas | Zona crítica búfer > 1 | `Semaphore(B)` |
| **Mc** | Montacargas | S2 | **1** | **Sección crítica búfer 1** | `ReentrantLock` |
| **Ch** | Charola del vendedor | S1, S3 | Ch porciones | Recurso propio (no compartido) | — |
| — | Contadores y lista de agentes (GUI) | todos | — | Datos compartidos con el EDT | `AtomicInteger`, `ConcurrentHashMap` |

### Interacciones especiales
- **Guardia ↔ Aficionado (cita/rendezvous):** el aficionado avisa "llegué" (`release` de `aficionadoListo`) y espera el veredicto (`acquire` de `veredicto`). El guardia espera al aficionado, revisa el boleto y libera el veredicto.
- **Escalera de un solo sentido:** si hay gente subiendo, quien quiere bajar espera (`await`), y viceversa. Para evitar **inanición**, después de cierto número de personas en un sentido se da turno al otro.
- **Prevención de deadlock (Mesero):** el mesero necesita **una bandeja (Bj) y el montacargas (Mc)**. Todos los meseros los piden **en el mismo orden** (primero Bj, luego Mc), lo que rompe la espera circular. Habrá un modo de demostración que invierte el orden en la mitad de los meseros para mostrar el deadlock.

---

## 3. Eventos

| Evento | Quién lo genera | Quién lo recibe | Efecto |
|---|---|---|---|
| Llegada de aficionado | Generador de S1 | Guardia | El guardia sale de su espera y revisa boleto |
| Boleto válido / inválido | Guardia | Aficionado | Pasa a su zona o es expulsado |
| Aficionado migra | Aficionado (S1/S2/S3) | `ServidorRed` del piso destino | Se recrea el hilo en el otro servidor |
| **Inicio del partido** | Reloj (S1) | Todos los pisos (difusión por red) | Sonido de silbato; aficionados se sientan |
| **Gol** | Reloj (S1) | Todos los pisos | Sonido; aficionados en asiento pasan a *Celebrando* |
| **Medio tiempo** | Reloj (S1) | Todos los pisos | Alta probabilidad de ir al baño o a comprar → **contención** |
| **Fin del partido** | Reloj (S1) | Todos los pisos | Aficionados salen; el personal sale cuando ya no hay aficionados |
| Mostrador con comida / con espacio | Cocinero / Aficionado / Vendedor / Mesero | Hilos esperando en **M** | `notifyAll()` despierta a productores o consumidores |
| Nuevo pedido | Aficionado de palco | Meseros | Un mesero despierta y lo toma |
| Pedido entregado | Mesero | Aficionado de palco | El aficionado deja de esperar y come |
| Nuevo llamado | Aficionado (S1/S3) | Vendedores de gradas | Un vendedor va a su asiento |
| Venta hecha | Vendedor de gradas | Aficionado | El aficionado deja de esperar y come |
| Timeout (sin clientes) | Reloj interno del agente | Guardia / Vendedor / Mesero | Pasa a zona de decisión (descansar, seguir o salir) |

---

## 4. Secciones críticas

| # | Sección crítica | Qué se protege | Qué pasaría sin protección |
|---|---|---|---|
| 1 | Entrar a un torniquete | Número de torniquetes libres | Más de T personas pasando a la vez |
| 2 | Revisar boleto | Cita guardia ↔ aficionado | Dos guardias revisan al mismo aficionado, o un guardia revisa a dos a la vez |
| 3 | Subir/bajar escalera | Cantidad de personas y sentido actual | Gente chocando en ambos sentidos; más de E en la escalera |
| 4 | Ocupar asiento | Estado libre/ocupado del asiento | Dos aficionados sentados en el mismo lugar (boleto duplicado) |
| 5 | Usar baño general | Cubículos libres | Más de N personas en el baño |
| 6 | Usar baño de palco | El único baño del palco | Dos personas en el mismo baño |
| 7 | Poner/tomar del mostrador | Número de porciones (0…K) | Porciones negativas o más de K; comida "duplicada" |
| 8 | Pagar en caja | Cajas libres | Más de C cobrando a la vez |
| 9 | Encolar/tomar pedido de palco | Cola de pedidos | Pedidos perdidos o entregados dos veces |
| 10 | Encolar/tomar llamado | Cola de llamados | Dos vendedores al mismo aficionado |
| 11 | Usar bandeja + montacargas | Bandejas libres y el montacargas | **Deadlock** si se piden en distinto orden |
| 12 | Actualizar contadores / lista de agentes | Datos que lee la GUI | Contadores incorrectos, `ConcurrentModificationException` |

---

## 5. Modelado de los agentes

### 5.1 Aficionado

Tiene un **boleto** (General, Palco o Popular; algunos pueden ser falsos) y niveles de **hambre** y **vejiga** que suben con el tiempo.

1. Caminar hacia la entrada (S1).
2. Estar en fila del torniquete **[T]**.
3. Ser revisado por el guardia (cita):
   a) Boleto inválido → caminar para salirse.
   b) Boleto válido → zona de decisión según boleto.
4. Zona de decisión según boleto:
   a) **General** → caminar a su asiento (S1).
   b) **Palco** → caminar a la escalera hacia S2.
   c) **Popular** → caminar a la escalera hacia S3.
5. Subir escalera **[E]** → **migrar** al servidor destino.
   a) En S2: ser revisado por el guardia de palcos (mismo flujo que el paso 3).
6. Buscar y ocupar su asiento / palco **[A]**.
7. Estar viendo el partido (tiempo determinado). Después puede:
   a) Seguir viendo el partido.
   b) Celebrar (evento **gol**) → volver a ver el partido.
   c) Ir al baño (si la vejiga está alta o es **medio tiempo**).
   d) Ir a comprar comida a la tienda (S1/S3).
   e) Llamar al vendedor de gradas (S1/S3).
   f) Hacer pedido al mesero (S2).
   g) Caminar para salirse (evento **fin del partido** o aleatorio).
8. Ir al baño:
   a) S1/S3: esperar en la fila y usar un cubículo **[Bg]**.
   b) S2: esperar y usar el baño del palco **[Bp]**.
   c) Regresar a su asiento → paso 7.
9. Comprar en la tienda:
   a) Esperar caja libre y pagar **[Cj]**.
   b) Tomar porción del mostrador **[M]**; si está vacío, espera.
   c) Regresar a su asiento y comer → paso 7.
10. Llamar al vendedor:
    a) Dejar llamado en la cola **[Ll]**.
    b) Esperar al vendedor; si tarda demasiado, va a la tienda → paso 9.
    c) Comprarle y comer → paso 7.
11. Hacer pedido al mesero (S2):
    a) Dejar pedido en la cola **[Pd]**.
    b) Esperar la entrega.
    c) Comer → paso 7.
12. Caminar para salirse:
    a) Si está en S2/S3: bajar escalera **[E]** → **migrar** a S1.
    b) Salir por la entrada de S1.

**Estados del aficionado:**
`CAMINANDO_A_ENTRADA, EN_FILA_TORNIQUETE, SIENDO_REVISADO, RECHAZADO, CAMINANDO_A_ESCALERA, ESPERANDO_ESCALERA, EN_ESCALERA, MIGRANDO, BUSCANDO_ASIENTO, VIENDO_PARTIDO, CELEBRANDO, EN_FILA_BANO, EN_BANO, EN_FILA_CAJA, PAGANDO, ESPERANDO_COMIDA, COMIENDO, ESPERANDO_VENDEDOR, ESPERANDO_MESERO, SALIENDO, FUERA`

### 5.2 Guardia (entrada S1 y acceso a palcos S2)

1. Caminar hacia su puesto (torniquete o acceso a palcos).
2. Estar en el puesto:
   a) Espera un tiempo; si nadie llega, pasa a la zona de decisión.
   b) Llega notificación (evento) de un aficionado → pasa a revisar boleto.
3. Revisar boleto:
   a) Válido y de la zona correcta → abrir torniquete / dejar pasar.
   b) Falso o de otra zona → rechazar al aficionado.
   c) Pasa a la zona de decisión.
4. Zona de decisión:
   a) Continuar en el puesto.
   b) Ir a tomar descanso; después de un rato regresa a la zona de decisión.
   c) Caminar para salirse (fin del partido y ya no hay aficionados en la entrada).
5. Caminar para salirse → salir.

**Estados:** `CAMINANDO_A_PUESTO, ESPERANDO_AFICIONADO, REVISANDO_BOLETO, DESCANSANDO, SALIENDO, FUERA`

### 5.3 Cocinero (S1, S2, S3)

1. Caminar hacia la cocina.
2. Preparar una porción (tiempo determinado).
3. Colocar la porción en el mostrador **[M]**:
   a) Si el mostrador está lleno, espera (`wait`) hasta que haya espacio.
   b) Si hay espacio, la coloca y notifica (`notifyAll`) que ya hay comida.
4. Zona de decisión:
   a) Seguir preparando → paso 2.
   b) Ir a tomar descanso → zona de decisión.
   c) Caminar para salirse (fin del partido).
5. Caminar para salirse → salir.

**Estados:** `CAMINANDO_A_COCINA, PREPARANDO, ESPERANDO_ESPACIO, COLOCANDO, DESCANSANDO, SALIENDO, FUERA`

### 5.4 Mesero (S2)

1. Caminar hacia la cocina de palcos.
2. Estar en la cocina esperando pedidos **[Pd]**:
   a) Espera un tiempo; si no hay pedidos, pasa a la zona de decisión.
   b) Llega un pedido → lo toma.
3. Tomar una bandeja **[Bj]**; si no hay, espera.
4. Recoger comida del mostrador de cocina **[M]**; si está vacío, espera.
5. Usar el montacargas **[Mc]** para subir al nivel de palcos; si está ocupado, espera.
   *(Orden fijo: primero Bj, luego Mc → evita deadlock.)*
6. Entregar el pedido en el palco y notificar al aficionado.
7. Bajar en el montacargas, liberarlo **[Mc']** y devolver la bandeja **[Bj']**.
8. Zona de decisión:
   a) Seguir atendiendo → paso 2.
   b) Ir a tomar descanso → zona de decisión.
   c) Caminar para salirse (fin del partido y sin pedidos pendientes).
9. Caminar para salirse → salir.

**Estados:** `CAMINANDO_A_COCINA, ESPERANDO_PEDIDO, ESPERANDO_BANDEJA, ESPERANDO_COMIDA, ESPERANDO_MONTACARGAS, ENTREGANDO, REGRESANDO, DESCANSANDO, SALIENDO, FUERA`

### 5.5 Vendedor de gradas (S1, S3)

1. Caminar hacia la tienda.
2. Recargar la charola tomando porciones del mostrador **[M]**; si está vacío, espera.
3. Recorrer las gradas esperando llamados **[Ll]**:
   a) Espera un tiempo; si no hay llamados, sigue recorriendo o pasa a la zona de decisión.
   b) Llega un llamado → caminar al asiento del aficionado.
4. Vender al aficionado (resta 1 de su charola y le notifica).
   a) Si la charola queda vacía → regresar a la tienda (paso 1).
   b) Si aún tiene comida → seguir recorriendo (paso 3).
5. Zona de decisión:
   a) Seguir vendiendo.
   b) Ir a tomar descanso.
   c) Caminar para salirse (fin del partido).
6. Caminar para salirse → salir.

**Estados:** `CAMINANDO_A_TIENDA, RECARGANDO, ESPERANDO_COMIDA, RECORRIENDO_GRADAS, CAMINANDO_A_CLIENTE, VENDIENDO, DESCANSANDO, SALIENDO, FUERA`

### 5.6 Reloj del partido (S1)

1. Esperar a que se llene el estadio (un tiempo o un % de aficionados dentro).
2. Emitir evento **inicio** (silbato) → difundir a S2/S3.
3. Primer tiempo (tiempo determinado); aleatoriamente emitir **gol**.
4. Emitir **medio tiempo** → descanso (tiempo determinado).
5. Segundo tiempo; aleatoriamente emitir **gol**.
6. Emitir **fin del partido** → difundir a S2/S3.
7. Terminar.

**Estados:** `ESPERANDO_INICIO, PRIMER_TIEMPO, MEDIO_TIEMPO, SEGUNDO_TIEMPO, TERMINADO`

### 5.7 Hilo de red – ServidorRed (S1, S2, S3)

1. Abrir `ServerSocket` en el puerto del piso.
2. Esperar conexión (`accept`, estado **WAITING/BLOCKED en I/O**).
3. Leer mensaje:
   a) **AGENTE** → deserializar `AgenteDTO`, crear el hilo del agente en este piso e iniciarlo.
   b) **EVENTO** (inicio/gol/medio tiempo/fin) → notificar a los agentes locales y reproducir sonido.
4. Volver al paso 2 hasta que termine la simulación.

---

## 6. Sistema general (programa principal, igual en cada servidor)

1. Iniciar el simulador (`main`) con argumentos `--piso`, `--puerto`, `--vecinos`.
2. Configurar entradas (diálogo: número de agentes de cada tipo, capacidades de los búferes).
3. Crear los recursos del piso.
4. Iniciar en paralelo:
   - Hilo **ServidorRed**.
   - Agentes **Aficionados** (solo S1 los genera; en S2/S3 llegan por red).
   - Agentes **Guardias** (S1, S2).
   - Agentes **Cocineros**.
   - Agentes **Meseros** (S2) / **Vendedores de gradas** (S1, S3).
   - **Reloj del partido** (solo S1).
   - Ventana gráfica y tabla de estados (**EDT**).
5. Esperar a que terminen (join).
6. Procesos finales: estadísticas, cerrar sockets.

---

## 7. Estados de los hilos (Java)

La tabla de estados mostrará, además del estado del dominio, el **`Thread.State`** real de cada agente:

| `Thread.State` | Cuándo se ve en el simulador |
|---|---|
| `NEW` | Agente creado pero aún no iniciado (antes de `start()`, o recién recreado al migrar) |
| `RUNNABLE` | Caminando, preparando comida, revisando boleto |
| `BLOCKED` | Esperando entrar a un método `synchronized` (asiento, mostrador) |
| `WAITING` | En `wait()` del mostrador, `acquire()` de un semáforo, `await()` de la escalera |
| `TIMED_WAITING` | `sleep()` mientras ve el partido, usa el baño o descansa; esperas con timeout |
| `TERMINATED` | Agente que salió del estadio o que migró a otro servidor |

---

## 8. Diagramas BPMN

En `entrega1/bpmn/` (abrir con [diagrams.net](https://app.diagrams.net) → *File → Open from → Device*):

| Archivo | Contenido |
|---|---|
| `00_sistema_general.drawio` | Programa principal de cada servidor |
| `01_aficionado.drawio` | Aficionado |
| `02_guardia.drawio` | Guardia |
| `03_cocinero.drawio` | Cocinero |
| `04_mesero.drawio` | Mesero |
| `05_vendedor_gradas.drawio` | Vendedor de gradas |
| `06_reloj_partido.drawio` | Reloj del partido |
| `07_servidor_red.drawio` | Hilo de red / migración |

En `entrega1/bpmn/png/` está una imagen de cada diagrama para pegarla directamente en el documento de entrega.

Notación (igual al ejemplo de clase): ○ inicio, ● fin, ◇ decisión, ✚ paralelo, recuadro punteado con ⏱ = actividad con tiempo, ⏱ = evento temporizador, ⓑ = acceso a búfer (letra según la tabla de la sección 2; con apóstrofo `'` = liberar).
