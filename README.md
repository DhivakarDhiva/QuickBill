# QuickBill POS

[![Platform](https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-brightgreen.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target_SDK-35-blue.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-BOM_2024.12.01-informational.svg)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room-2.6.1-orange.svg)](https://developer.android.com/training/data-storage/room)
[![Koin](https://img.shields.io/badge/Koin-4.0.0-critical.svg)](https://insert-koin.io)

An offline-first Android Point of Sale (POS) and inventory management app built with modern Android development practices: **Kotlin 2.1**, **Jetpack Compose (Material 3)**, **Room Database**, **Koin DI**, and **Coroutines / Flow**.

Designed for retail counters, grocery stores, and small businesses needing fast terminal checkouts, GST compliance (CGST + SGST split), barcode scanning, split payments, thermal receipts, and local sales reporting without internet dependencies.

---

## Tech Stack & Architecture Decisions

```
com.quickbill.pos/
├── app/
│   └── QuickBillApp.kt             # Application class; initializes Koin, seeds defaults, restores session
├── data/
│   ├── local/
│   │   ├── QuickBillDatabase.kt    # Room DB definition (entities, converters, versioning)
│   │   ├── Converters.kt           # Room TypeConverters for Enums (PaymentMode, DiscountType, UserRole, BillStatus)
│   │   ├── dao/                    # ProductDao, BillDao, BillItemDao, BillPaymentDao, HeldCartDao, UserDao
│   │   └── entity/                 # ProductEntity, BillEntity, BillItemEntity, BillPaymentEntity, HeldCartEntity, UserEntity
│   ├── model/                      # CartItem, CartSummary, PaymentSplit, DailyReportData, Enums
│   ├── repository/                 # AuthRepository, BillingRepository, ProductRepository, ReportRepository, ThemeRepository
│   ├── seed/                       # SampleDataSeeder (default cashiers & starter product catalog)
│   └── util/
│       ├── BillingCalculator.kt    # Pure Kotlin calculations (tax, discounts, splits, change due)
│       ├── PdfReceiptGenerator.kt  # Android Graphics/PdfDocument 80mm thermal receipt generator
│       ├── CsvExporter.kt          # Storage/Share-compatible CSV report exporter
│       └── NetworkMonitor.kt       # ConnectivityManager Flow-based network observer
├── di/
│   └── AppModule.kt                # Koin dependency injection module (DAOs, Repos, ViewModels)
├── ui/
│   ├── components/                 # Reusable UI widgets, dialogs (TopBar, NavDrawer, ReceiptDialog, PaymentDialog, etc.)
│   ├── navigation/                 # Navigation Compose routes & Screen sealed class
│   ├── screens/
│   │   ├── auth/                   # Cashier login & PIN entry
│   │   ├── billing/                # Terminal cart, barcode lookup, held carts
│   │   ├── dashboard/              # Store analytics summary, quick actions, KPI cards
│   │   ├── history/                # Searchable sales history, calendar range picker, refund
│   │   ├── products/               # Product catalog, SKU duplicate guard, stock adjustments
│   │   └── reports/                # Daily sales breakdown, top selling items, CSV export
│   └── theme/                      # Material 3 color system, shapes, typography, motion specs
```

### Why these libraries?

- **Jetpack Compose + Material 3**: Fully declarative UI with custom thermal-style receipt previews, adaptive layouts (phones & POS tablets), and fluid animations.
- **Koin 4.0**: Lightweight dependency injection. Avoids heavy annotation-processing overhead (kapt) associated with Dagger/Hilt, keeping build times fast and test setup straightforward with `koinViewModel()`.
- **Room 2.6.1 + KSP**: Offline-first local persistence. Relational integrity across bills, line items, and payments. Room `@Transaction` blocks are used for checkout and refund stock-restoration routines.
- **Kotlinx Coroutines & Flow**: Reactive data streams from Room DAOs to ViewModel `StateFlow`s, collected in Compose via `collectAsState()`.
- **CameraX 1.4.1 + Google ML Kit Barcode Scanning**: On-device SKU/barcode scanning through camera feed with an overlay reticle.
- **Android `PdfDocument`**: Native receipt rendering without third-party PDF SDK bloat. Direct export via standard Android share sheet for printing or messaging.
- **BigDecimal Math**: All currency, discount, and tax calculations are handled with `BigDecimal` and `RoundingMode.HALF_UP` to prevent floating-point paise rounding errors.

---

## Architectural & Business Logic Assumptions

1. **Strict Offline-First**:
   - The app does not require a remote server to complete sales, manage stock, or generate reports.
   - A `NetworkMonitor` observer detects connectivity changes and displays an offline status indicator in the top bar, but terminal operations are never blocked by network state.
2. **Atomic Inventory Transactions**:
   - When a sale completes, item stocks are decremented in a single database transaction. If an item does not have enough stock, the checkout fails cleanly.
   - Refunding or voiding a bill updates its status to `REFUNDED` and rolls back inventory stock for all associated line items inside a database `@Transaction`.
3. **Non-Destructive Soft Deletes**:
   - Deleting a product sets `isArchived = 1` rather than issuing a raw SQL `DELETE`. This preserves foreign key references and historical sales records for past bills and daily reports.
4. **GST Tax Structure (Indian GST Standard)**:
   - Each product holds a tax rate percentage (0%, 5%, 12%, 18%, or 28%).
   - GST is split equally between **CGST** and **SGST** (e.g., 18% GST = 9% CGST + 9% SGST).
   - Taxes are calculated against the net taxable subtotal (after line-item and apportioned bill discounts).
5. **Discount Guardrails**:
   - Per-item and whole-bill discounts support both Percentage (%) and Flat (₹) values.
   - Percentage discounts are clamped between 0% and 100%.
   - Flat discounts cannot exceed the gross line-item or bill subtotal.
6. **Cashier Sessions & Role Enforcement**:
   - User sessions are persisted in encrypted/private `SharedPreferences`. When the app is closed and reopened, the logged-in session is restored until an explicit logout.
   - Actions like clearing sales history are restricted to `UserRole.ADMIN`.
7. **Appearance Preferences**:
   - Supports System Default, Light Mode, and Dark Mode.
   - The user's selection is persisted in `SharedPreferences` and loaded before first frame composition.

---

## Developer Setup & Build Instructions

### Prerequisites
- **Android Studio**: Ladybug (2024.2+) or Meerkat (recommended).
- **JDK**: Version 17 or 21 (Android Studio bundled JBR works out of the box).
- **Android SDK**:
  - `compileSdk`: 35
  - `minSdk`: 26 (Android 8.0 Oreo)
  - `targetSdk`: 35
  - Build-Tools: `35.0.0`

### Build from Command Line

Set your `JAVA_HOME` pointing to your JDK or Android Studio's bundled JBR:

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

**macOS / Linux (Bash):**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr"
./gradlew assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Running Unit Tests

Run the test suite across tax calculations, inventory logic, edge cases, payments, and theme persistence:

```bash
# Windows
.\gradlew.bat testDebugUnitTest

# macOS / Linux
./gradlew testDebugUnitTest
```

HTML test reports are generated at:
```
app/build/reports/tests/testDebugUnitTest/index.html
```

### Installing via ADB

Connect an Android device with USB debugging enabled or start an emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.quickbill.pos/.MainActivity
```

---

## Pre-Seeded Default Accounts & Catalog

If the database is empty upon initial install, QuickBill seeds starter accounts and sample products:

### Accounts
| Role | Full Name | Username | PIN | Permissions |
|---|---|---|---|---|
| **Store Manager** | Store Manager | `admin` | `1234` | Full access (Billing, Catalog CRUD, History, Reports, Clear Sales) |
| **Cashier 1** | Rahul Sharma | `cashier1` | `0000` | Billing, Product viewing, Standard sales history |
| **Cashier 2** | Priya Patel | `cashier2` | `1111` | Billing, Product viewing, Standard sales history |

*Note: The login screen contains 1-tap quick buttons to sign in with any demo account directly.*

### Sample Catalog
Includes 19 pre-configured grocery and retail items across categories (Groceries, Dairy, Beverages, Snacks, Personal Care, Household) with varying GST brackets (0% to 28%) and simulated stock levels (including low-stock and out-of-stock items for edge-case testing).

---

## Screen Layouts & Functional Walkthrough

### 1. Cashier Login (`LoginScreen.kt`)
```
+---------------------------------------------------+
|               [QuickBill POS Logo]                |
|               Sign In to Terminal                 |
|                                                   |
|   [ Quick Login:  (Admin)  (Cashier 1)  (Cashier 2) ]  |
|                                                   |
|             Selected: Rahul Sharma (Cashier)       |
|                  PIN: [ * * * * ]                 |
|                                                   |
|                  [ 1 ] [ 2 ] [ 3 ]                |
|                  [ 4 ] [ 5 ] [ 6 ]                |
|                  [ 7 ] [ 8 ] [ 9 ]                |
|                  [ C ] [ 0 ] [ ⌫ ]                |
|                                                   |
|                [ UNLOCK TERMINAL ]                |
+---------------------------------------------------+
```
- 4-digit PIN authentication with haptic feedback.
- Quick switch buttons for seamless cashier handovers.
- Session persistence across app restarts.

### 2. Dashboard (`DashboardScreen.kt`)
```
+---------------------------------------------------+
| ☰ QuickBill POS            [● Online] [Theme] [Avatar] |
+---------------------------------------------------+
| Good Afternoon, Rahul Sharma                      |
| [ Today's Sales: ₹14,250 ] [ Orders: 38 ]         |
| [ Items Sold: 142       ] [ Low Stock: 3 ]        |
|                                                   |
| HOURLY SALES TREND                                |
|  ₹ |    █                                         |
|    |  █ █   █                                     |
|    +--6A-9A-12P-3P-6P-9P------------------------- |
|                                                   |
| QUICK ACTIONS                                     |
| [ New Sale ]  [ Add Product ]  [ Daily Report ]   |
|                                                   |
| TOP SELLING ITEMS                                 |
| 1. Basmati Rice 1kg            24 sold  (₹2,880)  |
| 2. Roasted Coffee Beans        18 sold  (₹5,760)  |
+---------------------------------------------------+
```
- Real-time KPI summaries for today's volume.
- Interactive hourly sales bar chart.
- Low stock warning banner linking directly to filtered product inventory.

### 3. POS Billing Terminal (`BillingScreen.kt`)
```
+---------------------------------------------------+
| [🔍 Search product or SKU... ] [📷 Scan Barcode]   |
| [All] [Groceries] [Dairy] [Beverages] [Snacks]    |
+-----------------------------------+---------------+
| Products Grid                     | Active Cart   |
| +-------------------------------+ | Item 1   x2   |
| | Amul Butter 500g      ₹275.00 | | Item 2   x1   |
| | GST: 12% | Stock: 18 left     | | ------------- |
| +-------------------------------+ | Subtotal:  ₹- |
| | Greek Yogurt 100g      ₹60.00 | | Disc (%):  ₹- |
| | [LOW STOCK] | Stock: 2 left   | | CGST:      ₹- |
| +-------------------------------+ | SGST:      ₹- |
| | Full Cream Milk 1L     ₹68.00 | | Grand Total₹- |
| | [OUT OF STOCK - Disabled]     | | [Hold] [Pay]  |
+-----------------------------------+---------------+
```
- Fast catalog filtering via text or camera barcode scanner.
- Line item quantity increment/decrement, item discount configuration, and line total breakdown.
- Cart holding functionality to park transactions and resume anytime from the top bar.

### 4. Payment & Split Tender Modal (`PaymentDialog.kt`)
```
+---------------------------------------------------+
| Total Due: ₹840.00                                |
| Select Payment Method:                            |
| [ Cash ]     [ Card ]     [ UPI ]     [ Split ]   |
|                                                   |
| [Cash Mode Selected]                              |
| Tendered: [ ₹1000.00                            ] |
| Quick Add:  [Exact]  [+50]  [+100]  [+500]        |
|                                                   |
| ------------------------------------------------- |
| Total Paid: ₹1000.00     Change Due: ₹160.00      |
|                                                   |
| [ Cancel ]                 [ Complete Sale & Print ] |
+---------------------------------------------------+
```
- Multi-tender support: Cash, Card, UPI, and Split tender.
- Real-time change due calculator for cash payments.
- Dynamic UPI QR display simulation and Card transaction reference capture.

### 5. Thermal Receipt & PDF Export (`ReceiptDialog.kt`)
```
+---------------------------------------------------+
|               QUICKBILL SUPERMARKET               |
|            GSTIN: 29ABCDE1234F1Z5                 |
| Bill #: QB-20261004-0012    Date: 04/10/2026      |
| Cashier: Rahul Sharma                             |
| ------------------------------------------------- |
| ITEM               QTY     RATE      AMOUNT       |
| Basmati Rice 1kg    2    120.00      240.00       |
| Amul Butter 500g    1    275.00      275.00       |
| ------------------------------------------------- |
| Subtotal:                           ₹515.00       |
| CGST:                                ₹22.50       |
| SGST:                                ₹22.50       |
| Grand Total:                        ₹560.00       |
| ------------------------------------------------- |
| Payment: CASH                        ₹600.00      |
| Change Due:                           ₹40.00      |
|                                                   |
| [ Close ]         [ Share PDF ]       [ Print ]   |
+---------------------------------------------------+
```
- Formatted 80mm thermal receipt preview.
- Direct PDF rendering via Android `PdfDocument` with Android Share sheet intent.

### 6. Product Management (`ProductsScreen.kt`)
```
+---------------------------------------------------+
| Inventory (19 Products)           [+ New Product] |
| [🔍 Search by name / SKU ]   [Filter: Low Stock]  |
+---------------------------------------------------+
| Product Item Card                                 |
| Basmati Rice (1kg)            SKU: 890103000101   |
| Category: Groceries           Price: ₹120.00      |
| Tax: 5% GST                   Stock: 45 units     |
| [ -1 ] [ +1 ] [ +10 ]         [ Edit ] [ Delete ] |
+---------------------------------------------------+
```
- Full product CRUD with duplicate SKU validation dialog.
- Fast inline stock steppers (`-1`, `+1`, `+10`).
- Non-destructive soft deletion (`isArchived = 1`).

### 7. Sales History & Refund Management (`SalesHistoryScreen.kt`)
```
+---------------------------------------------------+
| Sales History                                     |
| [🔍 Search Bill # or Customer ]                   |
| Filter: [Today] [Yesterday] [Last 7 Days] [Custom]|
+---------------------------------------------------+
| Bill #QB-20261004-0003       ₹740.00  [COMPLETED] |
| 04 Oct 2026, 02:15 PM • Cashier: Priya Patel     |
| Items: 3 • Payment: UPI                           |
| [ View Receipt ]                   [ Issue Refund]|
+---------------------------------------------------+
```
- Comprehensive transaction history with custom calendar date-range filters.
- Detailed receipt dialog inspection.
- Refund execution with automatic Room `@Transaction` inventory restock.

### 8. Daily Reports & Analytics (`DailyReportScreen.kt`)
```
+---------------------------------------------------+
| Daily Performance Report             [Export CSV] |
| Selected Date: [ 04 Oct 2026 ▾ ]                  |
|                                                   |
| Gross Sales: ₹18,450     Net Sales: ₹17,900       |
| Total Bills: 42          Refunds: 1 (₹550)        |
|                                                   |
| PAYMENT BREAKDOWN                                 |
| Cash: ₹9,200 (51%) | Card: ₹5,100 | UPI: ₹3,600   |
|                                                   |
| ALL ITEMS SOLD (Click to inspect all lines)       |
+---------------------------------------------------+
```
- Full day-end reconciliation metrics.
- Modal inspection of all items sold with quantities and generated revenues.
- CSV export via standard Android share targets.

---

## Automated Test Coverage

The unit test suite validates core business logic independently from the Android UI lifecycle:

| Test Class | Purpose | Key Scenarios Tested |
|---|---|---|
| `TaxUnitTest` | GST calculation rules | 0%, 5%, 12%, 18%, 28% GST brackets; CGST/SGST 50-50 splits; paise rounding with `HALF_UP`. |
| `PaymentUnitTest` | Cash & tender math | Exact cash payment; excess tender change calculation; split payments across Cash, Card, and UPI; remaining balance checks. |
| `InventoryUnitTest` | Stock integrity | Stock decrements on checkout; out-of-stock validation; low-stock threshold triggers; inventory restoration on bill refund. |
| `EdgeCaseBillingUnitTest` | Input boundaries | 100% discount clamping; flat discount exceeding item price; empty cart subtotals; zero-tax grocery staples. |
| `ThemeUnitTest` | Appearance state | Persistence of `ThemeMode.SYSTEM`, `ThemeMode.LIGHT`, `ThemeMode.DARK` and default fallback behavior. |

---

## License

This project is licensed under the MIT License.
