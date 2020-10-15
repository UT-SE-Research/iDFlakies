select count(distinct s.slug)
from flaky_test_classification ftc
inner join subject s on ftc.subject_name = s.name
where ftc.flaky_type = ?
