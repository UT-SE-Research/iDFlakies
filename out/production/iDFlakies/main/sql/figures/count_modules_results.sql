select count(distinct subject_name)
from
(
  select subject_name, count(*) as n
  from detection_round
  group by subject_name
) i
where i.n > 0