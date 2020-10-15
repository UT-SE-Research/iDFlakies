select count(distinct ftc.subject_name)
from flaky_test_classification ftc
where ftc.flaky_type = ?;

