select round(avg(100.0 * cast(i.same_best_flaky as float) / i.t), 1)
from
(
	select dr.subject_name,
		   sum(case when drf.flaky_found = i.best_flaky then 1 else 0 end) as same_best_flaky,
		   sum(case when drf.random_found = i.best_random then 1 else 0 end) as same_best_random,
		   count(*) t
	from detection_round_failures drf
	inner join detection_round dr on drf.detection_round_id = dr.id
	inner join
	(
		select dr.subject_name, max(flaky_found) as best_flaky, max(random_found) as best_random
		from detection_round_failures drf
		inner join detection_round dr on drf.detection_round_id = dr.id
		group by dr.subject_name
	) i on dr.subject_name = i.subject_name
	where i.best_flaky > 0
	group by dr.subject_name
) i