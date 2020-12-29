select count(distinct o.subject_name)
from confirmation_by_test cbt
inner join original_order o on o.test_name = cbt.test_name
where cbt.confirmed_runs <> cbt.total_runs and cbt.confirmed_runs > 0
