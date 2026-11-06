# Monetra - Personal Finance Manager

A modern, professional Android application for personal finance management built with Kotlin, Jetpack Compose, and modern Android development best practices.

---

## 1. Project Overview

**Monetra** helps users manage accounts, track income and expenses, organize categories, and monitor budgets and savings goals. It supports both English (LTR) and Persian (RTL) localization and light/dark theme customization.

---

## 2. Technology Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Architecture:** Lightweight MVVM + MVI
- **Dependency Injection:** Hilt
- **Local Database:** Room
- **State Management:** StateFlow & Kotlin Flow
- **Preferences:** DataStore Preferences
- **Navigation:** Navigation Compose (Type-safe Navigation)
- **Image Loading:** Coil
- **Serialization:** Kotlinx Serialization
- **Security:** Android Biometric API
- **Build System:** Gradle Kotlin DSL with Version Catalog (`libs.versions.toml`)
- **Testing:** JUnit, MockK, Coroutines Test, Turbine

---

## 3. Architecture Explanation

Monetra uses a clean, simple 3-tier architecture with lightweight MVI concepts:

```
Compose UI  --->  ViewModel  --->  Repository  --->  Local Data Source (Room/DataStore)
```

### Key Rules:
- **UI Layer:** Composables observe UI state via `collectAsStateWithLifecycle()` and send user actions (`UiEvent`) to the `ViewModel`.
- **State & Side Effects:** ViewModels expose immutable `StateFlow<UiState>` for UI state and `Channel<UiEffect>` as `Flow` for single-shot side effects (navigation, snackbars).
- **Domain Layer:** Pure Kotlin models (`Account`, `Transaction`, `Category`) and repository interfaces. No unnecessary `UseCase` classes for basic CRUD.
- **Data Layer:** Local data source using Room DAOs and DataStore preferences. Mappers convert between database entities (`AccountEntity`) and domain models (`Account`).

---

## 4. Package Structure

```
app/
 ├── FinanceApplication.kt
 ├── MainActivity.kt
 ├── core/
 │    ├── common/          # Result, AppException, DispatcherProvider
 │    ├── database/        # FinanceDatabase, DatabaseModule
 │    ├── datastore/       # PreferencesManager, DataStoreModule
 │    ├── designsystem/    # Theme (Color, Type, Shape, Theme) & Reusable Components
 │    ├── navigation/      # NavigationRoutes, NavigationDestination, AppNavigation
 │    ├── security/        # BiometricManager
 │    └── utils/           # DateUtils, CurrencyUtils, Constants
 ├── data/
 │    ├── local/           # Room DAOs, Entities, Converters
 │    ├── mapper/          # Entity <-> Domain Model Mappers
 │    └── repository/      # Repository Implementations & Hilt RepositoryModule
 ├── domain/
 │    ├── model/           # Account, Transaction, Category, TransactionType
 │    └── repository/      # Repository Interfaces
 └── feature/              # Feature modules (UI, ViewModels, States, Events, Effects)
      ├── dashboard/
      ├── transactions/
      ├── accounts/
      ├── categories/
      ├── budgets/
      ├── savings/
      ├── analytics/
      ├── debts/
      └── settings/
```

---

## 5. Database Design Overview

The app uses **Room** for local database persistence:

- **Monetary Amounts:** Stored as `Long` integers (cents or currency units) to avoid floating-point inaccuracies.
- **Timestamps:** Stored as `Long` UTC milliseconds.
- **Entities & Relationships:**
  - `AccountEntity`: Primary key `id` (Long), columns for name, type, balance, currency, icon, color, default status, timestamps.
  - `CategoryEntity`: Primary key `id` (Long), columns for name, type, icon, color, system flag.
  - `TransactionEntity`: Primary key `id` (Long), foreign keys to `AccountEntity` and `CategoryEntity` with indexes on `account_id`, `category_id`, and `timestamp`.

---

## 6. How to Build and Run

1. Open the project in **Android Studio** (Ladybug or newer).
2. Sync the project with Gradle files (`File -> Sync Project with Gradle Files`).
3. Select an emulator or connected device running **Android 8.0 (API level 26)** or higher.
4. Click **Run** (`Shift + F10`) or run via Gradle command line:
   ```bash
   ./gradlew assembleDebug
   ```
5. To execute unit tests:
   ```bash
   ./gradlew test
   ```

---

## 7. Coding Conventions

- **Immutability:** Use `data class` with `val` properties for UI states and entities.
- **Naming:** Follow standard Kotlin guidelines (CamelCase for classes/types, camelCase for functions/variables).
- **Error Handling:** Use custom `AppException` sealed hierarchy and `Result<T>` wrappers.
- **No Hardcoded Strings:** User-facing strings must be declared in `strings.xml` supporting English (`values-en`) and Persian (`values-fa`).

---

## 8. How to Add a New Feature

To add a new feature (e.g., `feature/budgets`):

1. **Domain Model & Repository:** Add necessary domain model data classes in `domain/model/` and interface in `domain/repository/` if new operations are required.
2. **Local Data Source:** Add Room Entity in `data/local/entity/` and DAO methods in `data/local/dao/`. Update `FinanceDatabase`.
3. **Repository Implementation:** Implement the repository contract in `data/repository/` and bind in `RepositoryModule`.
4. **Feature UI Stack:** Under `feature/budgets/`, create:
   - `BudgetsUiState.kt`
   - `BudgetsUiEvent.kt`
   - `BudgetsUiEffect.kt`
   - `BudgetsViewModel.kt`
   - `BudgetsScreen.kt`
5. **Navigation:** Register the new `@Serializable` route in `NavigationRoutes.kt` and add a `composable` entry in `AppNavigation.kt`.
6. **Unit Tests:** Add unit tests for ViewModel and Repository in `app/src/test/java/`.
