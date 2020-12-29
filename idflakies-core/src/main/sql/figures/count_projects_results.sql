select count(distinct slug)
from
(
  select s.slug, count(*) as n
  from detection_round dr
  inner join subject s on dr.subject_name = s.name
  group by s.slug
) i
where i.n > 0