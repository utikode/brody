# GenApp - Text-to-App Platform

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.x-61dafb.svg)](https://reactjs.org/)
[![AI](https://img.shields.io/badge/AI-9router-purple.svg)](https://9router.decolua.com/)

**GenApp** adalah platform SaaS "Text-to-App" yang memungkinkan pengguna membuat aplikasi Android (.apk) dan iOS (.ipa) tingkat lanjut (E-Commerce, Bisnis) hanya melalui chat di browser web.

## 🚀 Fitur Utama

- **AI-Powered Development**: Menggunakan 9router (Gratis & Unlimited) sebagai AI engine
- **Multi-Platform Output**: Generate aplikasi Android (Kotlin + Jetpack Compose) dan iOS (Swift + SwiftUI)
- **Database Integration**: Integrasi otomatis dengan Supabase/Firebase
- **Monetization Ready**: Integrasi Google AdMob & Facebook Audience Network
- **Real-time Preview**: Preview kode dan visualisasi komponen secara real-time
- **One-Click Build**: Build otomatis menghasilkan file .apk/.ipa

## 🏗️ Arsitektur Sistem

GenApp terdiri dari 5 engine utama:

1. **AI Orchestrator (9router)**: Menerima prompt user, memecahnya menjadi JSON struktur aplikasi
2. **Transpiler Core**: Mengubah JSON menjadi kode native (Kotlin/Swift)
3. **BaaS Engine**: Menyuntikkan kode koneksi database (Supabase/Firebase)
4. **Monetization Engine**: Menyuntikkan kode iklan (Banner, Interstitial, Rewarded)
5. **Build Engine**: Menjalankan Gradle (Android) dan Xcodebuild (iOS)

## 📁 Struktur Proyek

```
genapp/
├── genapp-core/          # Library core dengan model data dan logic
│   ├── src/main/java/com/genapp/core/
│   │   ├── model/        # Java Records (Aplikasi, Layar, Komponen, dll)
│   │   ├── generator/    # Code generators (Compose, SwiftUI)
│   │   └── service/      # AI Service (NineRouterService)
│   └── pom.xml
├── genapp-api/           # Spring Boot REST API
│   ├── src/main/java/com/genapp/api/
│   │   ├── controller/   # REST Controllers
│   │   └── config/       # Configuration classes
│   └── pom.xml
├── frontend/             # React + Vite + TailwindCSS
│   ├── src/
│   │   ├── components/   # UI Components
│   │   ├── services/     # API Services
│   │   └── App.jsx
│   └── package.json
├── android-template/     # Template proyek Android
├── ios-template/         # Template proyek iOS
└── pom.xml               # Maven parent POM
```

## 🛠️ Prasyarat

### Backend
- **Java JDK 17+** (Wajib)
- **Maven 3.8+**
- **Android SDK** (untuk build Android)
- **Xcode** (untuk build iOS, hanya di macOS)
- **Gradle 8+** (termasuk dalam template)

### Frontend
- **Node.js 18+**
- **npm 9+** atau **yarn 1.22+**

### AI Service
- Akses ke endpoint 9router: `https://9router.decolua.com/v1`

## 📦 Instalasi & Setup

### 1. Clone Repository

```bash
cd /workspace
# Repository sudah tersedia
```

### 2. Setup Backend

#### Install dependencies Maven
```bash
cd /workspace/genapp
mvn clean install
```

#### Konfigurasi Environment (Opsional)
Buat file `genapp-api/src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080

# 9router AI Configuration
ninerouter.api.url=https://9router.decolua.com/v1
ninerouter.api.key=${NINEROUTER_API_KEY:}

# Build Configuration
genapp.build.output-dir=/tmp/genapp-builds
genapp.build.android-template-path=./android-template
genapp.build.ios-template-path=./ios-template

# Supabase Configuration (Default)
genapp.supabase.url=https://your-project.supabase.co
genapp.supabase.anon-key=your-anon-key

# Firebase Configuration (Alternative)
genapp.firebase.config-path=./firebase-config.json
```

#### Jalankan Backend
```bash
cd /workspace/genapp/genapp-api
mvn spring-boot:run
```

Backend akan berjalan di `http://localhost:8080`

### 3. Setup Frontend

#### Install dependencies
```bash
cd /workspace/genapp/frontend
npm install
```

#### Jalankan Development Server
```bash
npm run dev
```

Frontend akan berjalan di `http://localhost:5173`

### 4. Setup Android Build Environment

#### Download Android SDK Command Line Tools
```bash
# Di Linux/macOS
mkdir -p ~/android-sdk
cd ~/android-sdk
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip

# Setup environment variables
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

#### Install Required SDK Components
```bash
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
sdkmanager --licenses
```

### 5. Setup iOS Build Environment (macOS Only)

```bash
# Pastikan Xcode terinstall
xcode-select --install

# Accept Xcode license
sudo xcodebuild -license accept

# Install CocoaPods (jika diperlukan)
sudo gem install cocoapods
```

## 🔌 API Endpoints

### POST `/api/chat`
Mengirim pesan user ke AI dan mendapatkan struktur JSON aplikasi.

**Request:**
```json
{
  "message": "Buat aplikasi toko online dengan iklan banner",
  "conversationHistory": []
}
```

**Response:**
```json
{
  "appDefinition": {
    "name": "TokoOnline",
    "packageName": "com.genapp.tokoonline",
    "platform": ["android", "ios"],
    "monetization": {
      "enabled": true,
      "adTypes": ["banner"]
    },
    "database": {
      "type": "supabase",
      "config": {...}
    },
    "screens": [...],
    "components": [...]
  },
  "generatedCode": {
    "kotlin": "...",
    "swift": "..."
  }
}
```

### POST `/api/build`
Membangun aplikasi dari struktur JSON.

**Request:**
```json
{
  "appDefinition": {...},
  "platform": "android"
}
```

**Response:**
```json
{
  "buildId": "uuid-here",
  "status": "started",
  "downloadUrl": null
}
```

### GET `/api/status/{buildId}`
Cek status build real-time.

**Response:**
```json
{
  "buildId": "uuid-here",
  "status": "completed",
  "progress": 100,
  "downloadUrl": "/downloads/app-debug.apk",
  "logs": ["Build started...", "Compiling...", "Done!"]
}
```

## 💻 Penggunaan

### 1. Melalui Web Interface

1. Buka browser dan akses `http://localhost:5173`
2. Ketik prompt di panel chat kiri, contoh:
   - "Buat aplikasi e-commerce dengan fitur login dan keranjang belanja"
   - "Buat aplikasi berita dengan iklan interstitial"
   - "Buat aplikasi todo list dengan database Supabase"
3. Lihat preview kode dan visualisasi di panel kanan
4. Klik tombol **"Build App"** untuk generate APK/IPA
5. Download file hasil build

### 2. Melalui API (cURL)

```bash
# Chat dengan AI
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Buat aplikasi todo list sederhana",
    "conversationHistory": []
  }'

# Build aplikasi
curl -X POST http://localhost:8080/api/build \
  -H "Content-Type: application/json" \
  -d '{
    "appDefinition": {...},
    "platform": "android"
  }'
```

## 🧪 Testing

### Run Unit Tests
```bash
cd /workspace/genapp
mvn test
```

### Run Integration Tests
```bash
cd /workspace/genapp
mvn verify -Pintegration-tests
```

### Frontend Tests
```bash
cd /workspace/genapp/frontend
npm test
```

## 📝 Model Data

### Aplikasi (Root Object)
```java
record Aplikasi(
    String name,
    String packageName,
    List<String> platforms,
    MonetizationConfig monetization,
    DatabaseConfig database,
    List<Layar> screens,
    List<Komponen> components
) {}
```

### Layar (Screen)
```java
record Layar(
    String name,
    String type, // LOGIN, HOME, DETAIL, etc.
    List<Komponen> components,
    String navigationTarget
) {}
```

### Komponen (UI Element)
```java
record Komponen(
    String type, // TEXT, BUTTON, LAZYCOLUMN, TEXTFIELD, etc.
    String label,
    Map<String, Object> properties,
    String dataSource, // e.g., "supabase_products"
    String actionType, // FETCH_DATA, NAVIGATE, SHOW_AD, etc.
    List<Komponen> children
) {}
```

## 🔐 Keamanan

- API Key 9router disimpan di environment variable
- File build disimpan di temporary directory dengan akses terbatas
- CORS configured untuk frontend domain saja
- Rate limiting pada API endpoints (konfigurasi default)

## 🚨 Troubleshooting

### Backend tidak bisa start
```bash
# Cek Java version
java -version

# Harus Java 17+
# Jika tidak, install:
# Ubuntu: sudo apt install openjdk-17-jdk
# macOS: brew install openjdk@17
```

### Build Android gagal
```bash
# Cek ANDROID_HOME
echo $ANDROID_HOME

# Install required SDK components
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Clean and rebuild
cd android-template
./gradlew clean
./gradlew assembleDebug
```

### Frontend tidak connect ke backend
```bash
# Cek backend running
curl http://localhost:8080/api/status

# Update VITE_API_URL di frontend/.env
VITE_API_URL=http://localhost:8080
```

### 9router API error
```bash
# Test endpoint langsung
curl https://9router.decolua.com/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "default",
    "messages": [{"role": "user", "content": "test"}]
  }'
```

## 📄 License

MIT License - lihat [LICENSE](LICENSE) file untuk detail.

## 🤝 Kontribusi

1. Fork repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📞 Support

Untuk pertanyaan dan dukungan:
- GitHub Issues: [Link ke Issues]
- Email: support@genapp.example.com
- Discord: [Link ke Discord Server]

---

**Dibuat dengan ❤️ oleh Tim GenApp**