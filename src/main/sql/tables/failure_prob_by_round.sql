select s.slug, round(avg(f.perc_fail), 1), round(avg(r.perc_fail), 1), round(avg(a.perc_fail), 1)
from
(
	select i.subject_name, round(100 * cast(i.failed_rounds as float) / rounds, 1) as perc_fail
	from
	(
		select dr.subject_name,
			   sum(case when flaky_found > 0 or random_found > 0 then 1 else 0 end) as failed_rounds,
			   count(*) as rounds
		from detection_round_failures drf
		inner join detection_round dr on dr.id = drf.detection_round_id
		left join flaky_test_counts f on dr.subject_name = f.subject_name and f.flaky_type = 'flaky'
		left join flaky_test_counts r on dr.subject_name = r.subject_name and r.flaky_type = 'random'
		where f.number > 0 or r.number > 0
		group by dr.subject_name
	) i
) a
inner join
(
	select i.subject_name, round(100 * cast(i.failed_rounds as float) / rounds, 1) as perc_fail
	from
	(
		select dr.subject_name,
			   sum(case when flaky_found > 0 then 1 else 0 end) as failed_rounds,
			   count(*) as rounds
		from detection_round_failures drf
		inner join detection_round dr on dr.id = drf.detection_round_id
		left join flaky_test_counts f on dr.subject_name = f.subject_name and f.flaky_type = 'flaky'
		left join flaky_test_counts r on dr.subject_name = r.subject_name and r.flaky_type = 'random'
		where f.number > 0 or r.number > 0
		group by dr.subject_name
	) i
) f on f.subject_name = a.subject_name
inner join
(
	select i.subject_name, round(100 * cast(i.failed_rounds as float) / rounds, 1) as perc_fail
	from
	(
		select dr.subject_name,
			   sum(case when random_found > 0 then 1 else 0 end) as failed_rounds,
			   count(*) as rounds
		from detection_round_failures drf
		inner join detection_round dr on dr.id = drf.detection_round_id
		left join flaky_test_counts f on dr.subject_name = f.subject_name and f.flaky_type = 'flaky'
		left join flaky_test_counts r on dr.subject_name = r.subject_name and r.flaky_type = 'random'
		where f.number > 0 or r.number > 0
		group by dr.subject_name
	) i
) r on r.subject_name = a.subject_name
inner join subject s on a.subject_name = s.name
group by s.slug