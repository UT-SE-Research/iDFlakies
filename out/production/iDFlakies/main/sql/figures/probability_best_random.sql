select round(avg(100.0 * cast(i.same_best_no as float) / i.t), 1)
from
(
	select dr.subject_name,
		   sum(case when drf.no_found = i.best_no then 1 else 0 end) as same_best_no,
		   sum(case when drf.od_found = i.best_od then 1 else 0 end) as same_best_od,
		   count(*) t
	from detection_round_failures drf
	inner join detection_round dr on drf.detection_round_id = dr.id
	inner join
	(
		select dr.subject_name, max(no_found) as best_no, max(od_found) as best_od
		from detection_round_failures drf
		inner join detection_round dr on drf.detection_round_id = dr.id
		group by dr.subject_name
	) i on dr.subject_name = i.subject_name
	where i.best_od > 0 and dr.round_type <> 'original'
	group by dr.subject_name
) i
