# TODO: Fix Java compilation errors

**Approved Plan Steps:**
1. [x] Edit RegisterRequest.java: Uncomment the jakarta.validation import
2. [x] Edit pom.xml: Add spring-boot-starter-validation dependency  
3. [x] (Optional) Clean up commented import in AuthController.java
4. [x] Run `mvn clean compile` to verify **[BUILD SUCCESS]**
5. [x] Test application startup with `mvn spring-boot:run`

<<<<<<< HEAD
### Phase 1: Core Models & DB Setup ✅
- [x] Create TODO.md
- [x] Update compose.yaml (add app service, env vars)
- [x] Create Dockerfile
- [x] Update application.yaml (datasource, JPA, profiles)
- [x] Create entities: Role.java, User.java, Product.java, Order.java
- [x] Create repos for each

### Phase 2: Security & Auth (Next)
- [ ] SecurityConfig.java
- [ ] UserDetailsServiceImpl.java (in service)
- [ ] AuthController.java + login/register.html

### Phase 2: Security & Auth
- [ ] SecurityConfig.java
- [ ] UserDetailsServiceImpl.java (in service)
- [ ] AuthController.java + login/register.html

### Phase 3: Business Logic
- [ ] DTOs (UserDTO, ProductDTO, OrderDTO, etc.)
- [ ] Services: UserService, ProductService, OrderService
- [ ] Controllers: ProductController, OrderController, HomeController + views

### Phase 4: Exception Handling & Views
- [ ] GlobalExceptionHandler.java
- [ ] Thymeleaf templates (dashboards per role)

### Phase 5: Testing
- [ ] 15 unit tests (services)
- [ ] 3 integration tests (controllers)

### Phase 6: DevOps & Deploy
- [x] GitHub Actions ci-cd.yml
- [ ] Update README.md (diagrams, endpoints, instructions)
- [ ] Test local: mvn test, docker compose up
- [ ] Git push, Render deploy

Updated after each step.
=======
>>>>>>> ac0cad1ddb15438d264bf28337f6586338c1797a
