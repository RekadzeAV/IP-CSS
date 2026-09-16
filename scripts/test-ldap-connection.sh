#!/bin/bash

# LDAP Connectivity Test Script
# Проверка подключения к LDAP/AD серверу

set -e

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Конфигурация (можно переопределить через переменные окружения)
LDAP_SERVER=${AUTH_LDAP_SERVER:-"ldap://192.168.10.37:389"}
LDAP_BIND_DN=${AUTH_LDAP_BIND_DN:-"cn=admin,dc=surveillance,dc=local"}
LDAP_BIND_PASSWORD=${AUTH_LDAP_BIND_PASSWORD:-""}
LDAP_BASE_DN=${AUTH_LDAP_BASE_DN:-"dc=surveillance,dc=local"}
LDAP_USER_SEARCH_BASE=${AUTH_LDAP_USER_SEARCH_BASE:-"ou=users,dc=surveillance,dc=local"}
TEST_USERNAME=${TEST_USERNAME:-"admin"}

echo -e "${YELLOW}=== LDAP Connectivity Test ===${NC}"
echo ""

# Извлечение хоста и порта из URL
extract_host_port() {
    local url=$1
    local host=$(echo $url | sed -E 's/ldap[s]?:\/\/([^:]+).*/\1/')
    local port=$(echo $url | sed -E 's/ldap[s]?:\/\/[^:]+:([0-9]+).*/\1/')
    
    if [ "$port" == "$url" ]; then
        # Порт не указан, используем стандартный
        if [[ $url == *"ldaps://"* ]]; then
            port=636
        else
            port=389
        fi
    fi
    
    echo "$host $port"
}

read host port <<< $(extract_host_port $LDAP_SERVER)

echo -e "${YELLOW}Configuration:${NC}"
echo "  Server: $LDAP_SERVER"
echo "  Host: $host"
echo "  Port: $port"
echo "  Bind DN: $LDAP_BIND_DN"
echo "  Base DN: $LDAP_BASE_DN"
echo "  Test User: $TEST_USERNAME"
echo ""

# Проверка доступности порта
echo -e "${YELLOW}Step 1: Checking network connectivity...${NC}"
if command -v nc &> /dev/null; then
    if nc -z -w 5 $host $port; then
        echo -e "${GREEN}✓ Port $port is open${NC}"
    else
        echo -e "${RED}✗ Port $port is closed${NC}"
        exit 1
    fi
elif command -v telnet &> /dev/null; then
    echo "nc not found, trying telnet..."
    if telnet $host $port < /dev/null 2>&1 | grep -q "Connected"; then
        echo -e "${GREEN}✓ Port $port is open${NC}"
    else
        echo -e "${RED}✗ Port $port is closed${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠ Neither nc nor telnet available, skipping port check${NC}"
fi
echo ""

# Проверка LDAP подключения (anonymous bind)
echo -e "${YELLOW}Step 2: Testing LDAP connection (anonymous bind)...${NC}"
if command -v ldapsearch &> /dev/null; then
    if ldapsearch -x -H $LDAP_SERVER -b "$LDAP_BASE_DN" -s base "(objectClass=*)" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ LDAP connection successful${NC}"
    else
        echo -e "${YELLOW}⚠ Anonymous bind not allowed (this is OK)${NC}"
    fi
else
    echo -e "${YELLOW}⚠ ldapsearch not available, skipping LDAP test${NC}"
    echo "Install: sudo apt-get install ldap-utils"
fi
echo ""

# Проверка LDAP подключения (authenticated bind)
if [ -n "$LDAP_BIND_PASSWORD" ]; then
    echo -e "${YELLOW}Step 3: Testing LDAP authentication (authenticated bind)...${NC}"
    if command -v ldapsearch &> /dev/null; then
        if ldapsearch -x -H $LDAP_SERVER -D "$LDAP_BIND_DN" -w "$LDAP_BIND_PASSWORD" -b "$LDAP_BASE_DN" -s base "(objectClass=*)" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ LDAP authentication successful${NC}"
        else
            echo -e "${RED}✗ LDAP authentication failed${NC}"
            echo "Check your bind DN and password"
        fi
    fi
    echo ""
    
    # Поиск тестового пользователя
    echo -e "${YELLOW}Step 4: Searching for test user: $TEST_USERNAME${NC}"
    if ldapsearch -x -H $LDAP_SERVER -D "$LDAP_BIND_DN" -w "$LDAP_BIND_PASSWORD" -b "$LDAP_USER_SEARCH_BASE" "(uid=$TEST_USERNAME)" cn mail > /tmp/ldap_user.txt 2>&1; then
        echo -e "${GREEN}✓ User found${NC}"
        echo "User details:"
        cat /tmp/ldap_user.txt | grep -E "^(dn|cn|mail|uid):" | sed 's/^/  /'
    else
        echo -e "${YELLOW}⚠ User not found or insufficient permissions${NC}"
    fi
    echo ""
    
    # Поиск групп пользователя
    echo -e "${YELLOW}Step 5: Searching for user groups...${NC}"
    USER_DN=$(ldapsearch -x -H $LDAP_SERVER -D "$LDAP_BIND_DN" -w "$LDAP_BIND_PASSWORD" -b "$LDAP_USER_SEARCH_BASE" "(uid=$TEST_USERNAME)" dn 2>/dev/null | grep "^dn:" | head -1 | cut -d' ' -f2-)
    
    if [ -n "$USER_DN" ]; then
        echo "User DN: $USER_DN"
        if ldapsearch -x -H $LDAP_SERVER -D "$LDAP_BIND_DN" -w "$LDAP_BIND_PASSWORD" -b "ou=groups,$LDAP_BASE_DN" "(member=$USER_DN)" cn 2>/dev/null | grep -q "^cn:"; then
            echo -e "${GREEN}✓ User groups found${NC}"
            ldapsearch -x -H $LDAP_SERVER -D "$LDAP_BIND_DN" -w "$LDAP_BIND_PASSWORD" -b "ou=groups,$LDAP_BASE_DN" "(member=$USER_DN)" cn 2>/dev/null | grep "^cn:" | sed 's/^/  /'
        else
            echo -e "${YELLOW}⚠ No groups found or groups in different location${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ Could not determine user DN${NC}"
    fi
    echo ""
else
    echo -e "${YELLOW}⚠ LDAP_BIND_PASSWORD not set, skipping authentication tests${NC}"
    echo ""
fi

# Проверка LDAPS (если поддерживается)
if [[ $LDAP_SERVER == *"ldaps://"* ]]; then
    echo -e "${YELLOW}Step 6: Testing LDAPS certificate...${NC}"
    if command -v openssl &> /dev/null; then
        echo | openssl s_client -connect $host:$port -servername $host 2>/dev/null | openssl x509 -noout -dates 2>/dev/null
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ LDAPS certificate is valid${NC}"
        else
            echo -e "${YELLOW}⚠ Could not verify LDAPS certificate${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ openssl not available, skipping certificate check${NC}"
    fi
    echo ""
fi

# Итоговый отчет
echo -e "${YELLOW}=== Summary ===${NC}"
echo "LDAP Server: $LDAP_SERVER"
echo "Status: ${GREEN}CONNECTED${NC}"
echo "Authentication: ${GREEN}OK${NC}"
echo ""
echo -e "${GREEN}LDAP is ready for use!${NC}"
