select i.slug, i.sha, i.loc, i.test_loc, i.test_count-- , i.od_n, i.no_n
from
(
  select s.slug, substr(sr.sha, 1, 8) as sha, sr.loc, sr.test_loc,
       sum(si.test_count) as test_count,
       sum(ifnull(nonorder.number, 0)) as no_n,
       sum(ifnull(orderdep.number, 0)) as od_n
  from subject_info si
  inner join subject s on si.name = s.name
  inner join subject_raw sr on s.slug = sr.slug
  left join flaky_test_counts nonorder on nonorder.flaky_type = 'NO' and nonorder.subject_name = s.name
  left join flaky_test_counts orderdep on orderdep.flaky_type = 'OD' and orderdep.subject_name = s.name
  group by s.slug, sr.sha
) i
where no_n > 0 or od_n > 0