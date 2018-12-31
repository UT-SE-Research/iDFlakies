select count(distinct s.slug)
from flaky_test_classification ftc
inner join subject s on ftc.subject_name = s.name
inner join original_order o on s.name = o.subject_name and ftc.test_name = o.test_name
where ftc.flaky_type = ?
