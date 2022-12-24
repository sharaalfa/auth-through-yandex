# Инструкция по работе с сервисом auth-through-yandex

Цель обобщить понимание работы с сервисом пользовательских данных из яндекс для аутентификации и авторизации
на основе протокола grpc.

## Пошаговое руководство

1. Создаем образ приложения:
```bash
    sbt docker:publishLocal
```
2. Запускаем сервер с пробросом к определенному порту и указанием версии приложения:
```bash
    docker run --rm -p9005:9005 auth-through-yandex:0.1
```
3. Генерацией кода cоздаем клиента  на сервисах, которые будут пользоваться приложением на основе протокола yandex.proto:
```bash
    gedit src/main/protobuf/yandex.proto
```