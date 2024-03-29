# Инструкция по работе с сервисом auth-through-keycloak-and-yandex

Цель обобщить понимание работы с сервисом пользовательских данных из яндекс и базы данных для аутентификации и авторизации
на основе протокола grpc.

## Пошаговое руководство

1. Создаем образ приложения:
```bash
    sbt docker:publishLocal
```
2. Запускаем сервер с пробросом к определенному порту и указанием версии приложения:
```bash
    docker run --rm -p9005:9005 auth-through-yandex:1.0
```
3. Генерацией кода cоздаем клиента  на сервисах, которые будут пользоваться приложением на основе протокола base.proto, 
session.proto, yandex.proto:
```bash
    gedit src/main/protobuf/base.proto
```
```bash
    gedit src/main/protobuf/session.proto
```
```bash
    gedit src/main/protobuf/yandex.proto
```