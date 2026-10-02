# Learning Dashboard Mobile Application

A production-grade mobile learning dashboard engineered with **Kotlin** and **Jetpack Compose**, implementing **Clean Architecture + MVVM**, **Room Database** for offline-first caching, **Kotlin Coroutines & StateFlow** for reactive unidirectional data flow, and comprehensive unit testing.

---

## 1. Architecture

### Why did you choose your architecture?
We chose **Clean Architecture combined with MVVM (Model-View-ViewModel)** and **Unidirectional Data Flow (UDF)**:

```
┌──────────────────────────────────────────────────────────────┐
│  Presentation Layer (Jetpack Compose + Material 3)            │
│  - Screens: LoginScreen, DashboardScreen, CourseDetailScreen │
│  - ViewModels: StateFlow<UiState> + UI Events (UDF)          │
└──────────────────────────────┬───────────────────────────────┘
                               │ UseCase Invocations
┌──────────────────────────────▼───────────────────────────────┐
│  Domain Layer (Pure Kotlin - Zero Android Framework Ties)     │
│  - Models: Course, Lesson, User, Resource<T>                 │
│  - Use Cases: CalculateProgress, GetCourses, Login, etc.     │
│  - Repository Interfaces: CourseRepository, AuthRepository   │
└──────────────────────────────┬───────────────────────────────┘
                               │ Data Operations
┌──────────────────────────────▼───────────────────────────────┐
│  Data Layer (Repositories & Data Sources)                    │
│  - Repository Impl: CourseRepositoryImpl, AuthRepositoryImpl │
│  - Local Data Source: Room Database (SQLite) + Reactive Flow │
│  - Remote Data Source: MockCourseApiService (Network engine) │
└──────────────────────────────────────────────────────────────┘
```

**Key Reasons for this Choice:**
1. **Separation of Concerns & Testability**: Domain models and use cases have zero dependencies on Android UI frameworks or SQLite drivers, enabling ultra-fast, deterministic JVM unit tests without needing Robolectric or device emulators.
2. **Unidirectional Data Flow (UDF)**: The UI observes immutable `StateFlow<UiState>` emitted by ViewModels and dispatches user intentions (e.g., `toggleLesson()`). This prevents mutable state leakage, race conditions, and inconsistent screen states.
3. **Decoupled Data Sources**: Repositories abstract network and database implementations. If we replace the mock engine with Retrofit/Ktor or migrate from Room to SQLDelight, the domain and presentation layers remain untouched.
4. **Maintainability & Team Scalability**: Clear package boundaries allow multiple senior developers to collaborate on independent features (auth, dashboard, details) without merge conflicts.

---

## 2. Offline Support

### How are you storing and loading offline data?
We implemented the **Single Source of Truth (SSOT)** pattern using **Android Room (SQLite)** and reactive **Kotlin Coroutines Flow**:

1. **Local Schema**:
   - `CourseEntity`: Persists course metadata (`id`, `title`, `instructor`, `progress`, `lessonsCount`).
   - `LessonEntity`: Persists lesson records with foreign key constraints, cascade deletion, and indexing on `(courseId, orderIndex)`.
   - `CourseWithLessons`: Room relation querying the course and its nested lessons atomically.
2. **Loading Strategy (Cache-First / Network-Bound Resource)**:
   - When `CourseRepository.getCoursesStream()` is invoked:
     1. It immediately emits `Resource.Loading`.
     2. It reads any existing cached records from the Room DB and emits `Resource.Success(cachedCourses)` instantly (zero UI blank screens).
     3. It asynchronously triggers a remote refresh via `refreshCourses()`.
     4. When the remote payload arrives, it updates the Room database in a transaction while preserving user completion modifications.
     5. Room automatically invalidates its active query streams and pushes fresh updates through `Flow`.
3. **Graceful Offline Fallback**:
   - If the network request fails (or device goes into Airplane mode / simulated offline), the repository catches the exception. If cached data already exists, the user continues browsing uninterrupted. If the cache is empty, a clean `Resource.Error` is dispatched with a retry trigger.
4. **Interactive Offline Simulator**:
   - The TopAppBar contains a dynamic **Offline / Online filter chip toggle** that switches simulated connectivity on the fly, allowing reviewers to verify offline caching without altering device settings.

---

## 3. Security

### Where would you store authentication tokens in a production application?
In a production Android enterprise application:

1. **Android Keystore System + Jetpack Security (`EncryptedSharedPreferences`)**:
   - Authentication tokens (JWT access & refresh tokens) should be stored in **`EncryptedSharedPreferences`** powered by the **Android Keystore**.
   - The Keystore creates a 256-bit AES cryptographic master key (`MasterKey.DEFAULT_MASTER_KEY_ALIAS`) inside hardware-backed secure execution environments (TEE / StrongBox Keymaster), ensuring cryptographic keys cannot be extracted from application process memory or rooted file dumps.
2. **Short-Lived Access Tokens + In-Memory Caching**:
   - Keep active JWT access tokens strictly in memory (`StateFlow` / memory cache) with short lifespans (15–30 minutes).
   - Only write the refresh token to `EncryptedSharedPreferences`. When access tokens expire, a secure token authenticator (`OkHttp Authenticator`) requests a fresh token using mutual TLS.
3. **Defense in Depth**:
   - **Network Security Config**: Strict HTTPS enforcement with TLS 1.3 and HTTP Public Key Pinning (HPKP / Certificate Pinning) via `CertificatePinner` to prevent MitM attacks.
   - **BiometricPrompt**: For high-security actions, require hardware biometric authentication (Fingerprint / Face Unlock via Android Biometric API) before decrypting the master key.
   - **Process Memory Safety**: Wipe sensitive credential buffers (`CharArray`) immediately after serialization rather than waiting for garbage collection.

---

## 4. Scale

### If this application had 1 million users + hundreds of courses: 3–5 things you would improve

1. **Pagination with Jetpack Paging 3 + `RemoteMediator`**:
   - Replace bulk list retrieval with **Jetpack Paging 3**. Use `RemoteMediator` to coordinate incremental network page fetches (e.g., 20 courses per page) directly into Room with seamless lazy-loaded Compose `LazyPagingItems`.
2. **Edge Caching, HTTP ETags & Delta Synchronization**:
   - Put course catalogs behind a **CDN (Cloudflare / AWS CloudFront)**.
   - Implement HTTP `ETag` and `If-None-Match` headers so clients receive lightweight `304 Not Modified` responses when course contents haven't changed.
   - Use delta/patch synchronization for lesson completions rather than full catalog syncs.
3. **Database Indexing & Full-Text Search (FTS5)**:
   - Implement SQLite composite indices on `(instructor, progress)` and integrate **Room FTS4/FTS5** for sub-millisecond full-text course search over large catalogs.
4. **Reliable Background Sync via WorkManager**:
   - Offload lesson completion synchronizations and offline progress logs to **Android Jetpack WorkManager** with exponential backoff and network constraints (`NetworkType.CONNECTED`), guaranteeing sync even if the user force-quits the app.
5. **Feature-by-Feature Multi-Module Architecture**:
   - Modularize the Gradle codebase into feature and core modules:
     `:core:model`, `:core:database`, `:core:network`, `:core:designsystem`, `:feature:auth`, `:feature:dashboard`, `:feature:detail`.
   - Enables parallel build caching, faster CI/CD pipelines, and dynamic feature delivery.

---

## 5. Second Platform (iOS / macOS Implementation)

### How to implement the same application on Apple platforms (iOS/macOS with Swift / SwiftUI)

1. **UI Layer (SwiftUI + Observation Framework)**:
   - Use declarative **SwiftUI** with `@Observable` macros (iOS 17+) or `ObservableObject` with `@Published` properties.
   - Views: `LoginView`, `DashboardView`, `CourseDetailView` utilizing native `NavigationStack`, `List`, `ProgressView`, and SF Symbols (`checkmark.circle.fill`, `circle`).
2. **Architecture**:
   - Clean Architecture + MVVM matching the Kotlin design.
   - `LoginViewModel`, `DashboardViewModel`, `CourseDetailViewModel` handling view states.
   - Domain layer with Swift protocols for `CourseRepository` and `AuthRepository`.
3. **Concurrency**:
   - Modern **Swift Concurrency**: `async` / `await`, `Task`, `AsyncStream` / `AsyncSequence` for continuous data streaming, and `Actor` types for thread-safe state synchronization.
4. **Offline Persistence**:
   - **SwiftData** (or Core Data) with `@Model class CourseModel` and `@Model class LessonModel` maintaining relationships (`@Relationship(deleteRule: .cascade)`).
   - Local persistent container serving cached data immediately, backed by SQLite.
5. **Security (Keychain Services)**:
   - Store authentication tokens in **iOS Keychain Services** using `kSecClassGenericPassword` with `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` access control.
6. **Testing**:
   - **XCTest** framework verifying progress calculation, ViewModels with `async` expectations, and repository caching mocks.

---

## Project Structure

```
c:/task/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/learning/dashboard/
│   │   │   │   ├── domain/               # Domain Models, Use Cases, Repository Interfaces
│   │   │   │   │   ├── model/           # Course, Lesson, User, Resource
│   │   │   │   │   ├── repository/      # CourseRepository, AuthRepository
│   │   │   │   │   └── usecase/         # CalculateProgress, GetCourses, Detail, Login
│   │   │   │   ├── data/                 # Local & Remote Data Sources, Implementations
│   │   │   │   │   ├── local/           # Room Database, DAOs, Entities
│   │   │   │   │   ├── remote/          # Mock API Service, DTOs, Latency & Error simulation
│   │   │   │   │   └── repository/      # CourseRepositoryImpl, AuthRepositoryImpl
│   │   │   │   ├── presentation/         # Jetpack Compose UI & ViewModels
│   │   │   │   │   ├── login/           # Screen 1: Login UI, Validation, State
│   │   │   │   │   ├── dashboard/       # Screen 2: Dashboard UI, Offline toggle, Course list
│   │   │   │   │   ├── details/         # Screen 3: Details UI, Lesson toggle, Progress recalculation
│   │   │   │   │   ├── components/      # Badges, Banners, Cards, Empty views
│   │   │   │   │   ├── navigation/      # NavHost, Routes
│   │   │   │   │   └── theme/           # Material 3 Theme, Dark/Light palettes, Typography
│   │   │   │   ├── di/                  # Dependency Injection Container (AppContainer)
│   │   │   │   ├── LearningApp.kt       # Application class
│   │   │   │   └── MainActivity.kt      # Main Entry point
│   │   │   └── AndroidManifest.xml
│   │   └── test/                        # 16 Comprehensive Unit Tests (100% Pass Rate)
│   │       └── java/com/learning/dashboard/
│   │           ├── domain/usecase/CalculateProgressUseCaseTest.kt
│   │           ├── data/repository/CourseRepositoryTest.kt
│   │           ├── presentation/login/LoginViewModelTest.kt
│   │           └── presentation/details/CourseDetailViewModelTest.kt
│   └── build.gradle.kts
├── app-debug.apk                        # Compiled Debug APK ready for installation
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Verification & Testing

### 1. Automated Unit Tests (16 Tests - 100% Pass)
Run the test suite using Gradle:
```bash
./gradlew test
```
**Test Coverage Highlights:**
- `CalculateProgressUseCaseTest`: 6 tests verifying progress calculation logic, edge cases (0 lessons, negative values, 100% completion, rounding).
- `CourseRepositoryTest`: 3 tests verifying offline-first Room cache serving, network fallback resilience, and DAO transactions.
- `CourseDetailViewModelTest`: 2 tests verifying lesson completion toggle and reactive progress recalculation.
- `LoginViewModelTest`: 5 tests verifying email regex, password minimum length, loading indicators, error banners, and authentication success.

### 2. APK Compilation
Compile the debug APK:
```bash
./gradlew assembleDebug
```
The verified APK is generated at:
- `app/build/outputs/apk/debug/app-debug.apk`
- Copied to root: `./app-debug.apk`
