# GST Billing Pro 📊

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-blue?style=for-the-badge)
![Firebase](https://img.shields.io/badge/Firebase-Auth-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)
![Min SDK](https://img.shields.io/badge/Min_SDK-API_24-orange?style=for-the-badge)

> A professional Android application for small businesses to manage invoices, track payments, and generate GST-compliant PDFs — all offline-first.

---

## 📸 Screenshots

| Login | Dashboard | Invoice Management | PDF Preview |
|---|---|---|---|
| ![Login](assets/login.png) | ![Dashboard](assets/dashboard1.png) | ![Invoice](assets/create-invoice.png) | ![PDF](assets/pdf-preview.png) |

---

## 🎯 Problem Statement

Small businesses in India struggle with manual invoice creation, GST calculations, and payment tracking. GST Billing Pro digitizes the entire workflow — from customer onboarding to GST-compliant PDF generation — with offline-first support.

---

## 🚀 Key Features

| Feature | Description |
|---|---|
| 🔐 **Secure Auth** | Google Sign-In via Firebase with per-user data isolation |
| 📈 **Business Dashboard** | Real-time revenue, pending payments & monthly sales charts |
| 📄 **PDF Generation** | GST invoices with CGST/SGST, QR code & digital signature |
| 📑 **Invoice Management** | Full CRUD with Paid / Unpaid / Partial status filters |
| 💰 **Payment Tracking** | Record partial or full payments with history logs |
| 🏢 **Business Profile** | Customizable logo, GSTIN, address per account |

---

## 🛠️ Tech Stack

```
Kotlin  •  Jetpack Compose  •  MVVM  •  Room DB  •  Firebase Auth
Android PdfDocument  •  MPAndroidChart  •  Compose Navigation
```

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Repository Pattern |
| Local DB | Room (with Migrations) |
| Auth | Firebase Auth + Google Sign-In |
| PDF Engine | Android Graphics PdfDocument |
| Charts | MPAndroidChart |
| Navigation | Compose Navigation + ModalNavigationDrawer |

---

## 🏗️ Architecture

```
Jetpack Compose UI
        │
        ▼
   ViewModels (State Management)
        │
        ▼
  Repository Layer (Business Logic)
       / \
      /   \
Room DB   Firebase Auth
      \
   Analytics Engine → PDF Generator
```

**Flow:** User → Compose UI → ViewModel → Repository → Room / Firebase

---

## ⚙️ Setup & Installation

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17+
- A Firebase project with Google Sign-In enabled

### Steps

```bash
# 1. Clone the repo
git clone https://github.com/sanket1035/GSTbillingApp.git

# 2. Open in Android Studio

# 3. Add your Firebase config
# Place google-services.json inside the /app directory

# 4. Build & Run
# Target: API 24+ (Android 7.0 and above)
```

---

## 🧩 Key Challenges Solved

- **Multi-user data isolation** via Firebase UID scoped Room queries
- **Offline-first storage** with Room + reactive Flows
- **Automated GST calculations** (CGST/SGST split)
- **Dynamic PDF generation** with QR codes on-device
- **Revenue analytics** without a backend

---

## 🔮 Roadmap

- [ ] Cloud sync (Firebase Firestore)
- [ ] Customer analytics & ranking
- [ ] AI-based expense prediction
- [ ] GST filing integration (GSTN API)
- [ ] Multi-language support (Hindi, Marathi)

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

<p align="center">Built with ❤️ by <a href="https://linkedin.com/in/sanketchaudhari1035">Sanket Chaudhari</a> for Small Businesses</p>
