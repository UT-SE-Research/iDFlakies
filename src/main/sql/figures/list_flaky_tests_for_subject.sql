select distinct subject_name,
                flaky_type,
                test_name
from flaky_test_info
-- NOTE: If you change :name, then you will also need to change scripts/list_flaky_tests.sh
where subject_name like '%:name%';
