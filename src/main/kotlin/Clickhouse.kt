import com.clickhouse.jdbc.ClickHouseDataSource
import dev.inmo.tgbotapi.types.chat.CommonUser
import kotliquery.queryOf
import kotliquery.sessionOf
import org.telegram.telegrambots.meta.api.objects.User
import java.sql.SQLException
import javax.sql.DataSource

class Clickhouse {

    val dataSource: DataSource = try {
        ClickHouseDataSource(System.getenv("CLICKHOUSE_URL"))
    } catch (e: SQLException) {
        throw RuntimeException(e)
    }

    fun log(text: String, isInline: Boolean, user: CommonUser?, botName: String) {
        user?.let {
            insert(
                it.id.chatId,
                if (it.username != null) it.username!!.full else null,
                it.firstName,
                it.lastName,
                it.isPremium,
                isInline,
                it.languageCode,
                text,
                botName)
        }
    }

    fun log(text: String, isInline: Boolean, user: User?, botName: String){
        user?.let {
            insert(
                it.id,
                if (it.userName != null) it.userName else null,
                it.firstName,
                it.lastName,
                it.isPremium,
                isInline,
                it.languageCode,
                text,
                botName)
        }
    }

    private fun insert(userId: Long,
                       username: String?,
                       firstName: String?,
                       lastName: String?,
                       isPremium: Boolean,
                       isInline: Boolean,
                       languageCode: String?,
                       text: String,
                       botName: String){
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