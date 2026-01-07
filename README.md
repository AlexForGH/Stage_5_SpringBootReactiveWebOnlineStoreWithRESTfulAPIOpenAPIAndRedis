# SpringBootReactiveWebOnlineStoreWithRESTfulAPIOpenAPIAndRedis
## *EN*
#### Link to the previous project based on the Spring Boot and reactive technology stack: https://github.com/AlexForGH/SpringBootReactiveWebOnlineStore.git
#### The project demonstrates development capabilities using Spring Boot on a reactive technology stack with the development of a RESTful service, Open API specification and caching via Redis: Spring WebFlux, Spring Data R2DBC, RESTful API, Open API, Spring Data Redis.
#### Technology stack: Spring Framework, Spring Boot, H2DB, HTML, Thymeleaf, Spring WebFlux, Spring Data R2DBC, Docker (Multi Stage Build), RESTful API, Open API, Spring Data Redis.

### Application features:
    - filling the shopping cart with products
    - editing the shopping cart
    - removing products from the cart
    - searching for products by name
    - sorting products
    - creating product orders, with a check of the user's available balance, which is requested from the payment service via the RESTful API

### Application deployment:
    - Before you begin, you'll need:
            - Java (JRE) (version 23 was used during project development)
            - Docker (Multi Stage Build)
    1. Using an IDE (IntelliJIdea was used during project development):
            - clone the repository
            - open the project in the IDE
            - right‑click on the Dockerfile and select “Run Dockerfile”
            - go to the browser at http://localhost:8080/
            - the application's start page will open
    2. Without an IDE
            - clone the repository
            - run the following Docker commands:
                    - docker compose up --build
            - go to the browser at http://localhost:8080/
                - the application's start page will open.


## *RU*
#### Ссылка на предыдущий проект на основе Spring Boot и реактивного стека технологий: https://github.com/AlexForGH/SpringBootReactiveWebOnlineStore.git
#### Проект для демонстрации возможностей разработки с использованием Spring Boot на реактивном стеке технологий с разработкой RESTful сервиса, спецификации Open API и организации кэширования через Redis: Spring WebFlux, Spring Data R2DBC, RESTful API, Open API, Spring Data Redis.
#### Технологический стек: Spring Framework, Spring Boot, H2DB, HTML, Thymeleaf, Spring WebFlux, Spring Data R2DBC, Docker (Multi Stage Build), RESTful API, Open API, Spring Data Redis.

### Возможности приложения:
    - наполнение корзины товаров
    - редактирование корзины
    - удаление товаров из корзины
    - поиск товаров по названию
    - сортировка товаров
    - создание заказов на товары, с проверкой доступного баланса пользователя, который запрашивается из сервиса оплаты по RESTful API

### Развертывание приложения:
    - Перед началом работы необходимы:
            - Java (JRE) (при разработке проекта использовалась версия 23)
            - Docker (Multi Stage Build)
    1. Через IDE (при разработке проекта использовалась IntelliJIdea):
            - клонировать репозиторий
            - открыть проект в IDE
            - нажать ПКМ на Dockerfile и выбрать "Run Dockerfile"
            - зайти в браузер по адресу http://localhost:8080/
            - откроется стартовая страница приложения
    2. Без использования IDE
            - клонировать репозиторий
            - выполнить команды докера:
                    - docker compose up --build
            - зайти в браузер по адресу http://localhost:8080/
            - откроется стартовая страница приложения.