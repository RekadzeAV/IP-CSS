package com.company.ipcamera.desktop.ui.common

/**
 * Общие состояния UI для всех экранов
 *
 * Используется для унификации обработки состояний загрузки, успеха, ошибок и пустых состояний
 */
sealed class UiState<out T> {
    /**
     * Состояние загрузки данных
     */
    object Loading : UiState<Nothing>()

    /**
     * Успешное состояние с данными
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * Состояние ошибки
     */
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()

    /**
     * Пустое состояние (нет данных)
     */
    data class Empty(val message: String = "Нет данных") : UiState<Nothing>()

    /**
     * Проверка, является ли состояние успешным
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Проверка, является ли состояние загрузкой
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Проверка, является ли состояние ошибкой
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Проверка, является ли состояние пустым
     */
    val isEmpty: Boolean
        get() = this is Empty

    /**
     * Получить данные, если состояние успешное
     */
    fun getDataOrNull(): T? = (this as? Success)?.data
}
