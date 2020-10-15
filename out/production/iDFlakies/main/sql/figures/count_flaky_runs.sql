select subject_name, flaky_type, test_name, count(*) as number
from flaky_test_info
group by subject_name, flaky_type, test_name;
