select count(distinct i.name)
from
(
  select si.name,
         sum(ifnull(flaky.number, 0)) as flaky_n,
         sum(ifnull(rand.number, 0)) as rand_n
  from subject_info si
  left join flaky_test_counts flaky on flaky.flaky_type = 'flaky' and flaky.subject_name = si.name
  left join flaky_test_counts rand on rand.flaky_type = 'random' and rand.subject_name = si.name
  group by si.name
) i
where i.flaky_n > 0 or i.rand_n > 0