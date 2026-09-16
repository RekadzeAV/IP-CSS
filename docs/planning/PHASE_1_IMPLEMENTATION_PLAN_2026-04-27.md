# 🎯 Phase 1 MVP Implementation Plan

**Date:** 27 April 2026  
**Version:** 1.0.0  
**Status:** 🟡 **82% Complete**  
**Target Completion:** 2026-06-15 (6-8 weeks)

---

## 📊 Executive Summary

**Current Status:** Phase 1 MVP at 82% completion  
**Critical Blockers:** 1 (RTSP Client - FFmpeg Integration)  
**Total Tasks:** 12 tasks across 6 workstreams  
**Estimated Time:** 6-8 weeks  
**Confidence Level:** 85%

---

## 🎯 Implementation Strategy

### Priority Matrix

| Priority | Tasks | Estimated Time | Blockers |
|----------|-------|----------------|----------|
| **P0 - Critical** | 1 task | 3-4 weeks | FFmpeg API compatibility |
| **P1 - High** | 4 tasks | 5-7 weeks | RTSP client completion |
| **P2 - Medium** | 5 tasks | 3-4 weeks | Independent |
| **P3 - Low** | 2 tasks | 1-2 weeks | Phase 2 scope |

---

## 📋 Detailed Task Breakdown

### P0 - CRITICAL (Must Complete for MVP)

#### Task P0-1: RTSP Client - FFmpeg Integration
**Priority:** P0 - CRITICAL  
**Owner:** Tech Lead  
**Estimated:** 3-4 weeks  
**Status:** ⚠️ 15% Complete

**Subtasks:**

1. **FFmpeg API Compatibility Fix** (Week 1)
   - [ ] Update FFmpeg dependencies to compatible version
   - [ ] Fix audio decoding API issues (FFmpeg 8.0)
   - [ ] Resolve symbol conflicts in CMake build
   - [ ] Test compilation on all platforms (Linux, Android, iOS)
   - [ ] **Acceptance:** Clean build on all platforms

2. **FFmpeg-Kotlin FFI Completion** (Week 1-2)
   - [ ] Complete JNI bridge for video frames
   - [ ] Implement callback mechanism for decoded frames
   - [ ] Add memory management for native buffers
   - [ ] Create Kotlin wrapper class (RtspClient)
   - [ ] **Acceptance:** Video frames flow from native to Kotlin

3. **Codec Support Implementation** (Week 2-3)
   - [ ] H.264 decoding implementation
   - [ ] H.265/HEVC decoding implementation
   - [ ] MJPEG decoding implementation
   - [ ] Audio codec support (AAC, Opus)
   - [ ] **Acceptance:** Test with 3+ camera models

4. **Integration Testing** (Week 3-4)
   - [ ] Setup test environment with real cameras
   - [ ] Create integration test suite
   - [ ] Test connection stability (24h soak test)
   - [ ] Test codec fallback scenarios
   - [ ] **Acceptance:** 95%+ connection success rate

**Dependencies:**
- None (blocking all other tasks)

**Risks:**
- FFmpeg API changes may require significant refactoring
- Some cameras may use proprietary codecs
- Audio sync issues may require additional work

---

### P1 - HIGH (MVP Requirements)

#### Task P1-1: ONVIF Client - Real Camera Testing
**Priority:** P1 - HIGH  
**Owner:** Backend Developer  
**Estimated:** 1-2 weeks  
**Status:** 🟡 85% Complete

**Subtasks:**

1. **Test Environment Setup** (Day 1-2)
   - [ ] Acquire 3+ different ONVIF camera models
   - [ ] Setup isolated test network
   - [ ] Configure network tools (Wireshark, tcpdump)
   - [ ] **Acceptance:** Network ready for testing

2. **WS-Discovery Testing** (Day 3-5)
   - [ ] Test discovery on different subnets
   - [ ] Test discovery with 100+ devices
   - [ ] Test discovery timeout scenarios
   - [ ] Test UPnP fallback
   - [ ] **Acceptance:** 95%+ discovery success rate

3. **Event Service Testing** (Day 6-8)
   - [ ] Test subscription lifecycle
   - [ ] Test pull messages
   - [ ] Test reconnection scenarios
   - [ ] Test event filtering
   - [ ] **Acceptance:** Events received reliably

4. **Documentation & Bug Fixes** (Day 9-10)
   - [ ] Document known issues
   - [ ] Fix discovered bugs
   - [ ] Update API documentation
   - [ ] **Acceptance:** Test report completed

**Dependencies:**
- None (can run parallel with P0)

---

#### Task P1-2: Video Player - Low Latency Optimization
**Priority:** P1 - HIGH  
**Owner:** Frontend Developer  
**Estimated:** 1 week  
**Status:** 🟡 75% Complete

**Subtasks:**

1. **HLS Optimization** (Day 1-2)
   - [ ] Reduce segment duration (2s → 1s)
   - [ ] Optimize buffer size
   - [ ] Implement adaptive bitrate
   - [ ] **Acceptance:** <3s latency

2. **WebRTC Implementation** (Day 3-5)
   - [ ] Setup WebRTC signaling server
   - [ ] Implement WebRTC player
   - [ ] Test codec negotiation
   - [ ] **Acceptance:** <500ms latency

3. **Performance Testing** (Day 6-7)
   - [ ] Load test with 16 concurrent streams
   - [ ] Memory leak testing
   - [ ] Reconnection stress test
   - [ ] **Acceptance:** <100MB memory per stream

**Dependencies:**
- P0-1: RTSP Client (for live stream testing)

---

#### Task P1-3: PostgreSQL Migration
**Priority:** P1 - HIGH  
**Owner:** Backend Developer  
**Estimated:** 2-3 weeks  
**Status:** ⚠️ 0% Complete

**Subtasks:**

1. **Database Schema Design** (Week 1)
   - [ ] Design PostgreSQL schema
   - [ ] Create migration scripts
   - [ ] Setup connection pooling
   - [ ] **Acceptance:** Schema reviewed

2. **Repository Migration** (Week 2)
   - [ ] Migrate CameraRepository
   - [ ] Migrate RecordingRepository
   - [ ] Migrate EventRepository
   - [ ] **Acceptance:** All repositories working

3. **Testing & Optimization** (Week 3)
   - [ ] Performance testing
   - [ ] Connection pool tuning
   - [ ] Backup/restore procedures
   - [ ] **Acceptance:** Benchmarks met

**Dependencies:**
- None (can run parallel)

---

#### Task P1-4: Certificate Pinning - Production Testing
**Priority:** P1 - HIGH  
**Owner:** Security Engineer  
**Estimated:** 3-5 days  
**Status:** 🟡 90% Complete

**Subtasks:**

1. **Production Environment Setup** (Day 1)
   - [ ] Setup production-like environment
   - [ ] Configure HTTPS with proper certificates
   - [ ] Setup certificate rotation
   - [ ] **Acceptance:** Environment ready

2. **Testing & Validation** (Day 2-3)
   - [ ] Test certificate validation
   - [ ] Test rotation scenarios
   - [ ] Test expired certificate handling
   - [ ] **Acceptance:** All scenarios pass

3. **Documentation** (Day 4-5)
   - [ ] Update security documentation
   - [ ] Create certificate management guide
   - [ ] **Acceptance:** Documentation complete

**Dependencies:**
- None

---

### P2 - MEDIUM (Nice to Have for MVP)

#### Task P2-1: Android UI - RTSP Integration
**Priority:** P2 - MEDIUM  
**Owner:** Mobile Developer  
**Estimated:** 2-3 weeks  
**Status:** ⚠️ 30% Complete

**Subtasks:**

1. **Video Player Component** (Week 1)
   - [ ] Integrate ExoPlayer
   - [ ] Configure HLS playback
   - [ ] Add controls (play/pause/seek)
   - [ ] **Acceptance:** Basic playback working

2. **Camera List & Details** (Week 2)
   - [ ] Camera list screen
   - [ ] Camera detail screen
   - [ ] Live preview
   - [ ] **Acceptance:** UI complete

3. **Recording UI** (Week 3)
   - [ ] Recording list
   - [ ] Playback screen
   - [ ] Export functionality
   - [ ] **Acceptance:** All features working

**Dependencies:**
- P0-1: RTSP Client

---

#### Task P2-2: Redis Rate Limiting
**Priority:** P2 - MEDIUM  
**Owner:** Backend Developer  
**Estimated:** 3-5 days  
**Status:** ⚠️ 0% Complete

**Subtasks:**

1. **Redis Integration** (Day 1-2)
   - [ ] Setup Redis connection
   - [ ] Implement rate limit store
   - [ ] Configure TTL
   - [ ] **Acceptance:** Redis working

2. **Rate Limit Middleware** (Day 3-4)
   - [ ] Implement middleware
   - [ ] Configure limits per endpoint
   - [ ] Add bypass for admin
   - [ ] **Acceptance:** Middleware working

3. **Testing** (Day 5)
   - [ ] Load testing
   - [ ] Edge case testing
   - [ ] **Acceptance:** Performance acceptable

**Dependencies:**
- None

---

#### Task P2-3: Security Headers
**Priority:** P2 - MEDIUM  
**Owner:** Security Engineer  
**Estimated:** 1 day  
**Status:** ⚠️ 0% Complete

**Subtasks:**

1. **Header Configuration** (Day 1)
   - [ ] Configure CSP
   - [ ] Configure HSTS
   - [ ] Configure X-Frame-Options
   - [ ] **Acceptance:** All headers set

2. **Testing** (Day 1)
   - [ ] Test with security scanner
   - [ ] **Acceptance:** No vulnerabilities

**Dependencies:**
- None

---

#### Task P2-4: Server-Side Validation
**Priority:** P2 - MEDIUM  
**Owner:** Backend Developer  
**Estimated:** 1 week  
**Status:** ⚠️ 0% Complete

**Subtasks:**

1. **Validation Library** (Day 1-2)
   - [ ] Setup validation library
   - [ ] Create validation rules
   - [ ] **Acceptance:** Library ready

2. **Endpoint Validation** (Day 3-5)
   - [ ] Add validation to all endpoints
   - [ ] Create error responses
   - [ ] **Acceptance:** All endpoints validated

3. **Testing** (Day 6-7)
   - [ ] Unit tests
   - [ ] Integration tests
   - [ ] **Acceptance:** Tests passing

**Dependencies:**
- None

---

#### Task P2-5: Logging & Monitoring
**Priority:** P2 - MEDIUM  
**Owner:** DevOps Engineer  
**Estimated:** 1 week  
**Status:** ⚠️ 0% Complete

**Subtasks:**

1. **Logging Setup** (Day 1-3)
   - [ ] Configure structured logging
   - [ ] Setup log aggregation
   - [ ] Configure log retention
   - [ ] **Acceptance:** Logs flowing

2. **Monitoring** (Day 4-5)
   - [ ] Setup Prometheus metrics
   - [ ] Create Grafana dashboards
   - [ ] Configure alerts
   - [ ] **Acceptance:** Dashboards working

3. **Documentation** (Day 6-7)
   - [ ] Create runbooks
   - [ ] **Acceptance:** Documentation complete

**Dependencies:**
- None

---

### P3 - LOW (Phase 2 Scope)

#### Task P3-1: iOS UI (SwiftUI)
**Priority:** P3 - LOW  
**Owner:** iOS Developer  
**Estimated:** 4-6 weeks  
**Status:** ❌ 0% Complete

**Dependencies:**
- P0-1: RTSP Client

---

#### Task P3-2: Desktop UI (Compose Desktop)
**Priority:** P3 - LOW  
**Owner:** Desktop Developer  
**Estimated:** 3-4 weeks  
**Status:** ⚠️ 70% Complete

**Dependencies:**
- P0-1: RTSP Client

---

## 📅 Timeline & Milestones

### Week 1-2: Critical Blocker Focus
- [ ] **Week 1:** FFmpeg API compatibility + FFI completion
- [ ] **Week 2:** Codec support + basic integration tests

**Milestone M1:** RTSP Client basic functionality ✅

### Week 3-4: High Priority Tasks
- [ ] **Week 3:** ONVIF testing + PostgreSQL schema
- [ ] **Week 4:** PostgreSQL migration + certificate testing

**Milestone M2:** Core infrastructure ready ✅

### Week 5-6: Integration & Testing
- [ ] **Week 5:** Video player optimization + Android UI
- [ ] **Week 6:** Integration testing + bug fixes

**Milestone M3:** MVP feature complete ✅

### Week 7-8: Final Polish
- [ ] **Week 7:** Performance optimization + security
- [ ] **Week 8:** Final testing + documentation

**Milestone M4:** Phase 1 MVP Ready for Beta ✅

---

## 🚨 Risk Management

### High Risk Items

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| FFmpeg API incompatibility | Medium | High | Fallback to existing H.264 parser |
| Camera codec diversity | High | Medium | Implement codec detection + fallback |
| Performance issues | Medium | Medium | Early performance testing |
| Resource constraints | Medium | High | Prioritize critical paths |

### Contingency Plans

1. **If FFmpeg integration fails:**
   - Use existing HLS streaming as fallback
   - Implement server-side transcoding

2. **If timeline slips:**
   - Defer P2/P3 tasks to Phase 2
   - Focus on P0/P1 critical path only

3. **If testing reveals major issues:**
   - Allocate buffer time (1 week)
   - Prioritize stability over features

---

## ✅ Definition of Done

### For Each Task

- [ ] Code implemented
- [ ] Unit tests written (80%+ coverage)
- [ ] Integration tests passing
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] Performance benchmarks met
- [ ] Security review complete

### For Phase 1 MVP

- [ ] All P0 tasks complete
- [ ] All P1 tasks complete
- [ ] 80%+ P2 tasks complete
- [ ] All critical tests passing
- [ ] Performance benchmarks met
- [ ] Security audit passed
- [ ] Documentation complete
- [ ] Demo ready

---

## 📊 Progress Tracking

### Weekly Checkpoints

**Week 1:** FFmpeg API + FFI (20%)  
**Week 2:** Codec support (40%)  
**Week 3:** ONVIF testing + PostgreSQL (55%)  
**Week 4:** Video player + Certificate (70%)  
**Week 5:** Android UI + Integration (80%)  
**Week 6:** Testing + Bug fixes (90%)  
**Week 7:** Optimization (95%)  
**Week 8:** Final polish (100%)

---

## 📚 References

- [Phase 1 Status Report](../../_to_be_archived/ROOT_FILES/PROJECT_STATUS.md)
- [RTSP Client Documentation](../../archive/docs-duplicates-2026-08-08/RTSP_CLIENT.md)
- [ONVIF Client Documentation](../archive/docs-legacy-2026-04-27/ONVIF_CLIENT.md)
- [Architecture Overview](../ARCHITECTURE.md)

---

**Created:** 27 April 2026  
**Owner:** Tech Lead  
**Next Review:** 2026-05-05 (Weekly)
