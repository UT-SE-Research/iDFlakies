select count(distinct i.slug)
from
(
  select s.slug,
         sum(ifnull(nonorder.number, 0)) as no_n,
         sum(ifnull(orderdep.number, 0)) as od_n
    from subject_info si
  inner join subject s on si.name = s.name
  left join flaky_test_counts nonorder on nonorder.flaky_type = 'NO' and nonorder.subject_name = s.name
  left join flaky_test_counts orderdep on orderdep.flaky_type = 'OD' and orderdep.subject_name = s.name
  group by s.slug
) i
where i.no_n > 0 or i.od_n > 0