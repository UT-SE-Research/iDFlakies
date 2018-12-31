select count(distinct i.slug)
from
(
  select s.slug,
         sum(ifnull(flaky.number, 0)) as flaky_n,
         sum(ifnull(rand.number, 0)) as rand_n
    from subject_info si
  inner join subject s on si.name = s.name
  left join flaky_test_counts flaky on flaky.flaky_type = 'flaky' and flaky.subject_name = s.name
  left join flaky_test_counts rand on rand.flaky_type = 'random' and rand.subject_name = s.name
  group by s.slug
) i
where i.flaky_n > 0 or i.rand_n > 0