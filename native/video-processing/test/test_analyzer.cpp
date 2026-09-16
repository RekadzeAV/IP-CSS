#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>
#include <string>
#include <map>
#include <algorithm>
#include <iomanip>

// Простой анализатор результатов тестов
// Парсит вывод тестов и создает отчет

struct TestResult {
    std::string name;
    bool passed;
    std::string message;
    double duration_ms;
};

struct TestSummary {
    int total;
    int passed;
    int failed;
    std::vector<TestResult> results;
};

TestSummary parse_test_output(const std::string& filename) {
    TestSummary summary = {0, 0, 0, {}};

    std::ifstream file(filename);
    if (!file.is_open()) {
        std::cerr << "Failed to open file: " << filename << std::endl;
        return summary;
    }

    std::string line;
    TestResult current;
    bool in_test = false;

    while (std::getline(file, line)) {
        // Поиск начала теста
        if (line.find("Running test:") != std::string::npos) {
            if (in_test) {
                summary.results.push_back(current);
            }

            size_t pos = line.find("Running test:") + 13;
            current.name = line.substr(pos);
            // Удаление пробелов
            while (!current.name.empty() && current.name[0] == ' ') {
                current.name.erase(0, 1);
            }
            current.passed = false;
            current.message = "";
            current.duration_ms = 0.0;
            in_test = true;
            summary.total++;
        }
        // Поиск результата теста
        else if (line.find("PASSED") != std::string::npos) {
            current.passed = true;
            summary.passed++;
            summary.results.push_back(current);
            in_test = false;
        }
        else if (line.find("FAILED") != std::string::npos) {
            current.passed = false;
            summary.failed++;
            // Попытка извлечь сообщение об ошибке
            size_t pos = line.find("FAILED");
            if (pos != std::string::npos && pos + 6 < line.length()) {
                current.message = line.substr(pos + 6);
            }
            summary.results.push_back(current);
            in_test = false;
        }
        // Поиск метрик производительности
        else if (line.find("FPS:") != std::string::npos) {
            // Извлечение FPS
            size_t pos = line.find("FPS:");
            if (pos != std::string::npos) {
                std::string fps_str = line.substr(pos + 4);
                // Можно парсить FPS для анализа
            }
        }
    }

    if (in_test) {
        summary.results.push_back(current);
    }

    return summary;
}

void generate_html_report(const TestSummary& summary, const std::string& output_file) {
    std::ofstream html(output_file);
    if (!html.is_open()) {
        std::cerr << "Failed to create HTML report: " << output_file << std::endl;
        return;
    }

    html << "<!DOCTYPE html>\n";
    html << "<html><head><title>Test Results</title>\n";
    html << "<style>\n";
    html << "body { font-family: Arial, sans-serif; margin: 20px; }\n";
    html << "h1 { color: #333; }\n";
    html << ".summary { background: #f5f5f5; padding: 15px; border-radius: 5px; margin-bottom: 20px; }\n";
    html << ".passed { color: green; }\n";
    html << ".failed { color: red; }\n";
    html << "table { border-collapse: collapse; width: 100%; }\n";
    html << "th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n";
    html << "th { background-color: #4CAF50; color: white; }\n";
    html << "tr:nth-child(even) { background-color: #f2f2f2; }\n";
    html << "</style>\n";
    html << "</head><body>\n";

    html << "<h1>Test Results Report</h1>\n";

    html << "<div class=\"summary\">\n";
    html << "<h2>Summary</h2>\n";
    html << "<p><strong>Total:</strong> " << summary.total << "</p>\n";
    html << "<p class=\"passed\"><strong>Passed:</strong> " << summary.passed << "</p>\n";
    html << "<p class=\"failed\"><strong>Failed:</strong> " << summary.failed << "</p>\n";
    html << "<p><strong>Success Rate:</strong> "
         << std::fixed << std::setprecision(2)
         << (summary.total > 0 ? (summary.passed * 100.0 / summary.total) : 0.0) << "%</p>\n";
    html << "</div>\n";

    html << "<h2>Test Details</h2>\n";
    html << "<table>\n";
    html << "<tr><th>Test Name</th><th>Status</th><th>Message</th></tr>\n";

    for (const auto& result : summary.results) {
        html << "<tr>\n";
        html << "<td>" << result.name << "</td>\n";
        if (result.passed) {
            html << "<td class=\"passed\">PASSED</td>\n";
        } else {
            html << "<td class=\"failed\">FAILED</td>\n";
        }
        html << "<td>" << result.message << "</td>\n";
        html << "</tr>\n";
    }

    html << "</table>\n";
    html << "</body></html>\n";
}

void generate_markdown_report(const TestSummary& summary, const std::string& output_file) {
    std::ofstream md(output_file);
    if (!md.is_open()) {
        std::cerr << "Failed to create Markdown report: " << output_file << std::endl;
        return;
    }

    md << "# Test Results Report\n\n";
    md << "## Summary\n\n";
    md << "- **Total:** " << summary.total << "\n";
    md << "- **Passed:** " << summary.passed << "\n";
    md << "- **Failed:** " << summary.failed << "\n";
    md << "- **Success Rate:** "
       << std::fixed << std::setprecision(2)
       << (summary.total > 0 ? (summary.passed * 100.0 / summary.total) : 0.0) << "%\n\n";

    md << "## Test Details\n\n";
    md << "| Test Name | Status | Message |\n";
    md << "|-----------|--------|----------|\n";

    for (const auto& result : summary.results) {
        md << "| " << result.name << " | ";
        md << (result.passed ? "✅ PASSED" : "❌ FAILED") << " | ";
        md << result.message << " |\n";
    }
}

int main(int argc, char* argv[]) {
    if (argc < 2) {
        std::cout << "Usage: test_analyzer <test_output_file> [--html] [--markdown]" << std::endl;
        std::cout << "  --html: Generate HTML report" << std::endl;
        std::cout << "  --markdown: Generate Markdown report" << std::endl;
        return 1;
    }

    std::string input_file = argv[1];
    bool generate_html = false;
    bool generate_markdown = false;

    for (int i = 2; i < argc; i++) {
        if (std::string(argv[i]) == "--html") {
            generate_html = true;
        } else if (std::string(argv[i]) == "--markdown") {
            generate_markdown = true;
        }
    }

    std::cout << "Analyzing test results from: " << input_file << std::endl;

    TestSummary summary = parse_test_output(input_file);

    std::cout << "\n=== Test Summary ===" << std::endl;
    std::cout << "Total: " << summary.total << std::endl;
    std::cout << "Passed: " << summary.passed << std::endl;
    std::cout << "Failed: " << summary.failed << std::endl;
    std::cout << "Success Rate: "
              << std::fixed << std::setprecision(2)
              << (summary.total > 0 ? (summary.passed * 100.0 / summary.total) : 0.0) << "%" << std::endl;

    if (generate_html) {
        std::string html_file = input_file + ".html";
        generate_html_report(summary, html_file);
        std::cout << "HTML report generated: " << html_file << std::endl;
    }

    if (generate_markdown) {
        std::string md_file = input_file + ".md";
        generate_markdown_report(summary, md_file);
        std::cout << "Markdown report generated: " << md_file << std::endl;
    }

    return (summary.failed > 0) ? 1 : 0;
}
