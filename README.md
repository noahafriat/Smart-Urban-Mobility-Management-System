# Smart Urban Mobility Management System (SUMMS)
**SOEN 343: Software Architecture and Design**

---

**Repository link**: https://github.com/noahafriat/Smart-Urban-Mobility-Management-System

---

### **Team Members**

| Name | Student ID | Role |
| :--- | :--- | :--- |
| **Noah Afriat** | 40276193 | **Team Leader** / Full Stack Developer |
| **Robert Mounsef** | 40279248 | Backend Developer |
| **Jakson Rabinovitch** | 40285726 | Backend Developer |
| **Paul Nasr** | 40282491 | Frontend Developer |
| **Tala Khraim** | 40276410 | Frontend Developer |

---

### Tech Stack
**Frontend**: Vue 3 (Pinia, Vue Router, TypeScript)  
**Backend**: Java Spring Boot 3

---

### How to Run

#### 1. Start the Backend
Open a terminal in the `backend` folder and use the Maven Wrapper:
```bash
cd backend
./mvnw spring-boot:run
```
*The backend API will be available at `http://localhost:8080/api`*

*(Note: If `JAVA_HOME` is not set, provide the path to your JDK, for example: `JAVA_HOME="/c/Program.../jbr" ./mvnw spring-boot:run`)*

#### 2. Start the Frontend
Open a new terminal in the `frontend` folder, install dependencies, and start the development server:
```bash
cd frontend
npm install
npm run dev
```
*The web app will be accessible at `http://localhost:5173`*
