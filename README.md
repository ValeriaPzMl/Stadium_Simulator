# Simulador distribuido de Estadio de Fútbol

Proyecto final de **Fundamentos de Programación en Paralelo**: un simulador gráfico en Java de un estadio de fútbol durante un partido. Aficionados, guardias, cocineros, meseros y vendedores son **hilos concurrentes** que compiten por los recursos del estadio (torniquetes, baños, comida, escaleras, asientos). Para evitar condiciones de carrera y deadlocks se usan **semáforos, candados y monitores**.

El estadio está **distribuido en 3 servidores**, uno por piso. Los aficionados se mueven físicamente de una computadora a otra al subir o bajar las escaleras.

## Los tres pisos

| Servidor | Zona | Qué hay |
|---|---|---|
| **S1 – Planta baja** | Cancha y gradas bajas | Única entrada (torniquetes + guardias), asientos numerados, tienda, baño general, escaleras |
| **S2 – Palcos** | Palcos privados | Acceso con pase especial, cada palco con baño propio, meseros que llevan la comida |
| **S3 – Grada alta** | Zona popular (lugares baratos) | Asientos, tienda, baño general, vendedores de gradas |

## Agentes

- **Aficionado**: llega con boleto General, Palco o Popular, pasa por la entrada, va a su lugar, ve el partido, va al baño, compra comida y al final se va.
- **Guardia**: revisa boletos en la entrada (S1) y en el acceso a palcos (S2).
- **Cocinero**: prepara comida y la deja en el mostrador.
- **Mesero**: atiende los pedidos de los palcos.
- **Vendedor de gradas**: recorre las gradas vendiendo a quien lo llama.
- **Reloj del partido**: marca inicio, goles, medio tiempo y final. Estos eventos cambian el comportamiento de todos; en el medio tiempo, por ejemplo, todos corren al baño.

## Conceptos de concurrencia que se demuestran

- Sección crítica de **búfer 1**: baño de cada palco (`synchronized` / `ReentrantLock`).
- Zonas críticas de **búfer > 1**: baños generales, torniquetes y cajas (`Semaphore`).
- **Productor-consumidor** con monitor (`wait`/`notifyAll`): mostrador de comida, cola de pedidos y cola de llamados.
- **Monitor con condiciones**: escalera de un solo sentido a la vez.
- **Prevención de deadlock**: el mesero necesita bandeja + montacargas y los toma en orden fijo.
- **Estados de los hilos**: la tabla muestra el `Thread.State` real de cada agente (RUNNABLE, BLOCKED, WAITING, TIMED_WAITING…).

## Tecnologías

Java 17+, Swing (gráficos), `javax.sound` (sonido), sockets TCP (comunicación entre servidores).

## Ejecución (planeada)

```bash
# En cada computadora (o en 3 terminales locales)
java -jar estadio.jar --piso=1 --puerto=5001 --vecinos=localhost:5002,localhost:5003
java -jar estadio.jar --piso=2 --puerto=5002 --vecinos=localhost:5001
java -jar estadio.jar --piso=3 --puerto=5003 --vecinos=localhost:5001
```

## Documentación

- [`PLAN.md`](PLAN.md): plan del proyecto, arquitectura y scope por entregas.
- [`entrega1/ANALISIS.md`](entrega1/ANALISIS.md): análisis y diseño (agentes, búferes, estados, eventos, secciones críticas).
- `entrega1/bpmn/`: diagramas BPMN de cada agente (abrir con [diagrams.net](https://app.diagrams.net)).
- `Descripcion/`: enunciado del proyecto y ejemplo del profesor.
