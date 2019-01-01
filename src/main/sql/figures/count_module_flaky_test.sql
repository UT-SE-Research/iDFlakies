select count(distinct i.name)
from
(
  select si.name,
         sum(ifnull(nonorder.number, 0)) as flaky_n,
         sum(ifnull(orderdep.number, 0)) as rand_n
  from subject_info si
  left join flaky_test_counts nonorder on nonorder.flaky_type = 'NO' and nonorder.subject_name = si.name
  left join flaky_test_counts orderdep on orderdep.flaky_type = 'OD' and orderdep.subject_name = si.name
  group by si.name
) i
where i.flaky_n > 0 or i.rand_n > 0