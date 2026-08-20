# Project Analysis: Company Data Search Service

## Executive Summary
The project demonstrates good architecture and understanding of Spring Boot, but has **critical issues** that prevent it from running and some documentation gaps. Below is a detailed evaluation against requirements.

---

## Requirements Checklist

### ✅ Core Requirements Status

| Requirement | Status | Notes |
|---|---|---|
| **API Endpoint** | ✓ DONE | GET `/search?q=<query>` with optional `forceRefresh` parameter |
| **Company Model** | ✓ DONE | All required fields present (number, name, status, type, dates, address) |
| **Officers** | ✓ DONE | List with name, role, appointment date |
| **PSC (Persons with Significant Control)** | ✓ DONE | List with name and nature of control |
| **Caching** | ✓ DONE | 24-hour TTL with SearchCache JPA entity |
| **Storage** | ✓ DONE | H2 embedded database |
| **Politeness** | ✓ DONE | User-Agent, 500ms delays, max 100 companies |

### ✅ Optional Extensions Status

| Feature | Status | Notes |
|---|---|---|
| **Failure Handling** | ✓ DONE | Try-catch blocks in scraper with error logging |
| **forceRefresh Flag** | ✓ DONE | Implemented in API |
| **Multiple Queries** | ✗ NOT DONE | Would enhance usability |
| **Deduplication** | ✓ PARTIAL | Officers and PSC deduplicated, but no cross-company merge |
| **Tests** | ✓ BASIC | SearchControllerTest exists but limited scope |

### ✅ Code Quality

| Aspect | Status | Notes |
|---|---|---|
| **Clean Code** | ✓ GOOD | Well-organized, readable, good separation of concerns |
| **Architecture** | ✓ GOOD | Controller → Service → Scraper pattern is solid |
| **Error Handling** | ✓ GOOD | Catches and logs scraping failures |
| **Performance** | ⚠ CAUTION | Synchronous scraping could be slow for large result sets |

---

## Critical Issues Found

### 🔴 **Issue #1: Java Version Mismatch (BLOCKER)**
- **Problem**: Project requires Java 17, but system has Java 11
- **Impact**: Project will NOT COMPILE
- **Solution**: Either downgrade pom.xml to Java 11, or install Java 17+
- **Code Location**: `pom.xml` line with `<java.version>17</java.version>`

### 🔴 **Issue #2: Incorrect Jackson Import (BLOCKER)**
- **Problem**: Line in `CompanySearchService.java`:
  ```java
  import tools.jackson.core.JacksonException;
  import tools.jackson.databind.json.JsonMapper;
  ```
- **Should be**: 
  ```java
  import com.fasterxml.jackson.core.JacksonException;
  import com.fasterxml.jackson.databind.json.JsonMapper;
  ```
- **Impact**: COMPILATION ERROR - wrong package path
- **Location**: `CompanySearchService.java` lines ~4-5

### 🟡 **Issue #3: Missing Jackson Dependency Configuration**
- **Problem**: `JsonMapper` bean is autowired but never created
- **Impact**: Runtime injection failure
- **Solution**: Add Spring Bean configuration for JsonMapper

### 🔴 **Issue #4: Empty README.md**
- **Problem**: README is empty/corrupted (shows only `#   C o m p a n y S e a r c h A p p l i c a t i o n  `)
- **Impact**: Critical documentation missing
- **Required Coverage**:
  - ✗ How to run the project
  - ✗ Database setup instructions
  - ✗ Example request/response
  - ✗ Caching strategy explanation
  - ✗ Hardest part reflection
  - ✗ What's not finished/improvements

---

## Code Analysis

### Strengths ✅

1. **Good Architecture**
   - Clear separation: Controller → Service → Scraper
   - Repository pattern for data access
   - @Component/@Service annotations properly used

2. **Solid Scraping Logic**
   - Robust HTML parsing with Jsoup
   - Multiple fallback strategies for extracting data
   - Handles edge cases (missing data, malformed HTML)
   - Deduplication logic for PSC and Officers

3. **Thoughtful Caching**
   - TTL-based invalidation (24 hours)
   - Query normalization (lowercase, whitespace trimming)
   - Honors `forceRefresh` flag

4. **Politeness Implemented**
   - 500ms delay between requests
   - Custom User-Agent
   - Max 100 companies per search
   - Respects server load

5. **Test Coverage**
   - Unit test for controller exists
   - Uses Mockito for isolation

### Weaknesses & Improvements 🔧

1. **Potential Issues**
   - **Synchronous processing**: Fetching 100 companies sequentially could take 50+ seconds (100 × 500ms)
   - **No retry logic**: If a company fetch fails, it's silently skipped (could use exponential backoff)
   - **No API validation**: Empty query returns empty list (should return 400 error)
   - **No request logging**: Hard to debug issues

2. **Missing Features**
   - **Multiple queries**: Cannot search for multiple companies in one call
   - **Batch retry**: No mechanism to retry failed company fetches
   - **Rate limiting on cache**: No per-IP rate limiting
   - **Search result pagination**: No support for "page 2" of results

3. **Testing Gaps**
   - No integration tests
   - No scraper unit tests
   - No tests for date parsing, deduplication, or cache expiration
   - SearchControllerTest only tests happy path

4. **Data Model Gaps**
   - No @JsonProperty annotations (could cause serialization issues)
   - No equals/hashCode for deduplication to work correctly
   - No toString for debugging

5. **Performance**
   - 24-hour cache might be too long for dynamic data
   - No async/parallel fetching capability

---

## Caching Strategy Review

**Current Strategy**: 24-hour TTL with optional refresh

**Pros:**
- Simple to understand and implement
- Reasonable balance between freshness and load reduction
- forceRefresh flag provides escape hatch

**Cons:**
- 24 hours could be stale for dissolved companies (requirement mentions "active last week might be dissolved today")
- Could implement shorter TTL (2-6 hours) for better freshness
- No granular refresh (e.g., refresh only specific query)

**Suggestion**: Document the tradeoff and consider shortening TTL to 6 hours.

---

## Recommendations (Priority Order)

### 🔴 MUST FIX (Before submission)
1. **Fix Java version** - Change to 11 or install Java 17
2. **Fix Jackson import** - Correct package path
3. **Add JsonMapper bean** - Create configuration class
4. **Complete README.md** - Document everything
5. **Test build & startup** - Verify `mvn clean package` and `java -jar`

### 🟡 SHOULD FIX (Improves quality)
6. Add validation to search endpoint (reject empty queries with 400)
7. Add @JsonProperty annotations to domain classes
8. Improve test coverage (add scraper tests, cache tests)
9. Add logging (SLF4J) instead of System.err
10. Add equals/hashCode to Officer and PSC for better deduplication

### 💡 NICE TO HAVE (If time permits)
11. Add request logging to see actual API flow
12. Add exception handling to SearchController
13. Implement retry logic for failed company fetches
14. Add pagination support for large result sets
15. Add performance metrics (request duration, cache hit rate)

---

## Next Steps

1. **Fix compilation issues** (30 minutes)
   - Fix Jackson import
   - Fix Java version mismatch
   - Add JsonMapper configuration

2. **Test the build** (15 minutes)
   - Run `mvn clean package`
   - Run the application
   - Test with curl

3. **Write comprehensive README** (45 minutes)
   - Setup instructions
   - Example requests/responses
   - Caching explanation
   - Reflection on challenges

4. **Add basic improvements** (1 hour)
   - Input validation
   - Better logging
   - Improve test coverage

5. **Final validation** (15 minutes)
   - Verify everything works end-to-end
   - Double-check README completeness

---

## Estimated Effort to Fix
- **Critical fixes**: 1-2 hours
- **Quality improvements**: 2-3 hours
- **Total to production-ready**: 3-5 hours

The core logic is solid; the issues are primarily configuration and documentation.
