select round(avg(100 * i.perc), 1)
from
(
  select distinct ti.test_name, ftc.flaky_type, cast(ti.failures as float) / rounds as perc, ti.failures, si.rounds
  from
  (
    select fti.subject_name, test_name, count(*) as failures
    from flaky_test_info fti
    inner join detection_round dr on fti.detection_round_id = dr.id
    where dr.round_type <> 'original'
    group by fti.subject_name, test_name
  ) ti
  inner join
  (
    select dr.subject_name, count(*) as rounds
    from detection_round dr
    where dr.round_type <> 'original'
    group by dr.subject_name
  ) si on ti.subject_name = si.subject_name
  inner join flaky_test_classification ftc on ti.test_name = ftc.test_name
  where si.rounds > ?
  order by perc
) i
