package com.example.poker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("privacy_policy_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "Политика конфиденциальности",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Дата последнего обновления: 26 сентября 2026 г.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    PolicySection(
                        title = "1. Общие положения",
                        body = "Настоящая Политика конфиденциальности (далее — «Политика») описывает, как индивидуальный предприниматель Мазитов Марат Гафурович (ИНН 235306395455, далее — «Разработчик») обрабатывает информацию пользователей мобильного приложения «Разбор рук» (далее — «Приложение»).\nИспользуя Приложение, вы соглашаетесь с условиями настоящей Политики. Если вы не согласны с ними, пожалуйста, прекратите использование Приложения."
                    )

                    PolicySection(
                        title = "2. Назначение приложения",
                        body = "«Разбор рук» — это локальный аналитический инструмент для визуализации истории турнирных раздач (GGPoker, RedStar/iPoker). Приложение не является азартной игрой, не содержит функционала ставок, онлайн-казино или игр на реальные деньги, а также не выплачивает выигрыши. Приложение предназначено для обучающих и аналитических целей."
                    )

                    PolicySection(
                        title = "3. Какие данные мы собираем",
                        body = "3.1. Данные, которые Приложение НЕ собирает:\nПриложение не собирает, не передаёт и не хранит на сторонних серверах следующие данные:\n• Персональные данные (имя, фамилия, номер телефона, адрес электронной почты)\n• Данные о местоположении\n• Контакты, фотографии, сообщения и другие файлы с вашего устройства\n• Идентификаторы устройства (IMEI, рекламные идентификаторы)\n• Данные о других приложениях, установленных на устройстве\n• Статистику использования и аналитику\n\n3.2. Данные, которые обрабатываются локально:\nПриложение работает полностью офлайн. Все данные обрабатываются и хранятся только на вашем устройстве:\n• Файлы истории раздач (.txt, .xml), которые вы выбираете через системный проводник. Приложение получает доступ к выбранному файлу только для его чтения и отображения. Файлы не копируются на сторонние серверы и не передаются третьим лицам.\n• Настройки Приложения (например, отметка о покупке полного доступа) — хранятся локально на устройстве.\n• Счётчик просмотренных раздач — хранится локально на устройстве."
                    )

                    PolicySection(
                        title = "4. Доступ к файлам на устройстве",
                        body = "При нажатии кнопки «Выбрать турнир» Приложение открывает системный проводник (файловый менеджер) вашего устройства. Это стандартный механизм Android, который позволяет вам самостоятельно выбрать файл истории раздач.\nВажно:\n• Приложение получает доступ только к тому файлу, который вы выбрали вручную.\n• Приложение не имеет доступа к другим файлам, папкам или данным на вашем устройстве.\n• Приложение не сканирует память устройства автоматически.\n• Выбранный файл обрабатывается локально и не передаётся куда-либо."
                    )

                    PolicySection(
                        title = "5. Платежи",
                        body = "Оплата полной версии осуществляется через официальный RuStore In-App Purchases SDK в строгом соответствии с требованиями магазина приложений RuStore. Все платёжные данные (номер карты, банковские реквизиты) обрабатываются исключительно платёжной системой RuStore. Разработчик не получает и не хранит ваши платёжные данные.\nИнформация о факте покупки (флаг «полный доступ активирован») хранится локально на вашем устройстве и/или в системе RuStore.\nОзнакомиться с политикой конфиденциальности RuStore можно на официальном сайте RuStore."
                    )

                    PolicySection(
                        title = "6. Передача данных третьим лицам",
                        body = "Разработчик не передаёт какие-либо данные пользователей третьим лицам, поскольку такие данные не собираются.\nПриложение не использует:\n• Аналитические сервисы (Google Analytics, Firebase и т.п.)\n• Рекламные сети\n• Сервисы сбора статистики\n• Социальные сети и сторонние SDK, собирающие данные"
                    )

                    PolicySection(
                        title = "7. Безопасность",
                        body = "Поскольку Приложение не собирает и не передаёт персональные данные, риски утечки информации минимальны. Все данные, которые вы загружаете в Приложение, остаются на вашем устройстве и защищены стандартными механизмами безопасности Android."
                    )

                    PolicySection(
                        title = "8. Возрастное ограничение",
                        body = "Приложение предназначено для пользователей старше 18 лет. Приложение не предназначено для использования детьми и подростками. Разработчик сознательно не собирает данные от лиц младше 18 лет."
                    )

                    PolicySection(
                        title = "9. Изменения в Политике",
                        body = "Разработчик оставляет за собой право вносить изменения в настоящую Политику. Актуальная версия всегда доступна внутри Приложения в разделе «Политика конфиденциальности». Продолжение использования Приложения после изменений означает согласие с новой редакцией."
                    )

                    PolicySection(
                        title = "10. Контакты",
                        body = "По вопросам, связанным с настоящей Политикой конфиденциальности, вы можете связаться с Разработчиком:\n• Разработчик: ИП Мазитов Марат Гафурович\n• ИНН: 235306395455\n• Email: mazitov_expert@mail.ru"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("Понятно", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column {
        Text(
            text = title,
            color = Color(0xFF38BDF8),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = body,
            color = Color(0xFFCBD5E1),
            fontSize = 11.5.sp,
            lineHeight = 16.sp
        )
    }
}
