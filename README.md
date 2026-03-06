# The Sky Online Service

A **Spring Boot and Thymeleaf web application** that provides online stationery and digital services. Users can browse services, place orders, download announcements, and manage their orders, while administrators can manage services, announcements, and customer orders through a secure dashboard.

---

## 🚀 Features

### Customer Features
- Browse and view online services with details and cost.
- Place orders for services and view order history.
- Download order receipts (PDF format).
- Leave general comments on services.
- Secure login to access personal orders.
- Responsive UI for desktop and mobile devices.

### Admin Features
- Secure admin login and logout.
- Upload, update, and manage announcements with expiration tracking.
- Add, edit, and delete online services.
- View and filter customer service orders by status and service.
- Update order status (Pending → In Progress → Completed).
- Change admin password with real-time password validation.

### General
- Responsive design using **Bootstrap 5**.
- Interactive UI elements including modals, cards, and badges.
- Alerts for success, errors, and notifications.
- Downloadable files for customers and announcements.

---

## 💻 Tech Stack

- **Backend:** Spring Boot  
- **Frontend:** Thymeleaf templates, Bootstrap 5, Bootstrap Icons  
- **Database:** PostgreSQL  
- **Security:** Spring Security for admin authentication  
- **PDF Generation:** Order receipts in PDF format  
- **Tools & Libraries:** Maven, JDK 17, HTML5, CSS3, JavaScript  

---

## 🖼 Screenshots

**Home Page / Announcements**  
![Home Page](./screenshots/home.png)  

**Admin Dashboard / Upload Service**  
![Admin Dashboard](./screenshots/admin-dashboard.png)  

**Customer Orders Page**  
![My Orders](./screenshots/my-orders.png)  

**Service Listing Page**  
![Our Services](./screenshots/services.png)  
.

---

## ⚡ Installation & Setup

1. **Clone the repository**
```bash
git clone https://github.com/meshack-venance/the-sky-online-service.git
cd the-sky-online-service

