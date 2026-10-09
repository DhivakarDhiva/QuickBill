# QuickBill + QuickKitchen

Author:
Dhivakar

Role:
Android Developer

Year:
2026

Project:
QuickBill POS + QuickKitchen KDS

Primary contributions:

- Android application development (Single APK dual-mode architecture: POS Terminal & Kitchen Display System)
- POS implementation (Order creation, multi-item cart, held carts, payment flows, PDF receipts)
- KDS implementation (Real-time kitchen ticket display, preparation status pipeline, audio & vibration alerts)
- Kotlin / Jetpack Compose (Declarative Material 3 UI, custom design tokens, dark/light theme support)
- Billing (Itemized checkout, subtotal calculations, split tenders across Cash/Card/UPI)
- Tax and discount logic (Indian GST multi-tier rates, CGST/SGST 50-50 splits, flat & percentage discounts)
- Inventory (Live stock management, stock deduction on sale, automatic restock upon refund, low-stock warnings)
- POS → KDS communication (Autonomous zero-cloud local area networking)
- NSD discovery (Android Network Service Discovery via `_quickbill._tcp` service type)
- WebSocket communication (Embedded Java-WebSocket server on KDS, persistent client connection on POS)
- P2P networking (Wi-Fi Direct / Wi-Fi P2P manager for direct device-to-device communication without router)
- Offline synchronization (Room-backed durable Outbox queue in `pending_events`, auto-drain on reconnect)
- Retry mechanism (Automatic exponential reconnection backoff and queued event retry loops)
- Acknowledgement mechanism (Bidirectional `ORDER_ACK` protocol ensuring guaranteed delivery)
- Duplicate prevention (Event deduplication using unique UUID tracking in `received_events`)
- Order state management (Room Database transactional entities, Kotlin Coroutines, and StateFlow architectures)
- UI/UX implementation (Responsive tablet and phone layouts, modal dialogs, status badges, barcode scanner integration)
- Testing (Unit test suites covering tax brackets, tender math, edge cases, inventory restoration, and sync protocols)
- Documentation (System architecture specifications, dual-mode setup guides, network protocol definitions)
