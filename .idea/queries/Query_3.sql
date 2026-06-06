select count(*), toDate(date_time)
from country_days_tracker_bot.country_days_tracker
group by toDate(date_time)
order by toDate(date_time) desc