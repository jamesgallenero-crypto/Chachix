<div align="center">

# 🌸 ChaChix Scents

### Leave a trace they'll remember.

**An Android perfume-shopping application built with Java, XML, and Firebase.**

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Language-Java-ED8B00?logo=openjdk&logoColor=white)
![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?logo=firebase&logoColor=black)
![CameraX](https://img.shields.io/badge/Camera-CameraX-E91E63)
![ML Kit](https://img.shields.io/badge/OCR-Google%20ML%20Kit-4285F4?logo=google&logoColor=white)

[Repository](https://github.com/jamesgallenero-crypto/Chachix) |
[Report an Issue](https://github.com/jamesgallenero-crypto/Chachix/issues)

</div>

---

## 📖 About the Project

**ChaChix Scents** is a native Android perfume-shopping application designed to provide customers with a convenient and user-friendly platform for exploring different fragrances.

The application allows customers to register, log in, browse perfumes, view product information, manage their shopping carts, save wishlist items, and preview checkout details.

It is developed using **Java and XML in Android Studio**, with Firebase Authentication and Cloud Firestore for managing accounts and customer information.

The application also integrates Android CameraX and Google ML Kit Text Recognition for experimental payment-card scanning.

> **Development Status:** ChaChix Scents is currently under development. Authentication, product browsing, local cart management, and checkout previews are available. Real order submission, online payment processing, and delivery tracking are not yet connected.

---

## ✨ Key Features

### 🔐 Authentication
- User registration with email and password.
- Login using Firebase Authentication.
- Password reset through email.
- Input validation.
- User logout.

### 🏠 Home Dashboard
- Personalized customer greeting.
- Customer location from Cloud Firestore.
- Product search functionality.
- Exclusive Offers section.
- Trending perfume products.
- Bottom navigation.

### 🛍️ Perfume Catalog
- Browse the five available fragrances.
- View perfume images and descriptions.
- Open individual product pages.
- Select product size options.
- Use Add to Cart or Buy Now.

### 🛒 Shopping Cart
- Add perfume products.
- Increase or decrease quantities.
- Remove individual items.
- Clear the shopping cart.
- Calculate product subtotal, discounts, and total.
- Store cart information using SharedPreferences.

### ❤️ Wishlist
- View saved favorite perfumes.
- Open wishlist products.
- Add selected perfumes to the cart.
- Remove wishlist entries.

### 💳 Checkout Preview
- Load customer delivery information.
- Display selected products.
- Calculate the estimated total.
- Select Cash on Delivery, Credit/Debit Card, or GCash.
- Review checkout details.

Checkout currently functions as a demonstration. It does not create confirmed orders or charge customers.

### 📷 Card Scanner
- Camera access through CameraX.
- Live camera preview.
- Text recognition using Google ML Kit.
- Experimental card-number detection.
- Scanned information review.

### 👤 Profile and Settings
- View customer information.
- Access wishlist products.
- Navigate to account settings.
- Request password reset.
- Access help and application information.
- Logout.

---

## 🌸 Perfume Collection

<div align="center">

| Scentricity | Richy | Luminous | Kelsey | Candice |
|:---:|:---:|:---:|:---:|:---:|
| <img src="app/src/main/res/drawable/product1.webp" width="110"> | <img src="app/src/main/res/drawable/product2.webp" width="110"> | <img src="app/src/main/res/drawable/product3.webp" width="110"> | <img src="app/src/main/res/drawable/product4.webp" width="110"> | <img src="app/src/main/res/drawable/product5.webp" width="110"> |

</div>

The collection includes five perfumes: **Scentricity, Richy, Luminous, Kelsey, and Candice**.

Customers can browse the collection, select a product, choose the available size, and proceed with their preferred shopping action.

---

## 🔄 Complete Application Flow

### 1. Registration and Login

The application begins with the customer authentication screen.

**For new customers:**

1. Open ChaChix Scents.
2. Select **Create Account**.
3. Enter personal information:
   - Full name
   - Email address
   - Phone number
   - Password
4. Enter the delivery address.
5. Accept the Terms and Conditions.
6. Tap **Sign Up**.
7. Firebase Authentication creates the account.
8. Cloud Firestore saves the customer profile.
9. Return to the login screen.

**For existing customers:**

1. Enter the registered email address.
2. Enter the password.
3. Tap **Login**.
4. Firebase Authentication verifies the credentials.
5. Successful authentication redirects the customer to the Home Dashboard.

The application also supports requesting a password-reset email.

### 2. Home Dashboard

After successful login:

1. The application loads the customer profile from Cloud Firestore.
2. The dashboard displays the customer's name and location.
3. The customer can browse featured offers and trending perfumes.
4. The search bar allows customers to find specific perfume products.
5. Customers can navigate to the Perfumes Catalog, Cart, or Profile.

### 3. Perfume Browsing

1. Open the Perfumes Catalog.
2. Browse the five available perfumes.
3. Select a perfume.
4. View the product details.
5. Choose an available perfume size.
6. Select **Add to Cart** or **Buy Now**.

### 4. Add to Cart

When the customer selects Add to Cart:

1. The application identifies the selected perfume.
2. The product and selected variant are added to the cart.
3. Shopping-cart information is saved using SharedPreferences.
4. The customer can open the Cart screen.
5. The cart displays the products, quantities, prices, discounts, and estimated total.
6. Customers can adjust quantities, remove items, or clear the cart.

The Cart checkout button currently opens a cart-review dialog rather than submitting an order.

### 5. Buy Now and Checkout Preview

When the customer selects Buy Now:

1. The selected product opens in the Payment screen.
2. Customer delivery details are retrieved from Cloud Firestore.
3. The application displays product details and calculates the total.
4. The customer selects a payment method.
5. Available options include:
   - Cash on Delivery
   - Credit/Debit Card
   - GCash
6. The customer taps **Place Order**.
7. The application displays an order-review preview.

**Important:** The current checkout does not process actual payments or save a confirmed order.

### 6. Wishlist

1. Open the Profile screen.
2. Access the Wishlist section.
3. View favorite perfume products.
4. Select a product to open its details.
5. Add a selected wishlist item to the shopping cart.
6. Remove unwanted wishlist entries.

### 7. Account Settings

1. Open Profile.
2. Select Settings.
3. Access available account features:
   - Edit Profile
   - Change Password
   - Notifications
   - Help & Support
   - About ChaChix
   - Logout
4. Selecting Logout signs the customer out through Firebase Authentication.
5. The application returns to the Login screen.

Purchase history, notifications, and delivery tracking are not yet connected to live order records.

---
## 📊 Application Flowchart

mermaid
flowchart TD
    A[Launch ChaChix] --> B[Login Screen]
    B --> C{Registered User?}

    C -->|No| D[Create Account]
    D --> E[Enter Personal and Address Details]
    E --> F[Firebase Authentication]
    F --> G[Save Profile to Cloud Firestore]
    G --> B

    C -->|Yes| H[Enter Email and Password]
    H --> I{Authentication Successful?}

    I -->|No| B
    I -->|Yes| J[Home Dashboard]

    J --> K[Perfume Catalog]
    J --> L[Profile]
    J --> M[Shopping Cart]

    K --> N[Select Perfume]
    N --> O[View Product and Choose Size]
    O --> P{Purchase Action}

    P -->|Add to Cart| Q[Save Product in SharedPreferences]
    Q --> M

    M --> R[Manage Products and Quantity]
    R --> S[Review Cart]
    S --> T[Cart Review Dialog]

    P -->|Buy Now| U[Payment Screen]
    U --> V[Load Delivery Information]
    V --> W[Calculate Total]
    W --> X[Select Payment Method]
    X --> Y[Place Order Button]
    Y --> Z[Order Review Preview]

    L --> AA[Wishlist]
    AA --> N

    L --> AB[Settings]
    AB --> AC{Account Action}
    AC -->|Password| AD[Reset Password]
    AC -->|Profile| AE[Profile Settings]
    AC -->|Logout| AF[Firebase Sign Out]
    AF --> B



The current checkout flows end at review dialogs. Successful order submission and real payment processing are planned features.

---

## 🔌 APIs, SDKs, and Integrations

ChaChix Scents uses Firebase service SDKs and Android libraries to support account management, customer information, camera access, and text recognition.

The application currently has four primary integrations.

| Integration | Technology | Purpose | Status |
|---|---|---|---|
| 1 | Firebase Authentication | Login, registration, logout, password recovery | Connected |
| 2 | Cloud Firestore | Customer information and delivery address | Connected |
| 3 | Android CameraX | Camera preview and image analysis | Integrated |
| 4 | Google ML Kit Text Recognition | Recognition of text visible to the camera | Integrated |

CameraX and ML Kit are Android SDK integrations rather than independently hosted REST APIs.

### 1. Firebase Authentication

*Provider:* Google Firebase

Firebase Authentication manages customer authentication and account access.

*Functions:*
- Register customers.
- Authenticate registered accounts.
- Send password-reset emails.
- Manage authenticated sessions.
- Logout.

*Integration example:*

FirebaseAuth auth = FirebaseAuth.getInstance();

auth.createUserWithEmailAndPassword(email, password);

auth.signInWithEmailAndPassword(email, password);

auth.sendPasswordResetEmail(email);

auth.signOut();

*Authentication flow:*

mermaid
flowchart TD
    A[User Credentials] --> B[Firebase Authentication]
    B --> C{Authentication Successful?}
    C -->|Yes| D[Home Dashboard]
    C -->|No| E[Show Error Message]

*Documentation:*  
https://firebase.google.com/docs/auth/android/start

### 2. Cloud Firestore

*Provider:* Google Firebase

Cloud Firestore stores and retrieves registered customer information.

*Data managed:*
- Full name
- Email address
- Phone number
- Delivery street
- Province
- City
- ZIP code
- Registration timestamp

*Example customer document:*

text
users/
  {userUid}/
    fullName
    email
    phone
    address/
      street
      province
      city
      zipCode
    termsAccepted
    createdAt

*Integration example:*

FirebaseFirestore database =
    FirebaseFirestore.getInstance();

database.collection("users")
    .document(pendingUid)
    .set(pendingProfile);

*Firestore flow:*

mermaid
flowchart TD
    A[Customer Registration] --> B[Create Firebase Account]
    B --> C[Save Profile to Cloud Firestore]
    C --> D[users Collection]
    D --> E[Customer UID Document]
    E --> F[Dashboard and Profile]
    E --> G[Delivery Information]

*Documentation:*  
https://firebase.google.com/docs/firestore/quickstart

### 3. Android CameraX

*Provider:* Android Jetpack

CameraX is used to access the Android device camera.

*Features:*
- Live camera preview.
- Camera permission handling.
- Rear-camera access.
- Image-analysis pipeline.
- Integration with ML Kit.

*Libraries used:*

implementation("androidx.camera:camera-core:1.6.2")
implementation("androidx.camera:camera-camera2:1.6.2")
implementation("androidx.camera:camera-lifecycle:1.6.2")
implementation("androidx.camera:camera-view:1.6.2")

These show one consistent CameraX version for illustration. The repository currently contains both 1.5.3 and 1.6.2 CameraX declarations, which should be consolidated.

*Documentation:*  
https://developer.android.com/media/camera/camerax

### 4. Google ML Kit Text Recognition

*Provider:* Google ML Kit

Google ML Kit provides on-device text recognition for the experimental card scanner.

The scanner processes camera frames and attempts to recognize text from a visible card.

*Features:*
- Text detection from camera frames.
- Text extraction.
- Card-number candidate identification.
- Detected information preview.

*Dependency:*

implementation(
    "com.google.mlkit:text-recognition:16.0.1"
)

*Integration example:*

TextRecognizer recognizer =
    TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    );

*Scanner flow:*

mermaid
flowchart TD
    A[Open Card Scanner] --> B[Request Camera Permission]
    B --> C[CameraX Preview]
    C --> D[Camera Frame Analysis]
    D --> E[Google ML Kit OCR]
    E --> F[Extract Text]
    F --> G[Validate Candidate Card Number]
    G --> H[Review Detected Details]

*Documentation:*  
https://developers.google.com/ml-kit/vision/text-recognition/v2/android

*Important:* This scanner is experimental and is not a secure payment-processing system. Real payment-card details should not be used for testing. Production payment collection should use a secure, compliant payment provider.

### 5. SharedPreferences

The shopping cart uses Android SharedPreferences for local data storage.

*Purpose:*
- Save selected products.
- Store quantities and variants.
- Restore cart data.
- Support cart updates.

*Example:*

SharedPreferences preferences =
    getSharedPreferences(
        "chachix_cart",
        MODE_PRIVATE
    );

This information is stored locally on the device and is not automatically synchronized through Firebase.

### 6. Payment Gateway Integration

The application currently displays the following payment choices:

- Cash on Delivery
- Credit/Debit Card
- GCash

These are checkout UI options and demonstration flows.

*Current limitations:*
- No live GCash transaction API.
- No connected credit-card payment processor.
- No confirmed order creation.
- No payment webhooks.
- No transaction tracking.

A secure payment gateway and order-management backend are planned for future development.
