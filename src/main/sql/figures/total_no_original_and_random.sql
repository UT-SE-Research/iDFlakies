select count(distinct test_name)
from flaky_test_failures
where flaky_type = 'NO'