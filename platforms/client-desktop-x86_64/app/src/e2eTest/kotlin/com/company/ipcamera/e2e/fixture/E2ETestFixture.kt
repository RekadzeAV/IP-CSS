package com.company.ipcamera.e2e.fixture

import com.company.ipcamera.e2e.api.TestApiClient
import com.company.ipcamera.e2e.data.TestDataFactory
import com.company.ipcamera.shared.domain.model.Camera
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

/**
 * Fixture для E2E тестов.
 * 
 * Использует Selenium WebDriver для автоматизации браузера.
 */
class E2ETestFixture {

    private lateinit var driver: WebDriver
    private lateinit var wait: WebDriverWait
    private val baseUrl = "http://localhost:8080"
    private val apiBaseUrl = "$baseUrl/api/v1"
    private val implicitWaitMs = 10000L

    private lateinit var apiClient: TestApiClient
    private val createdCameras = mutableListOf<String>()
    private val createdUsers = mutableListOf<String>()

    init {
        setupDriver()
        setupApiClient()
    }

    /**
     * Настройка WebDriver
     */
    private fun setupDriver() {
        val options = ChromeOptions().apply {
            addArguments("--headless=new")
            addArguments("--no-sandbox")
            addArguments("--disable-dev-shm-usage")
            addArguments("--disable-gpu")
            addArguments("--window-size=1920,1080")
        }
        driver = ChromeDriver(options)
        wait = WebDriverWait(driver, Duration.ofMillis(implicitWaitMs))
        driver.manage().timeouts().implicitlyWait(Duration.ofMillis(implicitWaitMs))
    }

    /**
     * Настройка API клиента
     */
    private fun setupApiClient() {
        apiClient = TestApiClient(baseUrl = apiBaseUrl)
        apiClient.initialize()
    }

    /**
     * Очистка тестовых данных
     */
    suspend fun cleanup() {
        try {
            // Delete created cameras
            createdCameras.forEach { cameraId ->
                try {
                    apiClient.deleteCamera(cameraId)
                } catch (e: Exception) {
                    // Ignore
                }
            }
            createdCameras.clear()
            
            // Delete created users
            createdUsers.forEach { userId ->
                try {
                    apiClient.deleteUser(userId)
                } catch (e: Exception) {
                    // Ignore
                }
            }
            createdUsers.clear()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    /**
     * Закрытие fixture
     */
    fun close() {
        try {
            runBlocking { cleanup() }
        } catch (e: Exception) {
            // Ignore
        }
        apiClient.close()
        driver.quit()
    }

    /**
     * Запуск приложения
     */
    fun startApplication() {
        driver.get(baseUrl)
    }

    /**
     * Переход на URL
     */
    fun navigateTo(path: String) {
        driver.get("$baseUrl$path")
        Thread.sleep(1000) // Wait for page load
    }

    /**
     * Заполнение формы логина
     */
    fun fillLoginForm(username: String, password: String) {
        val usernameInput = wait.until {
            driver.findElement(By.cssSelector("input[name='username'], input[type='text']"))
        }
        val passwordInput = driver.findElement(By.cssSelector("input[name='password'], input[type='password']"))
        
        usernameInput.clear()
        usernameInput.sendKeys(username)
        passwordInput.clear()
        passwordInput.sendKeys(password)
    }

    /**
     * Клик по кнопке логина
     */
    fun clickLoginButton() {
        val loginButton = wait.until {
            driver.findElement(By.cssSelector("button[type='submit'], button:contains('Login'), button:contains('Вход')"))
        }
        loginButton.click()
        Thread.sleep(2000) // Wait for navigation
    }

    /**
     * Ожидание URL
     */
    fun waitForUrl(path: String) {
        wait.until {
            driver.currentUrl.contains(path)
        }
    }

    /**
     * Проверка логина
     */
    fun isUserLoggedIn(): Boolean {
        try {
            val userInfo = driver.findElement(By.cssSelector(".user-info, .user-profile, [data-testid='user-info']"))
            return userInfo.isDisplayed
        } catch (e: NoSuchElementException) {
            return false
        }
    }

    /**
     * Логин как admin
     */
    fun loginAsAdmin() {
        navigateTo("/login")
        fillLoginForm("admin", "admin123")
        clickLoginButton()
        waitForUrl("/dashboard")
    }

    /**
     * Добавление камеры
     */
    fun addCamera(name: String, url: String, username: String, password: String) {
        // Click add camera button
        val addButton = wait.until {
            driver.findElement(By.cssSelector("button:contains('Add'), button:contains('Add Camera'), button[data-testid='add-camera']"))
        }
        addButton.click()
        Thread.sleep(500)

        // Fill form
        val nameInput = driver.findElement(By.cssSelector("input[name='name'], input[placeholder*='Name']"))
        val urlInput = driver.findElement(By.cssSelector("input[name='url'], input[placeholder*='URL'], input[placeholder*='rtsp']"))
        val userInput = driver.findElement(By.cssSelector("input[name='username']"))
        val passInput = driver.findElement(By.cssSelector("input[name='password']"))

        nameInput.clear()
        nameInput.sendKeys(name)
        urlInput.clear()
        urlInput.sendKeys(url)
        userInput.clear()
        userInput.sendKeys(username)
        passInput.clear()
        passInput.sendKeys(password)

        // Submit
        val submitButton = driver.findElement(By.cssSelector("button[type='submit'], button:contains('Save'), button:contains('Add')"))
        submitButton.click()
        Thread.sleep(2000) // Wait for API call
    }

    /**
     * Проверка существования камеры
     */
    fun cameraExists(name: String): Boolean {
        try {
            val cameraElement = driver.findElement(By.cssSelector("[data-testid='camera-$name']"))
            return cameraElement.isDisplayed
        } catch (e: NoSuchElementException) {
            try {
                // Fallback: поиск по тексту
                val elements = driver.findElements(By.cssSelector(".camera-item, [data-camera]"))
                return elements.any { element -> element.text.toString().contains(name) }
            } catch (e2: Exception) {
                return false
            }
        }
    }

    /**
     * Редактирование камеры
     */
    fun editCamera(cameraName: String, newName: String? = null, newUrl: String? = null) {
        // Find camera and click edit
        val editButton = wait.until {
            driver.findElement(By.cssSelector("[data-camera-name='$cameraName'] button:contains('Edit'), button[data-testid='edit-camera']"))
        }
        editButton.click()
        Thread.sleep(500)

        // Update name if provided
        if (newName != null) {
            val nameInput = driver.findElement(By.cssSelector("input[name='name']"))
            nameInput.clear()
            nameInput.sendKeys(newName)
        }

        // Update URL if provided
        if (newUrl != null) {
            val urlInput = driver.findElement(By.cssSelector("input[name='url']"))
            urlInput.clear()
            urlInput.sendKeys(newUrl)
        }

        // Submit
        val submitButton = driver.findElement(By.cssSelector("button[type='submit'], button:contains('Save')"))
        submitButton.click()
        Thread.sleep(2000)
    }

    /**
     * Удаление камеры
     */
    fun deleteCamera(cameraName: String) {
        // Find camera and click delete
        val deleteButton = wait.until {
            driver.findElement(By.cssSelector("[data-camera-name='$cameraName'] button:contains('Delete'), button[data-testid='delete-camera']"))
        }
        deleteButton.click()
        Thread.sleep(500)

        // Confirm deletion
        val confirmButton = wait.until {
            driver.findElement(By.cssSelector("button:contains('Confirm'), button:contains('Delete'), .modal button:last-child"))
        }
        confirmButton.click()
        Thread.sleep(2000)
    }

    /**
     * Старт записи
     */
    fun startRecording(cameraId: String) {
        val startButton = wait.until {
            driver.findElement(By.cssSelector("[data-camera-id='$cameraId'] button:contains('Start'), button[data-testid='start-recording']"))
        }
        startButton.click()
        Thread.sleep(1000)
    }

    /**
     * Проверка статуса записи
     */
    fun isRecording(cameraId: String): Boolean {
        try {
            val recordingIndicator = driver.findElement(By.cssSelector("[data-camera-id='$cameraId'] .recording, [data-camera-id='$cameraId'] .status:contains('Recording')"))
            return recordingIndicator.isDisplayed
        } catch (e: NoSuchElementException) {
            return false
        }
    }

    /**
     * Пауза записи
     */
    fun pauseRecording(cameraId: String) {
        val pauseButton = driver.findElement(By.cssSelector("[data-camera-id='$cameraId'] button:contains('Pause'), button[data-testid='pause-recording']"))
        pauseButton.click()
        Thread.sleep(1000)
    }

    /**
     * Проверка статуса паузы
     */
    fun isPaused(cameraId: String): Boolean {
        try {
            val pausedIndicator = driver.findElement(By.cssSelector("[data-camera-id='$cameraId'] .paused, [data-camera-id='$cameraId'] .status:contains('Paused')"))
            return pausedIndicator.isDisplayed
        } catch (e: NoSuchElementException) {
            return false
        }
    }

    /**
     * Возобновление записи
     */
    fun resumeRecording(cameraId: String) {
        val resumeButton = driver.findElement(By.cssSelector("[data-camera-id='$cameraId'] button:contains('Resume'), button[data-testid='resume-recording']"))
        resumeButton.click()
        Thread.sleep(1000)
    }

    /**
     * Остановка записи
     */
    fun stopRecording(cameraId: String) {
        val stopButton = driver.findElement(By.cssSelector("[data-camera-id='$cameraId'] button:contains('Stop'), button[data-testid='stop-recording']"))
        stopButton.click()
        Thread.sleep(2000)
    }

    /**
     * Проверка существования записи
     */
    fun recordingExists(cameraId: String): Boolean {
        try {
            val recordingElement = driver.findElement(By.cssSelector("[data-recording-camera='$cameraId'], .recording-item"))
            return recordingElement.isDisplayed
        } catch (e: NoSuchElementException) {
            return false
        }
    }

    /**
     * Создание пользователя
     */
    fun createUser(username: String, password: String, role: String) {
        val addButton = wait.until {
            driver.findElement(By.cssSelector("button:contains('Add User'), button[data-testid='add-user']"))
        }
        addButton.click()
        Thread.sleep(500)

        val usernameInput = driver.findElement(By.cssSelector("input[name='username']"))
        val passwordInput = driver.findElement(By.cssSelector("input[name='password']"))
        val roleSelect = driver.findElement(By.cssSelector("select[name='role']"))

        usernameInput.clear()
        usernameInput.sendKeys(username)
        passwordInput.clear()
        passwordInput.sendKeys(password)
        
        // Select role
        val select = Select(roleSelect)
        select.selectByValue(role)

        val submitButton = driver.findElement(By.cssSelector("button[type='submit']"))
        submitButton.click()
        Thread.sleep(2000)
    }

    /**
     * Проверка существования пользователя
     */
    fun userExists(username: String): Boolean {
        try {
            val userElement = driver.findElement(By.cssSelector("[data-username='$username']"))
            return userElement.isDisplayed
        } catch (e: NoSuchElementException) {
            try {
                // Fallback: поиск по тексту
                val elements = driver.findElements(By.cssSelector(".user-item, [data-user]"))
                return elements.any { element -> element.text.toString().contains(username) }
            } catch (e2: Exception) {
                return false
            }
        }
    }

    /**
     * Редактирование пользователя
     */
    fun editUser(username: String, newPassword: String? = null) {
        val editButton = wait.until {
            driver.findElement(By.cssSelector("[data-username='$username'] button:contains('Edit')"))
        }
        editButton.click()
        Thread.sleep(500)

        if (newPassword != null) {
            val passwordInput = driver.findElement(By.cssSelector("input[name='password']"))
            passwordInput.clear()
            passwordInput.sendKeys(newPassword)
        }

        val submitButton = driver.findElement(By.cssSelector("button[type='submit']"))
        submitButton.click()
        Thread.sleep(2000)
    }

    /**
     * Удаление пользователя
     */
    fun deleteUser(username: String) {
        val deleteButton = wait.until {
            driver.findElement(By.cssSelector("[data-username='$username'] button:contains('Delete')"))
        }
        deleteButton.click()
        Thread.sleep(500)

        val confirmButton = wait.until {
            driver.findElement(By.cssSelector("button:contains('Confirm'), button:contains('Delete')"))
        }
        confirmButton.click()
        Thread.sleep(2000)
    }

    /**
     * Обновление видео настроек
     */
    fun updateVideoSettings(resolution: String, fps: Int, bitrate: String) {
        val resolutionSelect = driver.findElement(By.cssSelector("select[name='resolution']"))
        val fpsInput = driver.findElement(By.cssSelector("input[name='fps']"))
        val bitrateInput = driver.findElement(By.cssSelector("input[name='bitrate']"))

        // Select resolution
        val select = Select(resolutionSelect)
        select.selectByValue(resolution)

        fpsInput.clear()
        fpsInput.sendKeys(fps.toString())
        bitrateInput.clear()
        bitrateInput.sendKeys(bitrate)
    }

    /**
     * Сохранение настроек
     */
    fun saveSettings() {
        val saveButton = driver.findElement(By.cssSelector("button:contains('Save'), button[type='submit']"))
        saveButton.click()
        Thread.sleep(2000)
    }

    /**
     * Получение видео настроек
     */
    fun getVideoSettings(): VideoSettings {
        val resolution = driver.findElement(By.cssSelector("select[name='resolution']")).getAttribute("value")
        val fps = driver.findElement(By.cssSelector("input[name='fps']")).getAttribute("value")?.toIntOrNull() ?: 30
        val bitrate = driver.findElement(By.cssSelector("input[name='bitrate']")).getAttribute("value") ?: "4000kbps"
        
        return VideoSettings(resolution ?: "1920x1080", fps, bitrate)
    }

    /**
     * Добавление камеры через API
     */
    suspend fun addCameraViaApi(
        name: String,
        url: String = "rtsp://127.0.0.1:8554/test",
        username: String = "admin",
        password: String = "password"
    ): Camera {
        val camera = TestDataFactory.createCamera(
            name = name,
            url = url,
            username = username,
            password = password
        )
        
        val createdCamera = apiClient.createCamera(camera)
        createdCameras.add(createdCamera.id)
        
        return createdCamera
    }

    /**
     * Удаление камеры через API
     */
    suspend fun deleteCameraViaApi(cameraId: String) {
        apiClient.deleteCamera(cameraId)
        createdCameras.remove(cameraId)
    }

    /**
     * Создание пользователя через API
     */
    suspend fun createUserViaApi(
        username: String,
        password: String = "TestPass123!",
        role: String = "OPERATOR"
    ): com.company.ipcamera.shared.domain.model.User {
        val user = TestDataFactory.createUser(
            username = username,
            role = TestDataFactory.stringToUserRole(role)
        )
        
        val createdUser = apiClient.createUser(user)
        createdUsers.add(createdUser.id)
        
        return createdUser
    }

    /**
     * Удаление пользователя через API
     */
    suspend fun deleteUserViaApi(userId: String) {
        apiClient.deleteUser(userId)
        createdUsers.remove(userId)
    }
}

/**
 * Видео настройки
 */
data class VideoSettings(
    val resolution: String,
    val fps: Int,
    val bitrate: String
)
