select round(max(100.0 * cast(with_failures as float) / t), 1)
from
(
  select sum(case when no_found > 0 then 1 else 0 end) as with_failures, count(*) as t
  from detection_round_failures drf
  inner join detection_round dr on drf.detection_round_id = dr.id
  left join flaky_test_counts f on dr.subject_name = f.subject_name and f.flaky_type = 'NO'
  left join flaky_test_counts r on dr.subject_name = r.subject_name and r.flaky_type = 'OD'
  where f.number > 0 or r.number > 0 -- Only for projects that have any flaky tests
) i;

