#ifndef MODEL_MANAGER_H
#define MODEL_MANAGER_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdbool.h>
#include <stddef.h>

/** Проверить существование файла модели по пути */
bool model_manager_file_exists(const char* path);

/** Прочитать размер файла в байтах; возвращает 0 при ошибке */
size_t model_manager_file_size(const char* path);

/** Загрузить файл в буфер. buffer должен быть выделен (size байт). Возвращает прочитанные байты или 0 при ошибке */
size_t model_manager_load_file(const char* path, void* buffer, size_t size);

/** Проверить, что путь не пустой и файл доступен */
bool model_manager_validate_path(const char* path);

#ifdef __cplusplus
}
#endif

#endif /* MODEL_MANAGER_H */
