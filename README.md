# ice-unistream-api-server
Сервер фасад, для работы с API платежной системы Unistream


для тестирования
запустить проект и отправить
post http://localhost:2277/f9b0c7b2-cc6f-4c11-9e46-1dcd7cb5b6f1
с телом:
{
"CardNumber":"5213240032711354",
"RecipientLastName":"Slavinskiy",
"RecipientFirstName": "Yaroslav",
"AcceptedCurrency": "RUB",
"Amount": 100
}