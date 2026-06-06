OPTIMIZE TABLE metrics FINAL;
OPTIMIZE TABLE workout FINAL;
OPTIMIZE TABLE workout_heart_rate_data FINAL;
OPTIMIZE TABLE workout_heart_rate_recovery FINAL;
OPTIMIZE TABLE workout_routes FINAL;
optimize table workout_step_count_log final ;
optimize table workout_walking_running_distance final ;


select workout_name, workout_start, count(*)
from workout_routes
group by workout_name, workout_start
order by workout_start desc;