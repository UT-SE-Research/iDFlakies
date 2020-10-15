select count(*)
from confirmation_by_test
where confirmed_runs <> total_runs and confirmed_runs > 0