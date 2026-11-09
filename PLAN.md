# Plan del proyecto: Simulador distribuido de Estadio de Fútbol

## Contexto
Proyecto final de *Fundamentos de Programación en Paralelo* (ver `Descripcion/01_finalProject.pdf`). Requisitos:
- Simulador **gráfico** con varios agentes visibles concurrentemente, cada uno con comportamiento propio e interacción con otros.
- **Sonido**, número de agentes **configurable**, **tabla de estados** por agente.
- **Secciones críticas con búfer 1** y **zonas críticas con búfer > 1**.
- **Cliente-servidor**: algunos agentes **cambian de una computadora a otra**.
- Objetivo: usar **candados, monitores y semáforos**, **evitar deadlock**, e **ilustrar los estados de un thread**.
- Entregas: (1) Análisis y diseño: agentes, búferes, estados, eventos, secciones críticas (BPMN); (2) corrección + avance de código; (3) final.

Decisiones tomadas: **Java 17+**, **Swing**, **sockets TCP** para la distribución, **sin jugadores animados** (un hilo *Reloj del partido* genera los eventos).

---

## 1. Arquitectura distribuida

| Servidor | Zona | Contenido |
|---|---|---|
| **S1 – Planta baja** | Cancha + gradas bajas | Entrada con torniquetes, guardias, asientos numerados, tienda, baño general (N cubículos), escaleras |
| **S2 – Palcos** | Palcos privados | Acceso restringido (guardia revisa pase), K palcos con baño individual, cocina + meseros |
| **S3 – Grada alta (popular)** | Lugares baratos | Asientos, tienda, baño general, vendedores de gradas |

- El mismo código corre en los 3: `java -jar estadio.jar --piso=1 --puerto=5001 --vecinos=host2:5002,host3:5003`. Funciona en 3 PCs o en 3 procesos locales (para desarrollar).
- **Entrada única en S1** (como estadio real): todos entran por S1; quien tiene boleto PALCO o POPULAR sube por la escalera y **migra** a S2/S3.
- **Migración de agente**: al terminar de subir la escalera, el hilo local serializa su estado (`AgenteDTO implements Serializable`: id, tipo, boleto, asiento, estado, hambre, vejiga…), lo envía por **socket TCP** (`ObjectOutputStream`) y termina (`TERMINATED`). El servidor destino tiene un hilo `ServidorRed` que acepta conexiones, recrea el agente y lo arranca como hilo nuevo. Al salir del estadio el agente baja y migra de regreso a S1.
- Los eventos del partido (inicio, gol, medio tiempo, fin) los genera S1 y los **difunde** a S2 y S3 por la misma red.

## 2. Agentes (cada uno = un `Thread` con máquina de estados)

| Agente | Dónde | Comportamiento resumido | Interactúa con |
|---|---|---|---|
| **Aficionado** (General / Palco / Popular) | S1 → S2/S3 | Llega → fila de torniquete → guardia valida → (sube escaleras) → busca su asiento → ve el partido; aleatoriamente o por evento va al baño, compra comida, llama al vendedor o al mesero → sale | Guardia, Cocinero (mostrador), Mesero, Vendedor de gradas |
| **Guardia** | S1 (entrada), S2 (acceso a palcos) | Espera aficionado en su puesto, revisa boleto (válido / falso / zona equivocada), deja pasar o rechaza; toma descansos | Aficionado |
| **Cocinero** | S1, S2, S3 | Productor: prepara comida y la pone en el mostrador (búfer acotado) | Aficionado, Vendedor de gradas, Mesero |
| **Mesero** | S2 | Toma pedido de la cola de pedidos, recoge en cocina, sube en el montacargas, entrega al palco | Aficionado de palco, Cocinero |
| **Vendedor de gradas** | S1, S3 | Recarga su charola en el mostrador, recorre gradas, atiende a quien lo llama, vuelve a recargar | Aficionado, Cocinero |
| **Reloj del partido** | S1 | Emite eventos: inicio, gol, medio tiempo, fin; S1 los difunde a S2/S3 | Todos (eventos) |

> El **medio tiempo** es el evento clave: todos van al baño o a la tienda a la vez, y se ve la contención en semáforos y colas.

## 3. Recursos compartidos ↔ mecanismo de sincronización

| Recurso | Búfer | Mecanismo | Concepto demostrado |
|---|---|---|---|
| Baño del palco | **1** | `synchronized` / `ReentrantLock` | Sección crítica búfer 1, exclusión mutua |
| Baños generales S1/S3 | N cubículos | `Semaphore(N, true)` | Zona crítica búfer > 1, fairness |
| Torniquetes de entrada | T | `Semaphore(T)` + cita guardia↔aficionado (2 semáforos) | Rendezvous |
| Mostrador de comida | K porciones | **Monitor propio** (`synchronized` + `wait()/notifyAll()`) | Productor-consumidor clásico |
| Cajas de cobro | C | `Semaphore(C)` | Recurso múltiple |
| Cola de pedidos de palcos | P | Monitor propio (productor-consumidor) | Aficionados producen pedidos, meseros consumen |
| Cola de llamados a vendedores | L | Monitor propio | Aficionados producen llamados, vendedores consumen |
| Escalera | capacidad E, **un sentido a la vez** | Monitor con `ReentrantLock` + `Condition` | Problema del "puente de un carril", evitar inanición |
| Asientos numerados | 1 por asiento | `synchronized` por asiento | Asiento ocupado / boleto duplicado |
| Mesero: **bandeja + montacargas** | B bandejas, 1 montacargas | Orden global de adquisición (bandeja → montacargas) | **Prevención de deadlock**; un switch "modo deadlock" invierte el orden para mostrar el problema |
| Contadores / lista de agentes para la GUI | — | `ConcurrentHashMap`, `AtomicInteger` | Datos compartidos con el EDT |

## 4. GUI, tabla de estados y sonido
- **Swing**: cada servidor abre su ventana con un `JPanel` que dibuja su piso (zonas, asientos, agentes como iconos/círculos de colores) repintado con `javax.swing.Timer` (~30 fps). Los agentes **no** tocan Swing directamente; solo actualizan su posición/estado.
- **Tabla de estados** (`JTable`): id, tipo, estado del dominio (`EN_FILA_BANO`, `COMPRANDO`…), **`Thread.getState()`** real (`NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED`) y recurso que espera. Filas coloreadas por estado.
- **Panel de contadores**: personas en baño / en fila, comida disponible, en escaleras, sentados, etc.
- **Diálogo de configuración** al iniciar (`JSpinner`): # aficionados por tipo, guardias, cocineros, vendedores, meseros, cubículos, porciones del mostrador, % boletos falsos.
- **Sonido** (`javax.sound.sampled.Clip`, archivos .wav): silbato (inicio/medio tiempo/fin), grito de gol, murmullo de multitud en loop.

## 5. Estructura del código (un solo proyecto, arranca con `--piso=N`)
```
src/estadio/
  Main.java                  // lee args, muestra config, arranca piso
  comun/    Boleto, TipoBoleto, AgenteDTO, Mensaje, Config
  red/      ServidorRed (hilo listener), ConexionVecino (envío), Protocolo
  recursos/ BanoGeneral, BanoPalco, Mostrador, Cajas, Torniquetes,
            Escalera, Asientos, ColaPedidos, ColaLlamados, Montacargas
  agentes/  Agente (abstract extends Thread: estado, x, y, piso)
            Aficionado, Guardia, Cocinero, Mesero, VendedorGradas, RelojPartido
  pisos/    Piso (abstract: recursos + agentes locales), PisoBaja, PisoPalcos, PisoPopular
  gui/      VentanaPiso, PanelPiso, TablaEstados, PanelContadores, DialogoConfig, Sonido
resources/sonidos/*.wav, resources/img/*.png
```

**Compilación:** sin Maven ni Gradle. Un script `build.sh` compila con `javac --release 17` y empaqueta `estadio.jar` con `jar`. La JDK de desarrollo es la 27-ea; con `--release 17` el jar también corre en computadoras con Java 17 o más nuevo.

## 6. Scope por entregas

**Entrega 1 – Análisis y diseño (sin código)** → `entrega1/`
- Agentes, búferes, estados, eventos y secciones críticas (`entrega1/ANALISIS.md`).
- **BPMN por agente** (Aficionado, Guardia, Cocinero, Mesero, Vendedor de gradas, Reloj del partido) + BPMN del sistema general y del hilo de red (`entrega1/bpmn/*.drawio`).

**Entrega 2 – Avance (MVP: S1 y una migración)**
1. `Agente` base + tabla de estados con `Thread.getState()`.
2. Aficionado + Guardia + torniquetes, baño general (semáforo), mostrador productor-consumidor.
3. GUI de S1 con agentes moviéndose y contadores.
4. Red mínima: un aficionado sube la escalera y aparece en S3.

**Entrega final**
- S2 completo (palcos, baño búfer 1, meseros, demo de deadlock evitado), S3 con vendedores de gradas.
- Reloj del partido con eventos difundidos a los 3 servidores.
- Escalera de un sentido, regreso a S1 al salir, sonido, diálogo de configuración.
- Extras si sobra tiempo: boletos falsos/duplicados más elaborados, revendedor.

**Fuera de scope:** jugadores animados, compra de boletos en taquilla, base de datos, tolerancia a fallos de red.

## 7. Verificación
- Correr 3 procesos locales (`--piso=1/2/3` en puertos distintos) y luego en 3 PCs de la misma red.
- Revisar en la tabla que aparezcan `BLOCKED`/`WAITING` en baños y mostrador durante el **medio tiempo**.
- Invariantes con asserts/logs: ocupación de baño ≤ N, baño de palco ≤ 1, porciones en mostrador ∈ [0, K], nadie se sienta en un asiento ocupado, la escalera nunca tiene gente en ambos sentidos.
- Modo deadlock ON → meseros trabados; OFF → nunca se traba (correr varios minutos con muchos agentes).
- Agentes totales sumando los 3 servidores = agentes creados − agentes que salieron (ninguno se pierde en la migración).

## 8. Plan de acción por fases

El trabajo se divide en fases pequeñas, y cada una termina con algo que se puede ejecutar y comprobar.

**Reglas de trabajo**
- No se pasa a la siguiente fase hasta que la **prueba de cierre** de la fase actual funcione.
- Al cerrar cada fase se hace un commit con el título `Fase N: ...`.
- Si una fase se complica, se parte en dos antes que dejarla a medias.

| Fase | Objetivo | Qué se construye | Prueba de cierre |
|---|---|---|---|
| **0. Esqueleto** | Que compile y abra | Paquetes de la sección 5, `build.sh`, `Main` que lee `--piso/--puerto/--vecinos` y abre una ventana Swing vacía con el nombre del piso | `./build.sh && java -jar estadio.jar --piso=1` abre la ventana "S1 – Planta baja" |
| **1. Agentes y tabla de estados** | Base de todos los agentes | `Agente` abstracto (estado del dominio, x/y, piso), registro concurrente de agentes, `PanelPiso` que dibuja círculos, `TablaEstados` con `Thread.getState()` refrescada por un `javax.swing.Timer`; agentes de prueba que caminan y duermen | N agentes moviéndose; la tabla muestra `RUNNABLE`/`TIMED_WAITING` y `TERMINATED` al terminar |
| **2. Entrada de S1** | Primer semáforo y primera cita | `Torniquetes` (Semaphore T), cita guardia↔aficionado (2 semáforos), `Guardia`, `Aficionado` hasta su asiento, `Asientos` (`synchronized`), boletos falsos | Con T=2 nunca hay más de 2 en revisión (contador + aserción en el log); los rechazados salen; nunca dos aficionados en el mismo asiento |
| **3. Baño y tienda de S1** | Zona crítica y productor-consumidor | `BanoGeneral` (Semaphore N), `Mostrador` (monitor con wait/notifyAll), `Cajas`, `Cocinero`, zona de decisión del aficionado | Porciones del mostrador siempre en [0, K]; con 0 cocineros los aficionados aparecen en `WAITING`; el baño nunca pasa de N |
| **4. Red mínima y migración** | Primer cambio de computadora | `Mensaje`, `AgenteDTO`, hilo `ServidorRed`, `ConexionVecino`, escalera simple; un aficionado Popular pasa de S1 a S3 | En 3 terminales locales el aficionado desaparece de S1 y aparece en S3; aficionados en S1 + en S3 = los que entraron |
| **5. Grada alta (S3) completa** | Segundo piso funcional y regreso | Baño y tienda en S3, `VendedorGradas`, `ColaLlamados`, regreso a S1 para salir | Ciclo completo S1→S3→S1→salida; cada llamado lo atiende un solo vendedor |
| **6. Palcos (S2)** | Búfer 1 y deadlock | Guardia de palcos, `BanoPalco` (lock), `ColaPedidos`, `Mesero` con bandeja + montacargas en orden fijo, switch de "modo deadlock" | Con modo deadlock ON los meseros se traban (WAITING permanente en la tabla); con OFF corre varios minutos sin trabarse |
| **7. Partido y escalera real** | Eventos distribuidos | `RelojPartido`, difusión de eventos por red, `Escalera` como monitor de un sentido con `Condition` y anti-inanición | Gol y medio tiempo llegan a los 3 pisos; en el medio tiempo se ve el pico de `WAITING`; la escalera nunca tiene gente en ambos sentidos |
| **8. Presentación** | Lo que se ve y se oye | `DialogoConfig` (JSpinner), `Sonido` (.wav), sprites o iconos, `PanelContadores`, colores por estado en la tabla | Cambiar la configuración cambia la simulación; se escucha el silbato, el gol y la multitud |
| **9. Prueba final** | Listo para entregar | Prueba en 3 PCs reales, prueba de estrés (muchos agentes), reporte final y ensayo de la exposición | Pasa el checklist de requisitos del profesor |

- **Entrega 2 (avance de programación):** fases 0 a 4. Ya tiene S1 funcionando y una migración real.
- **Entrega final:** fases 5 a 9.

### Requisitos del profesor → fase

| Requisito (`Descripcion/01_finalProject.pdf`) | Fase |
|---|---|
| Simulador con gráficos, varios agentes concurrentes | 1–3 |
| Comportamiento propio + interacción entre agentes | 2, 3, 5, 6 |
| Tabla de estados | 1 |
| Sección crítica búfer 1 | 2 (asiento), 6 (baño de palco, montacargas) |
| Zonas críticas búfer > 1 | 2, 3, 7 |
| Evitar deadlock | 6 |
| Cliente-servidor, agentes cambian de computadora | 4, 5, 7 |
| Sonido | 8 |
| Especificar cuántos agentes entran | 8 (antes de eso, por argumentos de línea de comandos) |
