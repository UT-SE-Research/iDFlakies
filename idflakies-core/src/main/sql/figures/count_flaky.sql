select i.number
from
(
  select flaky_type, count(*) as number
  from flaky_test_classification
  group by flaky_type
) i
where i.flaky_type = ?