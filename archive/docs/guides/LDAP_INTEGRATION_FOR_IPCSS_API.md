# LDAP Integration for IP-CSS API

**Date:** 2026-06-09  
**Status:** 🟡 READY TO IMPLEMENT  
**Priority:** 🔴 CRITICAL

---

## 📋 Overview

This document describes the integration of LDAP authentication with IP-CSS API server for user authentication against the NAS LDAP server.

### Target Environment

- **LDAP Server:** 192.168.10.37:389
- **Base DN:** dc=surveillance,dc=local
- **Bind DN:** cn=admin,dc=surveillance,dc=local
- **Cameras:** First contour (192.168.10.17, 20-24, 26)

---

## 🎯 Implementation Plan

### Phase 1: Dependencies Setup

Add required dependencies to `server/api/build.gradle.kts`:

```kotlin
dependencies {
    // Existing dependencies...
    
    // LDAP Authentication
    implementation("org.springframework.ldap:spring-ldap-core:3.1.0")
    implementation("org.springframework.security:spring-security-ldap:6.1.0")
    implementation("com.unboundid:unboundid-ldapsdk:6.0.11")
}
```

---

### Phase 2: Configuration

#### 2.1 Application Properties

Create `server/api/src/main/resources/application-ldap.yml`:

```yaml
spring:
  ldap:
    urls:
      - ldap://${AUTH_LDAP_SERVER:ldap://192.168.10.37:389}
    base: ${AUTH_LDAP_BASE_DN:dc=surveillance,dc=local}
    username: ${AUTH_LDAP_BIND_DN:cn=admin,dc=surveillance,dc=local}
    password: ${AUTH_LDAP_BIND_PASSWORD:}
    
    # Connection settings
    connect-timeout: 5000
    read-timeout: 5000
    
    # Search settings
    search:
      user-dn-patterns:
        - uid={0},ou=users,dc=surveillance,dc=local
      user-search-filter: ${AUTH_LDAP_USER_SEARCH_FILTER:(uid={0})}
      user-search-base: ou=users,dc=surveillance,dc=local
      group-search-base: ou=groups,dc=surveillance,dc=local
      group-search-filter: ${AUTH_LDAP_GROUP_SEARCH_FILTER:(member={0})}

# IP-CSS LDAP Settings
ipcamera:
  ldap:
    enabled: true
    first-contour-cameras: ${STORAGE_FIRST_CONTOUR_CAMERAS:}
    retention-days: ${STORAGE_RETENTION_DAYS:30}
    recording-mode: ${STORAGE_RECORDING_MODE:continuous}
```

#### 2.2 Environment Variables

Use `.env.nas` file:

```bash
# Already configured:
AUTH_LDAP_SERVER=ldap://192.168.10.37:389
AUTH_LDAP_BASE_DN=dc=surveillance,dc=local
AUTH_LDAP_BIND_DN=cn=admin,dc=surveillance,dc=local
AUTH_LDAP_BIND_PASSWORD=<NAS_PASSWORD>
AUTH_LDAP_USER_SEARCH_FILTER=(uid=%(user)s)
AUTH_LDAP_GROUP_SEARCH_FILTER=(member=%(user_dn)s)
```

---

### Phase 3: LDAP Configuration Class

Create `server/api/src/main/kotlin/com/company/ipcamera/server/config/LdapConfig.kt`:

```kotlin
package com.company.ipcamera.server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.LdapContextSource
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch
import org.springframework.security.ldap.search.LdapUserSearch
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider
import org.springframework.security.ldap.authentication.BindAuthenticator
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator

@Configuration
class LdapConfig {
    
    @Bean
    fun ldapContextSource(): LdapContextSource {
        val contextSource = LdapContextSource()
        contextSource.setUrl(System.getenv("AUTH_LDAP_SERVER") ?: "ldap://192.168.10.37:389")
        contextSource.setBase(System.getenv("AUTH_LDAP_BASE_DN") ?: "dc=surveillance,dc=local")
        contextSource.setUserDn(System.getenv("AUTH_LDAP_BIND_DN") ?: "cn=admin,dc=surveillance,dc=local")
        contextSource.setPassword(System.getenv("AUTH_LDAP_BIND_PASSWORD") ?: "")
        contextSource.afterPropertiesSet()
        return contextSource
    }
    
    @Bean
    fun ldapTemplate(contextSource: LdapContextSource): LdapTemplate {
        return LdapTemplate(contextSource)
    }
    
    @Bean
    fun ldapUserSearch(contextSource: LdapContextSource): LdapUserSearch {
        val searchBase = "ou=users,dc=surveillance,dc=local"
        val filter = "(uid={0})"
        return FilterBasedLdapUserSearch(searchBase, filter, contextSource)
    }
    
    @Bean
    fun ldapAuthenticationProvider(
        contextSource: LdapContextSource,
        userSearch: LdapUserSearch
    ): LdapAuthenticationProvider {
        val authenticator = BindAuthenticator(contextSource)
        authenticator.setUserSearch(userSearch)
        
        val authoritiesPopulator = DefaultLdapAuthoritiesPopulator(contextSource, "ou=groups,dc=surveillance,dc=local")
        authoritiesPopulator.setGroupSearchFilter("(member={0})")
        authoritiesPopulator.setSearchSubtree(true)
        
        val provider = LdapAuthenticationProvider(authenticator, authoritiesPopulator)
        provider.setUserDetailsMapper(LdapUserDetailsMapper())
        return provider
    }
    
    @Bean
    fun authenticationManager(
        ldapAuthProvider: LdapAuthenticationProvider
    ): AuthenticationManager {
        return ProviderManager(listOf(ldapAuthProvider))
    }
}
```

---

### Phase 4: Custom UserDetailsService

Create `server/api/src/main/kotlin/com/company/ipcamera/server/security/LdapUserDetailsService.kt`:

```kotlin
package com.company.ipcamera.server.security

import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.repository.UserRepository
import mu.KotlinLogging
import org.springframework.ldap.core.LdapTemplate
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class LdapUserDetailsService(
    private val ldapTemplate: LdapTemplate,
    private val userRepository: UserRepository
) : UserDetailsService {
    
    private val logger = KotlinLogging.logger {}
    
    override fun loadUserByUsername(username: String): UserDetails {
        return try {
            // Search for user in LDAP
            val searchBase = "ou=users,dc=surveillance,dc=local"
            val filter = "(&(objectClass=person)(uid=$username))"
            
            val attributes = ldapTemplate.search(
                searchBase,
                filter,
                arrayOf("uid", "cn", "mail", "description")
            )
            
            if (attributes.isEmpty()) {
                throw UsernameNotFoundException("User not found: $username")
            }
            
            val ldapUser = attributes[0]
            val uid = ldapUser.attributes["uid"]?.toString() ?: username
            val fullName = ldapUser.attributes["cn"]?.toString() ?: username
            val email = ldapUser.attributes["mail"]?.toString() ?: ""
            
            // Get user roles from LDAP groups
            val roles = getGroupsForUser(uid)
            
            // Map to internal User model
            val internalUser = User(
                id = generateId(uid),
                username = uid,
                email = email,
                fullName = fullName,
                role = mapRole(roles),
                permissions = mapPermissions(roles),
                isActive = true
            )
            
            // Save/update user in local database
            userRepository.save(internalUser)
            
            // Create Spring UserDetails
            org.springframework.security.core.userdetails.User
                .withUsername(uid)
                .password("") // LDAP auth uses bind, not password storage
                .authorities(roles.map { SimpleGrantedAuthority("ROLE_$it") })
                .accountLocked(false)
                .disabled(false)
                .build()
                
        } catch (e: Exception) {
            logger.error(e) { "Error loading user from LDAP: $username" }
            throw UsernameNotFoundException("Failed to load user from LDAP: $username", e)
        }
    }
    
    private fun getGroupsForUser(username: String): List<String> {
        val searchBase = "ou=groups,dc=surveillance,dc=local"
        val filter = "(&(objectClass=groupOfNames)(member=uid=$username,ou=users,dc=surveillance,dc=local))"
        
        return ldapTemplate.search(searchBase, filter, arrayOf("cn"))
            .map { it.attributes["cn"]?.toString() ?: "" }
            .filter { it.isNotEmpty() }
    }
    
    private fun mapRole(roles: List<String>): com.company.ipcamera.shared.domain.model.UserRole {
        return when {
            roles.any { it.equals("admin", ignoreCase = true) } -> 
                com.company.ipcamera.shared.domain.model.UserRole.ADMIN
            roles.any { it.equals("operator", ignoreCase = true) } -> 
                com.company.ipcamera.shared.domain.model.UserRole.OPERATOR
            else -> 
                com.company.ipcamera.shared.domain.model.UserRole.VIEWER
        }
    }
    
    private fun mapPermissions(roles: List<String>): List<String> {
        return when {
            roles.any { it.equals("admin", ignoreCase = true) } -> 
                listOf("*")
            roles.any { it.equals("operator", ignoreCase = true) } -> 
                listOf("read:cameras", "write:cameras", "read:recordings", "write:recordings")
            else -> 
                listOf("read:cameras", "read:recordings")
        }
    }
    
    private fun generateId(username: String): String {
        return "ldap-${username.hashCode()}"
    }
}
```

---

### Phase 5: Security Configuration Update

Update `server/api/src/main/kotlin/com/company/ipcamera/server/config/SecurityConfig.kt`:

```kotlin
package com.company.ipcamera.server.config

import com.company.ipcamera.server.security.LdapUserDetailsService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val ldapUserDetailsService: LdapUserDetailsService
) {
    
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/api/v1/health/**").permitAll()
                    .anyRequest().authenticated()
            }
            .authenticationProvider(ldapAuthenticationProvider())
            // Add JWT authentication as fallback
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)
        
        return http.build()
    }
    
    @Bean
    fun ldapAuthenticationProvider() = // Use bean from LdapConfig
        TODO("Reference LdapConfig.ldapAuthenticationProvider()")
    
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
```

---

## 🧪 Testing

### Test LDAP Authentication

```bash
# Test via API
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "<NAS_PASSWORD>"}' \
  -c cookies.txt

# Check cookies
cat cookies.txt
```

### Test User Search

```kotlin
@Test
fun `test LDAP user search`() {
    val userDetailsService = LdapUserDetailsService(ldapTemplate, userRepository)
    val userDetails = userDetailsService.loadUserByUsername("admin")
    
    assertNotNull(userDetails)
    assertEquals("admin", userDetails.username)
    assertTrue(userDetails.authorities.isNotEmpty())
}
```

---

## 📊 Integration Checklist

- [ ] Add LDAP dependencies to build.gradle.kts
- [ ] Create application-ldap.yml configuration
- [ ] Implement LdapConfig class
- [ ] Implement LdapUserDetailsService
- [ ] Update SecurityConfig to use LDAP
- [ ] Test LDAP authentication
- [ ] Test user synchronization
- [ ] Test role mapping
- [ ] Update docker-compose.yml with LDAP env vars
- [ ] Test in Docker container
- [ ] Document LDAP user creation process

---

## 🚀 Deployment

### Docker Compose Update

```yaml
services:
  surveillance-api:
    environment:
      - AUTH_LDAP_SERVER=ldap://192.168.10.37:389
      - AUTH_LDAP_BASE_DN=dc=surveillance,dc=local
      - AUTH_LDAP_BIND_DN=cn=admin,dc=surveillance,dc=local
      - AUTH_LDAP_BIND_PASSWORD=<NAS_PASSWORD>
```

---

*Created: 2026-06-09*  
*Version: 1.0*  
*Status: READY FOR IMPLEMENTATION*
