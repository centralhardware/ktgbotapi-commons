package me.centralhardware.telegram.bot.common

import com.clickhouse.jdbc.ClickHouseDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import java.sql.SQLException
import javax.sql.DataSource

open class BaseClickhouse {

    private val dataSource: DataSource = try {
        ClickHouseDataSource(System.getenv("CLICKHOUSE_URL"))
    } catch (e: SQLException) {
        throw RuntimeException(e)
    }

    protected fun insert(
        userId: Long,
        username: String?,
        firstName: String?,
        lastName: String?,
        isPremium: Boolean,
        isInline: Boolean,
        languageCode: String?,
        text: String,
        botName: String
    ) {
        sessionOf(dataSource).execute(
            //language=GenericSQL
            queryOf(
                """
           INSERT INTO default.bot_log (
                date_time,
                bot_name,
                user_id,
                usernames,
                first_name,
                last_name,
                is_premium,
                is_inline,
                lang,
                text
           ) VALUES (
                now(),
                :bot_name,
                :user_id,
                array(:usernames),
                :first_name,
                :last_name,
                :is_premium,
                :is_inline,
                :lang,
                :text
           ) 
        """, mapOf(
                    "bot_name" to botName,
                    "user_id" to userId,
                    "usernames" to username,
                    "first_name" to firstName,
                    "last_name" to lastName,
                    "is_premium" to isPremium,
                    "is_inline" to isInline,
                    "lang" to languageCode,
                    "text" to text
                )
            )
        )
    }


}