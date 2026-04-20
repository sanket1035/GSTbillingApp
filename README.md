# GST Billing Pro 📊

A professional, feature-rich Android application designed for small business owners to manage invoices, track payments, and generate GST-compliant PDFs.

## 🚀 Key Features

- **🔐 Secure Authentication**: Integrated with Google Sign-In for a seamless login experience.
- **👥 Multi-User Data Isolation**: Data is securely isolated per user using Firebase UID and reactive Room flows.
- **📈 Business Dashboard**: Real-time analytics showing total revenue, pending payments, and monthly sales charts.
- **📄 Professional PDF Generation**: 
  - Customizable business profile (Logo, Name, GSTIN, Address).
  - Automated GST calculations (CGST/SGST).
  - Dynamic QR codes for invoice verification.
  - Digital signature placeholder.
- **📑 Invoice Management**:
  - Full CRUD operations for invoices and items.
  - Advanced filtering (Paid, Unpaid, Partial statuses).
  - Quick Action FAB for rapid invoice creation.
- **💰 Payment Tracking**: Record partial or full payments with history logs.
- **📂 Navigation Drawer**: Modern, professional side-drawer navigation for easy access to all modules.

## 📸 Screenshots

| Dashboard | Navigation Drawer | Invoice History |
| :---: | :---: | :---: |
| ![Dashboard](https://via.placeholder.com/300x600?text=Dashboard) | ![Drawer](https://via.placeholder.com/300x600?text=Navigation+Drawer) | ![History](https://via.placeholder.com/300x600?text=Invoice+History) |

| Create Invoice | Business Settings | Generated PDF |
| :---: | :---: | :---: |
| ![Create](https://via.placeholder.com/300x600?text=Create+Invoice) | ![Settings](https://via.placeholder.com/300x600?text=Business+Settings) | ![PDF](https://via.placeholder.com/300x600?text=Invoice+PDF) |


## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room (with Migrations)
- **Networking/Auth**: Firebase Auth & Google Sign-In
- **PDF Engine**: Android Graphics PdfDocument
- **Navigation**: Compose Navigation with ModalNavigationDrawer
- **Charts**: MPAndroidChart

## ⚙️ Setup & Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/sanket1035/GSTbillingApp.git
   ```
2. Add your `google-services.json` to the `app/` directory.
3. Build the project in Android Studio.
4. Run on an emulator or physical device (API 24+).

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
Built with ❤️ for Small Businesses.
