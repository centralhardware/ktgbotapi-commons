select distinct country_days_tracker.country
from country_days_tracker;

alter table country_days_tracker delete where country = ''