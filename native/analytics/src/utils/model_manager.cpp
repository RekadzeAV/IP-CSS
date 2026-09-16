#include "utils/model_manager.h"
#include <cstring>
#include <fstream>
#include <string>

#ifdef _WIN32
#include <windows.h>
#else
#include <sys/stat.h>
#include <unistd.h>
#endif

bool model_manager_file_exists(const char* path) {
    if (!path || path[0] == '\0') return false;
#ifdef _WIN32
    DWORD att = GetFileAttributesA(path);
    return (att != INVALID_FILE_ATTRIBUTES && !(att & FILE_ATTRIBUTE_DIRECTORY));
#else
    struct stat st;
    return (stat(path, &st) == 0 && S_ISREG(st.st_mode));
#endif
}

size_t model_manager_file_size(const char* path) {
    if (!path) return 0;
    std::ifstream f(path, std::ios::binary | std::ios::ate);
    if (!f.good()) return 0;
    return static_cast<size_t>(f.tellg());
}

size_t model_manager_load_file(const char* path, void* buffer, size_t size) {
    if (!path || !buffer || size == 0) return 0;
    std::ifstream f(path, std::ios::binary);
    if (!f.good()) return 0;
    f.read(static_cast<char*>(buffer), static_cast<std::streamsize>(size));
    return static_cast<size_t>(f.gcount());
}

bool model_manager_validate_path(const char* path) {
    return model_manager_file_exists(path);
}
