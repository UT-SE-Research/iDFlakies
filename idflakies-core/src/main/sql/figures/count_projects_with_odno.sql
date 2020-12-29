select count(distinct s.slug)
from confirmation_by_test cbt
inner join original_order o on o.test_name = cbt.test_name
inner join subject s on o.subject_name = s.name
where cbt.confirmed_runs <> cbt.total_runs and cbt.confirmed_runs > 0
