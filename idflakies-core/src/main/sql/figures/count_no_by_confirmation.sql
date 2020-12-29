select count(*)
from flaky_test_classification ftc
inner join confirmation_by_test cbt on ftc.test_name = cbt.test_name
where cbt.confirmed_runs <> cbt.total_runs;

