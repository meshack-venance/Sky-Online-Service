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
```

2. **Create your local secrets file**
```bash
cp config/application-secrets.yaml.example config/application-secrets.yaml
```

3. **Choose the profile**
```bash
# default
export SPRING_PROFILES_ACTIVE=dev

# for production
export SPRING_PROFILES_ACTIVE=prod
```

4. **Set real secrets outside Git**
- Keep real database passwords and keys in environment variables or `config/application-secrets.yaml`.
- Do not commit `config/application-secrets.yaml`.
- `application-dev.yaml` and `application-prod.yaml` no longer store DB credentials.
- Put `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` in `config/application-secrets.yaml` or environment variables.
- Example local PostgreSQL URL is `jdbc:postgresql://localhost:5432/sky_online_db_local`.

5. **Bootstrap the first admin explicitly**
- The app no longer creates a default `admin/password` account.
- To create the first admin on an empty database, set `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD` in your environment or in `config/application-secrets.yaml`.
- After the first admin record exists, those bootstrap values are no longer needed for normal startup.

