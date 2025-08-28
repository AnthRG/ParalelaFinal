# ParalelaFinal

## Descripción del Proyecto

Este proyecto implementa un sistema de simulación de tráfico vehicular utilizando JavaFX y programación concurrente y paralela en Java. El sistema modela intersecciones de tráfico con semáforos inteligentes que priorizan vehículos de emergencia y gestionan el flujo vehicular de manera eficiente.

## Arquitectura del Sistema

El proyecto está estructurado en dos escenarios principales, cada uno con diferentes características y complejidades:

### Componentes Principales

- **CrossroadsApp**: Aplicación principal que presenta un menú para seleccionar entre los dos escenarios
- **SimulationConfig**: Configuración centralizada para dimensiones, velocidades y parámetros de simulación
- **LanePositionAdjustment**: Ajustes finos de posicionamiento para diferentes tipos de carriles

## Escenario 1: Intersección Simple (Crossroads Traffic)

### Características

- **Tipo de Intersección**: Intersección única de cuatro vías
- **Gestión de Tráfico**: Sistema de semáforos con control inteligente
- **Tipos de Vehículos**: Normales y de emergencia
- **Movimientos Soportados**: Giro a la derecha, directo, giro a la izquierda, y vuelta en U

### Funcionalidades Implementadas

#### Sistema de Prioridades
1. **Vehículos de Emergencia**: Tienen prioridad absoluta sobre todos los demás vehículos
2. **Primer Llegado, Primer Servido**: En condiciones normales, se prioriza por tiempo de llegada
3. **Control de Semáforos**: Los semáforos se ajustan automáticamente según las prioridades

#### Gestión Concurrente
- Utiliza `ScheduledExecutorService` para el control temporal
- Implementa `ReentrantLock` para sincronización thread-safe
- Actualización de UI en tiempo real con JavaFX Timeline

#### TrafficController
- Monitorea continuamente el estado de todos los vehículos
- Detecta automáticamente vehículos de emergencia
- Ajusta los semáforos para optimizar el flujo de tráfico
- Previene colisiones mediante detección de proximidad

### Entidades

#### Vehicle
- **Propiedades**: ID único, tipo (normal/emergencia), dirección, posición, tiempo de llegada
- **Estados**: En intersección, fase de vuelta en U
- **Comportamiento**: Movimiento autónomo según las reglas de tráfico

#### Intersection
- **Gestión de Colas**: Maneja colas separadas para cada dirección de movimiento
- **Estados**: Tracking de vehículos en diferentes posiciones
- **Sincronización**: Thread-safe para acceso concurrente

#### TrafficLight
- **Estados**: Verde/Rojo con cambios automáticos
- **Control**: Responde a condiciones de emergencia
- **Visualización**: Representación gráfica en tiempo real

## Escenario 2: Red de Intersecciones (Road Grid)

### Características

- **Tipo de Red**: Sistema de intersecciones múltiples (3x2 grid)
- **Intersecciones**: 6 intersecciones independientes (East1-3, West1-3)
- **Gestión Avanzada**: Control distribuido con sincronización entre intersecciones
- **Movimientos Complejos**: Incluye giros especiales hacia rutas norte-sur

### Funcionalidades Implementadas

#### Sistema de Control Distribuido
- **Sincronización Global**: Todos los semáforos East/West operan sincronizadamente
- **Control Independiente**: Movimientos norte-sur operan independientemente
- **Prioridad de Emergencia**: Los vehículos de emergencia tienen prioridad absoluta independientemente del estado del semáforo

#### Mejoras de Tráfico Implementadas

##### Prioridad Absoluta de Emergencia
```java
// PRIORITY 1: Emergency vehicles can ALWAYS proceed regardless of traffic light
if (v.isEmergency()) {
    canProceed = true;
}
```

##### Avance en Semáforo Rojo
Los vehículos pueden avanzar hasta su posición de espera incluso cuando el semáforo está en rojo, permitiendo un flujo más natural del tráfico:

```java
// For vehicles in red light, only allow movement if no collision
// This lets them advance to the intersection waiting area
if (!canProceed) {
    if (!canMoveWithoutCollision(v, current, westbound)) {
        break; // Stop if collision would occur
    }
    // Allow limited movement towards intersection for red light vehicles
    if (direction.equals("straight") || direction.equals("left") || direction.equals("right")) {
        processVehicleMovement(v, sourceLane, current, next, westbound, queue);
    }
}
```

#### Tipos de Movimientos Avanzados

1. **Movimientos Horizontales Estándar**: straight, left, right
2. **Vueltas en U**: Regulares y con extensión (u-turn-second)
3. **Giros Especiales Norte-Sur**: 
   - left-north-first/second
   - right-south-first/second
   - left-south-first/second
   - right-north-first/second

#### TrafficController Avanzado

##### Control de Múltiples Intersecciones
- **RightIntersections**: Intersecciones West (movimiento hacia el este)
- **LeftIntersections**: Intersecciones East (movimiento hacia el oeste)
- **Procesamiento Paralelo**: Manejo simultáneo de múltiples intersecciones

##### Detección de Colisiones Mejorada
```java
private boolean canMoveWithoutCollision(Vehicle movingVehicle, Intersection intersection, boolean westbound)
```
- Verificación horizontal y vertical de proximidad
- Cálculo de distancia mínima segura
- Prevención de colisiones en movimientos complejos

##### Gestión de Fases de Giro
- **Fase 0**: Aproximación a la intersección
- **Fase 1**: Ejecución del giro (90 grados)
- **Fase 2**: Movimiento post-giro
- **Fase 3**: Movimiento extendido (para variantes "second")

## Tecnologías Utilizadas

- **JavaFX**: Para la interfaz gráfica y animaciones
- **Java Concurrency**: ScheduledExecutorService, ReentrantLock, PriorityBlockingQueue
- **Gradle**: Sistema de construcción y gestión de dependencias
- **Module System**: Uso del sistema de módulos de Java 9+

## Configuración y Ejecución

### Requisitos
- Java 11 o superior
- JavaFX SDK
- Gradle 8.0+

### Ejecución
```bash
./gradlew run
```

### Estructura del Proyecto
```
src/main/java/app/paralelafinal/
├── CrossroadsApp.java          # Aplicación principal
├── config/                     # Configuraciones globales
├── escenario1/                 # Intersección simple
│   ├── controladores/
│   ├── entidades/
│   └── simulation/
└── escenario2/                 # Red de intersecciones
    ├── controladores/
    ├── entidades/
    └── simulation/
```

## Características Técnicas Destacadas

### Programación Concurrente
- Uso extensivo de hilos para simulación en tiempo real
- Sincronización thread-safe entre componentes
- Evitación de deadlocks mediante ordenamiento de locks

### Rendimiento Optimizado
- Procesamiento eficiente de colas de vehículos
- Minimización de tiempo de lock para operaciones críticas
- Detección inteligente de colisiones

### Interfaz de Usuario Responsiva
- Animaciones fluidas a 60 FPS
- Visualización en tiempo real del estado de semáforos
- Representación gráfica clara de diferentes tipos de vehículos

## Casos de Uso Simulados

1. **Tráfico Normal**: Flujo regular de vehículos con respeto a semáforos
2. **Emergencias**: Priorización automática de vehículos de emergencia
3. **Congestión**: Manejo inteligente de acumulación de vehículos
4. **Movimientos Complejos**: Gestión de giros en U y movimientos especiales