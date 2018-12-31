select count(distinct ftc.subject_name)
from flaky_test_classification ftc
inner join original_order o on ftc.subject_name = o.subject_name and ftc.test_name = o.test_name
where ftc.flaky_type = ?
