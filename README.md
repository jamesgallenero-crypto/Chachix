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
