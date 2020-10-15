select count(distinct subject_name)
from flaky_test_counts
where number > 0;

