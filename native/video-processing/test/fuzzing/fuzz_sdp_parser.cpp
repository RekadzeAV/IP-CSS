/**
 * @file fuzz_sdp_parser.cpp
 * @brief Fuzzing тест для SDP парсера
 * 
 * Компиляция с libFuzzer:
 * g++ -fsanitize=fuzzer,address -I../include -I../src \
 *   fuzz_sdp_parser.cpp ../src/rtsp_client.cpp -o fuzz_sdp
 */

#include <cstdint>
#include <cstring>
#include <string>

// Forward declaration из rtsp_client.cpp
extern "C" {
    struct RTSPClient;
}

// Имитация функции парсинга SDP
// В реальном проекте это будет вызов parse_sdp()
static bool parse_sdp_fuzz_target(const char* data, size_t size) {
    if (!data || size == 0) return false;
    
    // Защита от слишком больших данных
    if (size > 1024 * 1024) return false; // 1MB лимит
    
    std::string sdp(data, size);
    
    // Базовая валидация
    if (sdp.empty()) return false;
    
    // Проверка на минимальную структуру SDP
    bool has_v = sdp.find("v=") != std::string::npos;
    bool has_o = sdp.find("o=") != std::string::npos;
    bool has_s = sdp.find("s=") != std::string::npos;
    bool has_t = sdp.find("t=") != std::string::npos;
    
    // SDP должен иметь минимальную структуру
    if (!has_v || !has_o || !has_s || !has_t) {
        return false;
    }
    
    // Здесь должна быть полная логика parse_sdp()
    // Для fuzzing теста используем упрощённую версию
    
    return true;
}

// libFuzzer entry point
extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
    // Защита от нулевых указателей
    if (!data || size == 0) {
        return 0;
    }
    
    // Ограничение размера для производительности
    if (size > 1024 * 1024) {
        return 0;
    }
    
    try {
        parse_sdp_fuzz_target(reinterpret_cast<const char*>(data), size);
    } catch (...) {
        // Игнорируем исключения
    }
    
    return 0;
}

// Main для standalone тестирования
#include <iostream>
#include <fstream>

int main(int argc, char** argv) {
    if (argc < 2) {
        std::cerr << "Usage: " << argv[0] << " <sdp_file>" << std::endl;
        return 1;
    }
    
    std::ifstream file(argv[1], std::ios::binary);
    if (!file) {
        std::cerr << "Error: Cannot open file " << argv[1] << std::endl;
        return 1;
    }
    
    std::vector<uint8_t> data((std::istreambuf_iterator<char>(file)),
                               std::istreambuf_iterator<char>());
    
    LLVMFuzzerTestOneInput(data.data(), data.size());
    
    std::cout << "Fuzz test completed for: " << argv[1] << std::endl;
    return 0;
}
