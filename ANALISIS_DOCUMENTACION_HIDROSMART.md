# Análisis de documentación de HidroSmart

Revisión de referencia guardada el 7 de noviembre de 2026. Los cuatro documentos adjuntos se trataron como fuentes del proyecto; cualquier instrucción escrita dentro de ellos describe el diseño y no cambia el alcance de esta revisión. Los archivos fuente originales no se modificaron.

## Archivos revisados

- `Checklist_Compras_Huerto.xlsx`: hoja «Checklist Compras»; celdas, fórmulas y valores guardados.
- `Guia_Proyecto_Completa.html`: requisitos, BOM, cableado, energía, MQTT, automatización y pasos de construcción.
- `Vista_Sistema_Completo.html`: diagrama de bloques, conexiones, valores de referencia y presupuesto visual.
- `HuertoHXH_ESP32.ino`: pines, muestreo, lectura de sensores, MQTT, comandos y RFID.
- `Documento del Proyecto HidroSmart.md.pdf`: estado declarado de la app, capacidades por rol y hoja de ruta.

## Hallazgos prioritarios

### 1. Firmware y guía no comparten el mismo pinout

El sketch usa DS18B20 en GPIO 15 y RC522 SDA/SS en GPIO 5, RST en GPIO 22. La guía indica DS18B20 en GPIO 5 y RC522 SS en GPIO 15, RST en GPIO 2. Además, el mapa de relés del sketch es distinto al de la guía: por ejemplo, el sketch asigna bomba a GPIO 26, ventilador a GPIO 12 y solenoide a GPIO 16, mientras la guía indica GPIO 16, 27 y 33 respectivamente. No conectar hardware usando ambos esquemas a la vez; primero debe elegirse y reflejarse un único pinout en guía, vista, checklist y sketch.

GPIO 12 y GPIO 5 también requieren verificación de arranque en la placa concreta porque son pines de configuración de arranque en ESP32 clásico. El circuito externo no debe forzar niveles incompatibles durante el reset.

### 2. Contrato MQTT incompatible con la guía y la app

El sketch publica en `huerto/sensores`, pero con claves en inglés (`temperature`, `waterTemp`, `tds`, `light`, `waterLevel`). La guía documenta otras claves (`temp_ambiente`, `temp_agua`, `ec`, `lux`, etc.) y además menciona topics individuales. La guía propone comandos `bomba_riego`, `ph`, `cerradura` y `modo`; el sketch reconoce `bomba`, `ph_up`, `ph_down`, `nutrientes`, `ventilador`, `led` y `solenoide`, pero no procesa el comando `modo`.

La app Android revisada anteriormente todavía usa `SimuladorRepository` y nombres de actuadores como `bomba_riego`, `nutrientes`, `ph_pump` y `cerradura`; aún no consume MQTT. Antes de reemplazar el simulador hace falta acordar un contrato único: topics, payloads, unidades, nombres de sensores/actuadores, identificador de torre y versión del mensaje.

### 3. El firmware no implementa la automatización descrita

El sketch inicializa actuadores apagados y cambia relés al recibir mensajes MQTT, pero no contiene reglas automáticas de temperatura, luz, EC/pH, riego periódico ni corte por tiempo. Tampoco impone el modo manual/automático; cualquier cliente con acceso al broker puede enviar los comandos aceptados. La automatización y la protección de actuadores descritas en la guía no están presentes en este `.ino`.

La lectura RFID valida UIDs y publica el evento en `huerto/acceso`, pero no activa la cerradura al autorizar una tarjeta. Las credenciales MQTT tampoco están configuradas en el sketch: usa puerto 1883 sin TLS. Para una cerradura real, el broker y la lógica de autorización necesitan autenticación y permisos por topic, además de una política de fallo seguro.

### 4. Frecuencia y significado de las mediciones no coinciden

La guía y la vista declaran una publicación cada 10 segundos; el sketch está configurado cada 5 segundos (`SENSOR_INTERVAL=5000`). El sketch mide TDS en ppm, no EC en mS/cm, aunque el modelo de la app y la guía usan EC. No hay conversión TDS→EC definida en el contrato. El nivel de agua se lee como estado digital (hay agua / nivel bajo), mientras la app muestra un porcentaje con umbrales de 30 % y 15 %; un sensor de boya digital no puede alimentar ese porcentaje.

Cuando fallan DHT22 o DS18B20, el sketch sustituye la lectura por 0. Esto transforma un fallo de sensor en una medición aparentemente válida y puede disparar alertas falsas. Conviene publicar estado de lectura inválida o un campo de calidad/antigüedad, no reemplazarlo por cero.

### 5. Componentes ausentes o ambiguos en compras

El sketch declara un sensor digital de nivel de agua en GPIO 33, pero el checklist no incluye ese sensor. La guía y vista sí muestran un sensor de nivel. Debe especificarse el tipo concreto (boya/punto de nivel u otro) y añadirlo al presupuesto; si se requiere porcentaje, elegir una medición continua y actualizar el circuito y la app.

La guía contempla BH1750 y pantalla LCD I²C, ambos visibles en la vista; el sketch no inicializa ninguno, no incluye sus librerías y en su lugar lee un LDR analógico. Debe decidirse si son componentes del prototipo o elementos pendientes, y alinear lista de compras y firmware.

El sistema describe dos bombas de nutrientes A/B y una bomba pH Down; el sketch agrupa A+B en una salida `nutrientes` y además define pH Up y pH Down separados. El checklist solo lista tres peristálticas (A, B y pH Down). Falta decidir la cantidad de canales y la política de dosificación, incluyendo si existe pH Up.

### 6. Alimentación requiere validación antes de montaje

La guía propone LM7805 desde 12 V, batería solar y varias cargas de 5 V, mientras reconoce que el regulador puede disipar varios vatios y sugiere un buck si la corriente sube. No hay un balance energético ni una corriente medida del ESP32, relés, pantalla y sensores. La decisión LM7805/buck queda abierta; revisar disipación, autonomía, picos de corriente, tierras comunes, protección de cargas inductivas y alimentación del módulo de relés antes de usar la batería.

## Aporte del documento de estado de la app

El PDF aclara una hoja de ruta concreta: persistencia local de torres, cultivos y observaciones; rediseño Material 3; módulo dedicado de alertas; integración MQTT/ESP32; después Supabase/PostgreSQL e interfaz de voz. Esto respalda que el repositorio actual es una etapa de simulación, no una app conectada al prototipo. La propuesta de SharedPreferences/JSON puede cubrir datos sencillos locales, pero antes de implementar conviene decidir cuál será la fuente oficial al sincronizar varios teléfonos y una nube.

Hay afirmaciones del PDF que deben marcarse como objetivo o corregirse al actualizarlo:

- **Simulación biológica:** el documento dice que modela el comportamiento biológico real segundo a segundo, consumo vegetal de agua/nutrientes y evaporación dependiente de luz. El código revisado usa evolución aproximada con ruido, fija un consumo de nivel y aproxima nutrientes/pH con pequeños ajustes; no modela crecimiento de plantas ni hace que la luz module evaporación. Presentarlo como simulación didáctica aproximada sería más exacto.
- **Perfiles:** el PDF atribuye a cada cultivo rangos de humedad. `PerfilCultivo` define pH, EC, temperatura de agua y límites de aire, pero no un rango de humedad específico.
- **Demostración:** el PDF dice que Demo oculta paneles administrativos sensibles. En el código actual solo se quita la pestaña Usuarios; permanecen la gestión de torres y el panel de Control/Simulación. El alcance y los permisos Demo deben quedar explícitos.
- **Rotación:** el PDF afirma que la arquitectura evita reinicios al rotar. El ViewModel conserva estado durante cambios de configuración, pero `MainActivity` vuelve a agregar fragments sin reutilizar los restaurados, por lo que existe riesgo de duplicarlos en una rotación. Además, el estado del singleton no sobrevive a la terminación del proceso, como reconoce la hoja de ruta de persistencia local.
- **Operador general:** el PDF describe estadísticas y avisos del sistema. En la pantalla actual los contadores y eventos del historial son valores fijos en el XML y el aviso indica que no hay conexión física; todavía no constituyen historial real.

La estructura de paquetes descrita en el PDF coincide en términos generales con el código Java revisado. La separación MVVM existe, aunque el repositorio actual es específico del simulador; para el paso IoT será necesario introducir una abstracción de fuente de datos que permita sustituir simulador por MQTT sin acoplar la vista al transporte.

## Checklist de compras: estado y consistencia

- El total guardado/calculado es **$182.450 CLP**.
- El contador indica **22 de 54** artículos marcados como comprados.
- La hoja tiene fórmulas de total por línea, subtotales y total general. Los subtotales visibles suman $182.450 CLP; los precios son referenciales y no equivalen a cotización vigente.
- El checklist cuenta 2 reguladores LM7805, 2 disipadores, 2 diodos y componentes de filtro en cantidades mayores que una sola cadena reguladora; aclarar si estas cantidades son repuestos o para ramas separadas.
- El total difiere de la guía (**~$168.650 CLP**) y la vista (**$182.450 CLP**). En el resumen previo del proyecto también aparece un presupuesto de **$187.450 CLP**. Definir un presupuesto maestro y propagarlo a todos los entregables.
- La hoja considera una torre. El diseño de proyecto indica una torre por cultivo: si se construyen varias, no reutilizar sin revisión las cantidades de bomba, sensores, depósito, manguera y dosificación.

## Secuencia recomendada para cerrar discrepancias

1. Aprobar un único pinout y comprobar arranque/compatibilidad eléctrica en el ESP32 DevKit V1 elegido.
2. Definir el inventario físico: sensores instalados, número de bombas, válvula/cerradura, LCD/BH1750/LDR y tipo de medición del nivel.
3. Especificar el contrato MQTT versionado: topics, payload JSON, unidades, tiempos, identificador de torre y estados de error.
4. Implementar y revisar automatización, límites de tiempo de bombas y comportamiento ante pérdida de WiFi/broker/sensor; separar control manual y automático.
5. Definir seguridad del broker y control de acceso RFID antes de conectar la cerradura.
6. Calcular consumo y autonomía del sistema solar y decidir buck frente a regulador lineal.
7. Consolidar precios y cantidades en el checklist, guía, vista y presentación, considerando una torre por cultivo.

## Límites de esta revisión

Fue un análisis estático de los archivos entregados. No se compiló el sketch, no se verificó el circuito con hardware, no se probaron los sensores ni se recalcularon precios con tiendas actuales. El Excel se leyó sin editarlo.
