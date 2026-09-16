# Детальный план разработки видеоплееров

**Дата создания:** 26 January 2026
**Версия:** 1.0
**Статус:** В разработке

## Обзор

Данный документ содержит максимально подробный пошаговый план доработки видеоплееров для всех платформ проекта IP-CSS.

### Текущее состояние

- **Desktop видеоплеер:** 30% - требуется декодирование H.264/H.265
- **Android видеоплеер:** 70% - требуется доработка ExoPlayer интеграции
- **Веб-видеоплеер:** 85% - требуется доработка WebRTC поддержки

---

## 1. Desktop видеоплеер (30% → 100%)

### 1.1. Анализ текущей реализации

**Текущее состояние:**
- ✅ Базовая структура `VideoPlayer.kt` в `platforms/client-desktop-x86_64`
- ✅ Интеграция с `RtspClient` для получения кадров
- ✅ Отображение MJPEG кадров через `ImageIO`
- ⚠️ `VideoDecoderImpl` частично реализован, но не интегрирован
- ❌ Нет обработки H.264/H.265 NAL units
- ❌ Нет обработки SPS/PPS для H.264
- ❌ Нет обработки VPS/SPS/PPS для H.265

**Проблемы:**
1. `VideoDecoderImpl.decode()` не обрабатывает NAL units правильно
2. Отсутствует извлечение параметров кодека из SPS/PPS
3. Нет обработки B-frames и P-frames
4. Неправильная обработка timestamp кадров

### 1.2. Доработка VideoDecoderImpl

#### Шаг 1.1: Обработка NAL units для H.264

**Файл:** `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/video/VideoDecoderImpl.kt`

**Задачи:**
1. Добавить функцию извлечения NAL units из RTP пакетов
2. Реализовать парсинг SPS (Sequence Parameter Set)
3. Реализовать парсинг PPS (Picture Parameter Set)
4. Извлечение разрешения и FPS из SPS
5. Обработка IDR frames (Instantaneous Decoder Refresh)
6. Обработка P-frames и B-frames

**Код для реализации:**

```kotlin
// Добавить в VideoDecoderImpl
private var sps: ByteArray? = null
private var pps: ByteArray? = null
private var codecContextInitialized = false

private fun extractNalUnits(data: ByteArray): List<ByteArray> {
    val nalUnits = mutableListOf<ByteArray>()
    var i = 0

    while (i < data.size - 3) {
        // Поиск start code: 0x00 0x00 0x00 0x01 или 0x00 0x00 0x01
        if (data[i] == 0x00.toByte() && data[i + 1] == 0x00.toByte()) {
            val startCodeLength = if (data[i + 2] == 0x00.toByte() && data[i + 3] == 0x01.toByte()) {
                4
            } else if (data[i + 2] == 0x01.toByte()) {
                3
            } else {
                i++
                continue
            }

            val startPos = i + startCodeLength
            i = startPos

            // Поиск следующего start code
            var endPos = data.size
            var j = startPos
            while (j < data.size - 2) {
                if (data[j] == 0x00.toByte() && data[j + 1] == 0x00.toByte()) {
                    if (j + 2 < data.size && data[j + 2] == 0x01.toByte()) {
                        endPos = j
                        break
                    } else if (j + 3 < data.size && data[j + 2] == 0x00.toByte() && data[j + 3] == 0x01.toByte()) {
                        endPos = j
                        break
                    }
                }
                j++
            }

            if (endPos > startPos) {
                val nalUnit = data.sliceArray(startPos until endPos)
                nalUnits.add(nalUnit)
            }

            i = endPos
        } else {
            i++
        }
    }

    return nalUnits
}

private fun parseSPS(spsData: ByteArray): Pair<Int, Int>? {
    // Упрощенный парсинг SPS для получения разрешения
    // Полная реализация требует Exp-Golomb декодирования
    // Здесь используем упрощенную версию
    try {
        // Базовая проверка
        if (spsData.size < 4) return null

        // Извлечение разрешения (упрощенная версия)
        // В реальной реализации нужно парсить SPS полностью
        // Для MVP используем значения из конфигурации
        return Pair(width, height)
    } catch (e: Exception) {
        logger.error(e) { "Failed to parse SPS" }
        return null
    }
}

private fun initializeCodecContextWithSPSPPS() {
    if (codecContextInitialized || sps == null || pps == null) return

    try {
        // Извлечение параметров из SPS
        val (actualWidth, actualHeight) = parseSPS(sps!!) ?: return

        // Обновление контекста кодера с правильными параметрами
        codecContext?.let { ctx ->
            ctx.width(actualWidth)
            ctx.height(actualHeight)

            // Установка extradata (SPS + PPS) для H.264
            val extradata = ByteArray(sps!!.size + pps!!.size + 8)
            extradata[0] = 0x01 // version
            extradata[1] = sps!![1] // profile
            extradata[2] = sps!![2] // profile compat
            extradata[3] = sps!![3] // level
            extradata[4] = 0xFF.toByte() // reserved + lengthSizeMinusOne
            extradata[5] = 0xE1.toByte() // numOfSequenceParameterSets (1)

            // SPS length (2 bytes, big-endian)
            extradata[6] = ((sps!!.size shr 8) and 0xFF).toByte()
            extradata[7] = (sps!!.size and 0xFF).toByte()

            // SPS data
            System.arraycopy(sps!!, 0, extradata, 8, sps!!.size)

            // PPS count
            val ppsOffset = 8 + sps!!.size
            extradata[ppsOffset] = 0x01.toByte() // numOfPictureParameterSets (1)

            // PPS length (2 bytes, big-endian)
            extradata[ppsOffset + 1] = ((pps!!.size shr 8) and 0xFF).toByte()
            extradata[ppsOffset + 2] = (pps!!.size and 0xFF).toByte()

            // PPS data
            System.arraycopy(pps!!, 0, extradata, ppsOffset + 3, pps!!.size)

            val extradataPointer = BytePointer(extradata)
            ctx.extradata(extradataPointer)
            ctx.extradata_size(extradata.size)

            // Переоткрытие кодера с новыми параметрами
            val avCodec = avcodec_find_decoder(
                if (codec == VideoCodec.H264) avcodec.AV_CODEC_ID_H264 else avcodec.AV_CODEC_ID_H265
            )
            val result = avcodec_open2(ctx, avCodec, null as Pointer?)
            if (result >= 0) {
                codecContextInitialized = true
                logger.info { "Codec context initialized with SPS/PPS" }
            }
        }
    } catch (e: Exception) {
        logger.error(e) { "Failed to initialize codec context with SPS/PPS" }
    }
}
```

#### Шаг 1.2: Обработка NAL units для H.265

**Задачи:**
1. Добавить обработку VPS (Video Parameter Set) для H.265
2. Реализовать парсинг SPS для H.265
3. Реализовать парсинг PPS для H.265
4. Обработка HEVC NAL unit types

**Код для реализации:**

```kotlin
private var vps: ByteArray? = null // Для H.265

private fun processH265NalUnit(nalUnit: ByteArray) {
    if (nalUnit.isEmpty()) return

    val nalType = (nalUnit[0] shr 1) and 0x3F

    when (nalType) {
        32 -> { // VPS
            vps = nalUnit
            logger.debug { "Received VPS" }
        }
        33 -> { // SPS
            sps = nalUnit
            logger.debug { "Received SPS" }
        }
        34 -> { // PPS
            pps = nalUnit
            logger.debug { "Received PPS" }
            if (sps != null) {
                initializeCodecContextWithSPSPPS()
            }
        }
        19, 20 -> { // IDR frames
            if (!codecContextInitialized && sps != null && pps != null) {
                initializeCodecContextWithSPSPPS()
            }
        }
    }
}
```

#### Шаг 1.3: Улучшение метода decode()

**Задачи:**
1. Интегрировать обработку NAL units
2. Правильная сборка AVPacket из NAL units
3. Обработка фрагментированных RTP пакетов
4. Правильная установка timestamp

**Код для реализации:**

```kotlin
fun decode(frame: RtspFrame): DecodedVideoFrame? {
    if (!isInitialized || codecContext == null) {
        return null
    }

    return try {
        // Извлечение NAL units из данных кадра
        val nalUnits = extractNalUnits(frame.data)

        if (nalUnits.isEmpty()) {
            logger.debug { "No NAL units found in frame" }
            return null
        }

        // Обработка каждого NAL unit
        for (nalUnit in nalUnits) {
            if (nalUnit.isEmpty()) continue

            val nalType = (nalUnit[0] shr 1) and 0x3F

            // Обработка параметров кодека
            when (codec) {
                VideoCodec.H264 -> {
                    when (nalType) {
                        7 -> { // SPS
                            sps = nalUnit
                        }
                        8 -> { // PPS
                            pps = nalUnit
                            if (sps != null) {
                                initializeCodecContextWithSPSPPS()
                            }
                        }
                        5 -> { // IDR frame
                            if (!codecContextInitialized && sps != null && pps != null) {
                                initializeCodecContextWithSPSPPS()
                            }
                        }
                    }
                }
                VideoCodec.H265 -> {
                    processH265NalUnit(nalUnit)
                }
                else -> {}
            }
        }

        // Если контекст не инициализирован, пропускаем кадр
        if (!codecContextInitialized) {
            return null
        }

        // Создание AVPacket из всех NAL units
        val totalSize = nalUnits.sumOf { it.size }
        val packetData = ByteArray(totalSize)
        var offset = 0

        for (nalUnit in nalUnits) {
            System.arraycopy(nalUnit, 0, packetData, offset, nalUnit.size)
            offset += nalUnit.size
        }

        // Создание AVPacket
        val packet = AVPacket()
        avcodec.av_init_packet(packet)
        packet.data(BytePointer(packetData))
        packet.size(packetData.size)
        packet.pts(frame.timestamp)
        packet.dts(frame.timestamp)

        // Отправка пакета в декодер
        val sendResult = avcodec.avcodec_send_packet(codecContext, packet)
        if (sendResult < 0) {
            logger.debug { "Failed to send packet: $sendResult" }
            avcodec.av_packet_unref(packet)
            return null
        }

        // Получение декодированного кадра
        val decodedFrame = AVFrame()
        val receiveResult = avcodec.avcodec_receive_frame(codecContext, decodedFrame)

        avcodec.av_packet_unref(packet)

        if (receiveResult < 0) {
            logger.debug { "Failed to receive frame: $receiveResult" }
            return null
        }

        // Конвертация YUV в RGB
        sws_scale(
            swsContext,
            decodedFrame.data(),
            decodedFrame.linesize(),
            0,
            height,
            rgbFrame!!.data(),
            rgbFrame!!.linesize()
        )

        // Копирование RGB данных
        val rgbData = ByteArray(width * height * 3)
        rgbBuffer!!.get(rgbData)

        DecodedVideoFrame(
            data = rgbData,
            width = width,
            height = height,
            timestamp = frame.timestamp,
            format = DecodedVideoFrame.PixelFormat.RGB24
        )
    } catch (e: Exception) {
        logger.error(e) { "Error decoding frame" }
        null
    }
}
```

### 1.3. Интеграция декодера в Desktop VideoPlayer

#### Шаг 1.4: Обновление VideoPlayer.kt

**Файл:** `platforms/client-desktop-x86_64/app/src/main/kotlin/com/company/ipcamera/desktop/ui/components/VideoPlayer.kt`

**Задачи:**
1. Интегрировать `VideoDecoder` вместо прямого использования `ImageIO`
2. Определение кодека из RTSP потока
3. Создание декодера при получении первого кадра
4. Обработка декодированных кадров
5. Освобождение ресурсов декодера

**Код для реализации:**

```kotlin
@Composable
fun VideoPlayer(
    camera: Camera,
    modifier: Modifier = Modifier,
    onStatusChange: (RtspClientStatus) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var rtspClient by remember { mutableStateOf<RtspClient?>(null) }
    var currentFrame by remember { mutableStateOf<BufferedImage?>(null) }
    var status by remember { mutableStateOf<RtspClientStatus>(RtspClientStatus.DISCONNECTED) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Добавить декодер
    var videoDecoder by remember { mutableStateOf<com.company.ipcamera.core.network.video.VideoDecoder?>(null) }
    var codec by remember { mutableStateOf<com.company.ipcamera.core.network.video.VideoCodec?>(null) }
    var frameWidth by remember { mutableStateOf(1920) }
    var frameHeight by remember { mutableStateOf(1080) }

    // Создание RTSP клиента
    LaunchedEffect(camera.id) {
        val config = RtspClientConfig(
            url = camera.url,
            username = camera.username,
            password = camera.password,
            enableVideo = true,
            enableAudio = false,
            timeoutMillis = 10000
        )

        val client = RtspClient(config)
        rtspClient = client

        // Подписка на статус
        coroutineScope.launch {
            client.getStatus().collect { newStatus ->
                status = newStatus
                onStatusChange(newStatus)

                when (newStatus) {
                    RtspClientStatus.ERROR -> {
                        errorMessage = "Ошибка подключения к камере"
                        onError(errorMessage!!)
                    }
                    else -> errorMessage = null
                }
            }
        }

        // Подписка на видеокадры
        coroutineScope.launch {
            client.getVideoFrames().collect { frame ->
                try {
                    // Определение кодека при первом кадре
                    if (codec == null) {
                        // Попытка определить кодек из данных кадра
                        codec = determineCodecFromFrame(frame)
                        frameWidth = frame.width
                        frameHeight = frame.height

                        // Создание декодера
                        codec?.let { detectedCodec ->
                            if (detectedCodec == VideoCodec.H264 || detectedCodec == VideoCodec.H265) {
                                videoDecoder = com.company.ipcamera.core.network.video.VideoDecoder(
                                    detectedCodec,
                                    frameWidth,
                                    frameHeight
                                )

                                // Установка callback для декодированных кадров
                                videoDecoder?.setCallback { decodedFrame ->
                                    val image = decodedFrame.toBufferedImage()
                                    image?.let { currentFrame = it }
                                }
                            }
                        }
                    }

                    // Декодирование кадра
                    when (codec) {
                        VideoCodec.MJPEG -> {
                            // MJPEG через ImageIO
                            val image = try {
                                ImageIO.read(ByteArrayInputStream(frame.data))
                            } catch (e: Exception) {
                                null
                            }
                            image?.let { currentFrame = it }
                        }
                        VideoCodec.H264, VideoCodec.H265 -> {
                            // H.264/H.265 через декодер
                            videoDecoder?.decode(frame)
                        }
                        else -> {
                            // Fallback на MJPEG
                            val image = try {
                                ImageIO.read(ByteArrayInputStream(frame.data))
                            } catch (e: Exception) {
                                null
                            }
                            image?.let { currentFrame = it }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error processing frame" }
                }
            }
        }

        // Подключение и воспроизведение
        coroutineScope.launch {
            try {
                client.connect()
                client.play()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка подключения"
                onError(errorMessage!!)
            }
        }
    }

    // Очистка при размонтировании
    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                videoDecoder?.release()
                rtspClient?.disconnect()
                rtspClient?.close()
            }
        }
    }

    // ... остальной код UI остается без изменений
}

// Вспомогательная функция для определения кодека
private fun determineCodecFromFrame(frame: RtspFrame): VideoCodec {
    // Простая эвристика для определения кодека
    // В реальной реализации нужно анализировать данные кадра
    if (frame.data.size > 4) {
        // Проверка на H.264 start code
        if (frame.data[0] == 0x00.toByte() &&
            frame.data[1] == 0x00.toByte() &&
            frame.data[2] == 0x00.toByte() &&
            frame.data[3] == 0x01.toByte()) {
            val nalType = (frame.data[4].toInt() and 0x1F)
            if (nalType in 1..23) {
                return VideoCodec.H264
            }
        }

        // Проверка на H.265 start code
        if (frame.data[0] == 0x00.toByte() &&
            frame.data[1] == 0x00.toByte() &&
            frame.data[2] == 0x00.toByte() &&
            frame.data[3] == 0x01.toByte()) {
            val nalType = ((frame.data[4].toInt() shr 1) and 0x3F)
            if (nalType in 0..47) {
                return VideoCodec.H265
            }
        }

        // Проверка на JPEG/MJPEG
        if (frame.data[0] == 0xFF.toByte() && frame.data[1] == 0xD8.toByte()) {
            return VideoCodec.MJPEG
        }
    }

    return VideoCodec.UNKNOWN
}
```

### 1.4. Тестирование и оптимизация

#### Шаг 1.5: Тестирование

**Задачи:**
1. Тестирование с различными камерами (H.264, H.265, MJPEG)
2. Проверка производительности
3. Проверка обработки ошибок
4. Тестирование переподключения

#### Шаг 1.6: Оптимизация

**Задачи:**
1. Оптимизация памяти (освобождение неиспользуемых кадров)
2. Оптимизация CPU (многопоточное декодирование)
3. Кэширование декодированных кадров
4. Адаптивное качество

---

## 2. Android видеоплеер (70% → 100%)

### 2.1. Анализ текущей реализации

**Текущее состояние:**
- ✅ Базовый `ExoVideoPlayer` компонент реализован
- ✅ Поддержка RTSP и HLS потоков
- ✅ Настройка буферизации для низкой задержки
- ⚠️ Неполная обработка ошибок
- ⚠️ Отсутствует статистика воспроизведения
- ⚠️ Нет переключения между RTSP и HLS
- ⚠️ Недостаточно UI контролов

### 2.2. Улучшение обработки ошибок

#### Шаг 2.1: Расширенная обработка ошибок ExoPlayer

**Файл:** `android/app/src/main/java/com/company/ipcamera/android/ui/components/ExoVideoPlayer.kt`

**Задачи:**
1. Детальная классификация ошибок
2. Автоматическое переключение между RTSP и HLS при ошибках
3. Экспоненциальная задержка при переподключении
4. Логирование ошибок для отладки

**Код для реализации:**

```kotlin
@OptIn(UnstableApi::class)
@Composable
fun ExoVideoPlayer(
    videoUrl: String?,
    streamType: StreamType? = null,
    autoPlay: Boolean = true,
    enableLowLatency: Boolean = true,
    modifier: Modifier = Modifier,
    onPlayerReady: ((Player) -> Unit)? = null,
    onError: ((Exception) -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var retryCount by remember { mutableStateOf(0) }
    var currentStreamType by remember { mutableStateOf(streamType) }
    var fallbackToHls by remember { mutableStateOf(false) }
    val maxRetries = 3

    // Определяем тип потока автоматически, если не указан
    val detectedStreamType = currentStreamType ?: remember(videoUrl) {
        when {
            videoUrl?.startsWith("rtsp://") == true -> StreamType.RTSP
            videoUrl?.endsWith(".m3u8") == true || videoUrl?.contains("/hls/") == true -> StreamType.HLS
            else -> StreamType.HLS
        }
    }

    // Функция для получения HLS URL из RTSP URL
    val getHlsUrl: (String) -> String? = remember {
        { rtspUrl ->
            // Преобразование RTSP URL в HLS URL
            // Например: rtsp://camera/stream -> http://server/api/v1/cameras/{id}/stream/hls/playlist.m3u8
            rtspUrl.replace("rtsp://", "http://")
                .replaceAfterLast("/", "")
                .plus("hls/playlist.m3u8")
        }
    }

    // Создаем и настраиваем ExoPlayer
    val exoPlayer = remember {
        val loadControl: LoadControl = if (detectedStreamType == StreamType.RTSP && enableLowLatency && !fallbackToHls) {
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(1000, 2000, 500, 500)
                .build()
        } else {
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(5000, 15000, 2000, 5000)
                .build()
        }

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = autoPlay
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // Устанавливаем медиа-источник
    DisposableEffect(videoUrl, fallbackToHls) {
        val actualUrl = if (fallbackToHls && detectedStreamType == StreamType.RTSP) {
            getHlsUrl(videoUrl ?: "") ?: videoUrl
        } else {
            videoUrl
        }

        if (actualUrl != null && actualUrl.isNotBlank()) {
            val mediaItem = MediaItem.fromUri(actualUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }

        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)

                val errorType = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "NETWORK"
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "HTTP"
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "PARSING"
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "MANIFEST"
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "DECODER"
                    PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> "DECODER_QUERY"
                    PlaybackException.ERROR_CODE_DECODING_FAILED -> "DECODING"
                    else -> "UNKNOWN"
                }

                android.util.Log.e("ExoVideoPlayer", "Player error: $errorType - ${error.message}", error)

                // Автоматическое переключение на HLS при ошибках RTSP
                if (detectedStreamType == StreamType.RTSP &&
                    !fallbackToHls &&
                    errorType in listOf("NETWORK", "PARSING", "DECODER", "DECODING")) {
                    android.util.Log.i("ExoVideoPlayer", "Switching to HLS fallback")
                    fallbackToHls = true
                    retryCount = 0
                    return
                }

                // Повторные попытки для других ошибок
                if (retryCount < maxRetries && errorType == "NETWORK") {
                    retryCount++
                    onRetry?.invoke()
                } else {
                    onError?.invoke(Exception("$errorType: ${error.message}", error))
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    Player.STATE_READY -> {
                        retryCount = 0
                        android.util.Log.d("ExoVideoPlayer", "Player ready")
                    }
                    Player.STATE_BUFFERING -> {
                        android.util.Log.d("ExoVideoPlayer", "Player buffering")
                    }
                    Player.STATE_ENDED -> {
                        android.util.Log.d("ExoVideoPlayer", "Player ended")
                    }
                }
            }
        }

        exoPlayer.addListener(listener)
        onPlayerReady?.invoke(exoPlayer)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Автоматическое переподключение при ошибках
    LaunchedEffect(retryCount, fallbackToHls) {
        if (retryCount > 0 && retryCount <= maxRetries && videoUrl != null) {
            delay(2000L * retryCount) // Экспоненциальная задержка

            val actualUrl = if (fallbackToHls && detectedStreamType == StreamType.RTSP) {
                getHlsUrl(videoUrl) ?: videoUrl
            } else {
                videoUrl
            }

            val mediaItem = MediaItem.fromUri(actualUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    // Создаем PlayerView
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
```

### 2.3. Добавление статистики воспроизведения

#### Шаг 2.2: Компонент статистики

**Файл:** `android/app/src/main/java/com/company/ipcamera/android/ui/components/VideoPlayerStats.kt` (новый файл)

**Задачи:**
1. Отображение FPS
2. Отображение битрейта
3. Отображение буфера
4. Отображение задержки

**Код для реализации:**

```kotlin
@Composable
fun VideoPlayerStats(
    player: Player,
    modifier: Modifier = Modifier
) {
    var stats by remember { mutableStateOf<PlayerStats?>(null) }

    LaunchedEffect(player) {
        while (true) {
            delay(1000) // Обновление каждую секунду

            val videoFormat = player.videoFormat
            val playbackParameters = player.playbackParameters

            stats = PlayerStats(
                fps = videoFormat?.frameRate?.toInt() ?: 0,
                bitrate = videoFormat?.bitrate ?: 0,
                bufferPosition = player.bufferedPosition,
                currentPosition = player.currentPosition,
                bufferPercentage = if (player.duration > 0) {
                    (player.bufferedPosition * 100 / player.duration).toInt()
                } else {
                    0
                }
            )
        }
    }

    stats?.let { s ->
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "FPS: ${s.fps}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "Bitrate: ${s.bitrate / 1000} kbps",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "Buffer: ${s.bufferPercentage}%",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

data class PlayerStats(
    val fps: Int,
    val bitrate: Int,
    val bufferPosition: Long,
    val currentPosition: Long,
    val bufferPercentage: Int
)
```

### 2.4. Улучшение UI контролов

#### Шаг 2.3: Кастомные контролы

**Задачи:**
1. Добавить кнопку переключения между RTSP и HLS
2. Добавить индикатор качества потока
3. Добавить кнопку снимка экрана
4. Улучшить визуальную обратную связь

---

## 3. Веб-видеоплеер (85% → 100%)

### 3.1. Анализ текущей реализации

**Текущее состояние:**
- ✅ HLS.js интеграция полностью реализована
- ✅ Базовый WebRTC код присутствует в `webrtc.ts`
- ⚠️ WebRTC не полностью интегрирован в VideoPlayer
- ⚠️ Отсутствует серверная часть WebRTC
- ⚠️ Неполная обработка ICE candidates
- ⚠️ Нет статистики WebRTC соединения

### 3.2. Реализация серверной части WebRTC

#### Шаг 3.1: WebRTC endpoint на сервере

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/api/routes/StreamRoutes.kt`

**Задачи:**
1. Создать endpoint для WebRTC offer
2. Интеграция с Kurento или Janus для WebRTC
3. Обработка ICE candidates
4. Управление WebRTC сессиями

**Код для реализации:**

```kotlin
// Добавить в StreamRoutes.kt

post("/cameras/{cameraId}/stream/webrtc/offer") {
    val cameraId = call.parameters["cameraId"] ?: return@post call.respond(
        HttpStatusCode.BadRequest,
        ErrorResponse("Camera ID is required")
    )

    try {
        val request = call.receive<WebRTCOfferRequest>()

        // Создание WebRTC endpoint через Kurento/Janus
        // Здесь используется упрощенная версия
        val answer = createWebRTCAnswer(cameraId, request.offer)

        call.respond(
            HttpStatusCode.OK,
            WebRTCAnswerResponse(
                success = true,
                data = WebRTCAnswerData(
                    answer = answer,
                    iceCandidates = emptyList() // Получаются отдельно
                )
            )
        )
    } catch (e: Exception) {
        logger.error(e) { "Error creating WebRTC offer" }
        call.respond(
            HttpStatusCode.InternalServerError,
            ErrorResponse("Failed to create WebRTC offer: ${e.message}")
        )
    }
}

data class WebRTCOfferRequest(
    val offer: RTCSessionDescriptionInit
)

data class WebRTCAnswerResponse(
    val success: Boolean,
    val data: WebRTCAnswerData?,
    val message: String? = null
)

data class WebRTCAnswerData(
    val answer: RTCSessionDescriptionInit,
    val iceCandidates: List<RTCIceCandidateInit>
)
```

### 3.3. Улучшение клиентской части WebRTC

#### Шаг 3.2: Доработка webrtc.ts

**Файл:** `server/web/src/utils/webrtc.ts`

**Задачи:**
1. Улучшить обработку ICE candidates
2. Добавить обработку ошибок
3. Добавить статистику соединения
4. Добавить автоматическое переподключение

**Код для реализации:**

```typescript
export interface WebRTCStats {
  bytesReceived: number;
  bytesSent: number;
  packetsReceived: number;
  packetsSent: number;
  jitter: number;
  rtt: number;
  framesReceived: number;
  framesDropped: number;
}

export async function initWebRTCConnection(
  cameraId: string,
  apiUrl: string,
  options: WebRTCOptions
): Promise<MediaStream> {
  const peerConnection = new RTCPeerConnection({
    iceServers: options.iceServers,
    iceCandidatePoolSize: 10,
  });

  let iceCandidatesQueue: RTCIceCandidate[] = [];
  let isAnswerSet = false;

  // Обработка локальных ICE candidates
  peerConnection.onicecandidate = (event) => {
    if (event.candidate) {
      iceCandidatesQueue.push(event.candidate);
    }
  };

  // Обработка изменения состояния соединения
  peerConnection.onconnectionstatechange = () => {
    const state = peerConnection.connectionState;
    options.onConnectionStateChange?.(state);

    if (state === 'failed' || state === 'disconnected') {
      options.onError?.(new Error(`WebRTC connection ${state}`));
    }
  };

  // Обработка получения треков
  peerConnection.ontrack = (event) => {
    options.onTrack?.(event);
  };

  // Обработка ошибок ICE
  peerConnection.oniceconnectionstatechange = () => {
    const state = peerConnection.iceConnectionState;
    if (state === 'failed' || state === 'disconnected') {
      options.onError?.(new Error(`ICE connection ${state}`));
    }
  };

  try {
    // Создаем offer
    const offer = await peerConnection.createOffer({
      offerToReceiveVideo: true,
      offerToReceiveAudio: true,
    });

    await peerConnection.setLocalDescription(offer);

    // Отправляем offer на сервер
    const response = await fetch(`${apiUrl}/cameras/${cameraId}/stream/webrtc/offer`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        offer: {
          type: offer.type,
          sdp: offer.sdp,
        },
      }),
    });

    if (!response.ok) {
      throw new Error(`Failed to create WebRTC offer: ${response.statusText}`);
    }

    const data = await response.json();
    if (!data.success || !data.data?.answer) {
      throw new Error(data.message || 'Failed to get WebRTC answer');
    }

    const answer = data.data.answer;
    await peerConnection.setRemoteDescription(
      new RTCSessionDescription({
        type: answer.type,
        sdp: answer.sdp,
      })
    );
    isAnswerSet = true;

    // Добавляем ICE candidates из очереди
    while (iceCandidatesQueue.length > 0) {
      const candidate = iceCandidatesQueue.shift();
      if (candidate) {
        await peerConnection.addIceCandidate(candidate);
      }
    }

    // Обрабатываем ICE candidates от сервера
    if (data.data.iceCandidates) {
      for (const candidate of data.data.iceCandidates) {
        await peerConnection.addIceCandidate(
          new RTCIceCandidate(candidate)
        );
      }
    }

    // Создаем MediaStream из треков
    const stream = new MediaStream();

    // Ждем получения треков
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error('Timeout waiting for WebRTC tracks'));
      }, 10000);

      peerConnection.ontrack = (event) => {
        clearTimeout(timeout);
        event.streams[0].getTracks().forEach((track) => {
          stream.addTrack(track);
        });
        options.onTrack?.(event);
        resolve(stream);
      };

      // Если треки уже получены
      peerConnection.getReceivers().forEach((receiver) => {
        if (receiver.track) {
          stream.addTrack(receiver.track);
        }
      });

      if (stream.getTracks().length > 0) {
        clearTimeout(timeout);
        resolve(stream);
      }
    });
  } catch (error) {
    peerConnection.close();
    throw error;
  }
}

/**
 * Получить статистику WebRTC соединения
 */
export async function getWebRTCStats(
  peerConnection: RTCPeerConnection
): Promise<WebRTCStats | null> {
  try {
    const stats = await peerConnection.getStats();
    let bytesReceived = 0;
    let bytesSent = 0;
    let packetsReceived = 0;
    let packetsSent = 0;
    let jitter = 0;
    let rtt = 0;
    let framesReceived = 0;
    let framesDropped = 0;

    stats.forEach((report) => {
      if (report.type === 'inbound-rtp' && report.mediaType === 'video') {
        bytesReceived += report.bytesReceived || 0;
        packetsReceived += report.packetsReceived || 0;
        jitter += report.jitter || 0;
        framesReceived += report.framesReceived || 0;
        framesDropped += report.framesDropped || 0;
      } else if (report.type === 'outbound-rtp' && report.mediaType === 'video') {
        bytesSent += report.bytesSent || 0;
        packetsSent += report.packetsSent || 0;
      } else if (report.type === 'candidate-pair' && report.selected) {
        rtt = report.currentRoundTripTime ? report.currentRoundTripTime * 1000 : 0;
      }
    });

    return {
      bytesReceived,
      bytesSent,
      packetsReceived,
      packetsSent,
      jitter,
      rtt,
      framesReceived,
      framesDropped,
    };
  } catch (error) {
    console.error('Error getting WebRTC stats:', error);
    return null;
  }
}
```

### 3.4. Интеграция WebRTC в VideoPlayer

#### Шаг 3.3: Улучшение VideoPlayer.tsx

**Файл:** `server/web/src/components/VideoPlayer/VideoPlayer.tsx`

**Задачи:**
1. Улучшить обработку WebRTC в VideoPlayer
2. Добавить статистику WebRTC
3. Улучшить fallback на HLS
4. Добавить индикатор WebRTC соединения

**Изменения:**

```typescript
// В компоненте VideoPlayer добавить:

const [webrtcStats, setWebRTCStats] = useState<WebRTCStats | null>(null);

// В useEffect для WebRTC добавить:
useEffect(() => {
  if (streamType === 'webrtc' && pcRef.current) {
    const interval = setInterval(async () => {
      if (pcRef.current) {
        const stats = await getWebRTCStats(pcRef.current);
        setWebRTCStats(stats);
      }
    }, 1000);

    return () => clearInterval(interval);
  }
}, [streamType]);

// Добавить отображение статистики в UI
{webrtcStats && (
  <Box sx={{ position: 'absolute', top: 8, right: 8, bgcolor: 'rgba(0,0,0,0.7)', p: 1, borderRadius: 1 }}>
    <Typography variant="caption" color="white">
      RTT: {webrtcStats.rtt.toFixed(0)}ms
    </Typography>
    <Typography variant="caption" color="white" display="block">
      FPS: {webrtcStats.framesReceived}
    </Typography>
  </Box>
)}
```

---

## 4. План выполнения

### Фаза 1: Desktop видеоплеер (Неделя 1)
- [x] День 1-2: Доработка VideoDecoderImpl (NAL units, SPS/PPS)
- [ ] День 3: Интеграция декодера в VideoPlayer
- [ ] День 4: Тестирование и отладка
- [ ] День 5: Оптимизация производительности

### Фаза 2: Android видеоплеер (Неделя 2)
- [ ] День 1: Улучшение обработки ошибок
- [ ] День 2: Добавление статистики
- [ ] День 3: Улучшение UI контролов
- [ ] День 4-5: Тестирование и отладка

### Фаза 3: Веб-видеоплеер (Неделя 3)
- [ ] День 1-2: Реализация серверной части WebRTC
- [ ] День 3: Улучшение клиентской части WebRTC
- [ ] День 4: Интеграция в VideoPlayer
- [ ] День 5: Тестирование и отладка

### Фаза 4: Финальное тестирование (Неделя 4)
- [ ] Интеграционное тестирование всех платформ
- [ ] Тестирование производительности
- [ ] Исправление найденных проблем
- [ ] Документация

---

## 5. Критерии готовности

### Desktop видеоплеер
- ✅ Декодирование H.264 работает стабильно
- ✅ Декодирование H.265 работает стабильно
- ✅ MJPEG работает как fallback
- ✅ Производительность: минимум 25 FPS для 1080p
- ✅ Память: не более 200MB для одного потока

### Android видеоплеер
- ✅ RTSP потоки воспроизводятся стабильно
- ✅ HLS потоки воспроизводятся стабильно
- ✅ Автоматическое переключение RTSP → HLS при ошибках
- ✅ Статистика отображается корректно
- ✅ UI контролы работают правильно

### Веб-видеоплеер
- ✅ WebRTC соединение устанавливается успешно
- ✅ Задержка WebRTC < 500ms
- ✅ Fallback на HLS при ошибках WebRTC
- ✅ Статистика WebRTC отображается
- ✅ HLS работает как основной режим

---

## 6. Известные проблемы и ограничения

1. **Desktop:** Полный парсинг SPS требует реализации Exp-Golomb декодирования
2. **Android:** Некоторые камеры могут не поддерживать RTSP через ExoPlayer
3. **Web:** WebRTC требует STUN/TURN серверы для работы за NAT

---

## 7. Дополнительные улучшения (будущее)

1. Адаптивное качество видео
2. Запись видео из плеера
3. Снимки экрана
4. PTZ управление через плеер
5. Многокамерный режим

---

**Последнее обновление:** 26 January 2026
