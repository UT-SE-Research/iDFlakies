select s.slug, round(avg(nonorder.perc_fail), 1), round(avg(orderdep.perc_fail), 1), round(avg(a.perc_fail), 1)
from
(
	select i.subject_name, round(100 * cast(i.failed_rounds as float) / rounds, 1) as perc_fail
	from
	(
		select dr.subject_name,
			   sum(case when no_found > 0 or od_found > 0 then 1 else 0 end) as failed_rounds,
			   count(*) as rounds
		from detection_round_failures drf
		inner join detection_round dr on dr.id = drf.detection_round_id
		left join flaky_test_counts f on dr.subject_name = f.subject_name and f.flaky_type = 'NO'
		left join flaky_test_counts r on dr.subject_name = r.subject_name and r.flaky_type = 'OD'
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
			   sum(case when no_found > 0 then 1 else 0 end) as failed_rounds,
			   count(*) as rounds
		from detection_round_failures drf
		inner join detection_round dr on dr.id = drf.detection_round_id
		left join flaky_test_counts f on dr.subject_name = f.subject_name and f.flaky_type = 'NO'
		left join flaky_test_counts r on dr.subject_name = r.subject_name and r.flaky_type = 'OD'
		where f.number > 0 or r.number > 0
		group by dr.subject_name
	) i
) nonorder on nonorder.subject_name = a.subject_name
inner join
(
	select i.subject_name, round(100 * cast(i.failed_rounds as float) / rounds, 1) as perc_fail
	from
	(
		select dr.subject_name,
			   sum(case when od_found > 0 then 1 else 0 end) as failed_rounds,
			   count(*) as rounds
		from detection_round_failures drf
		inner join detection_round dr on dr.id = drf.detection_round_id
		left join flaky_test_counts f on dr.subject_name = f.subject_name and f.flaky_type = 'NO'
		left join flaky_test_counts r on dr.subject_name = r.subject_name and r.flaky_type = 'OD'
		where f.number > 0 or r.number > 0
		group by dr.subject_name
	) i
) orderdep on orderdep.subject_name = a.subject_name
inner join subject s on a.subject_name = s.name
group by s.slug