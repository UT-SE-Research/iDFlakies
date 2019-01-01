select s.slug,
	   round(avg(ifnull(nonorder.prob_failure_per_test, 0)), 1) as no_prob,
	   round(avg(ifnull(orderdep.prob_failure_per_test, 0)), 1) as od_prob,
	   round(avg(a.prob_failure_per_test), 1) as all_prob
from
(
	select i.subject_name, round(avg(100 * cast(i.failures as float) / nr.rounds), 1) as prob_failure_per_test
	from
	(
		select subject_name, test_name, sum(failures) as failures
		from flaky_test_failures
		group by subject_name, test_name
	) i
	inner join
	(
		select name as subject_name, sum(number) as rounds
		from num_rounds
		group by name
	) nr on i.subject_name = nr.subject_name
	group by i.subject_name
) a
left join
(
	select i.subject_name, round(avg(100 * cast(i.failures as float) / nr.rounds), 1) as prob_failure_per_test
	from
	(
		select subject_name, test_name, sum(failures) as failures
		from flaky_test_failures
		where flaky_type = 'OD'
		group by subject_name, test_name
	) i
	inner join
	(
		select name as subject_name, sum(number) as rounds
		from num_rounds
		group by name
	) nr on i.subject_name = nr.subject_name
	group by i.subject_name
) orderdep on orderdep.subject_name = a.subject_name
left join
(
	select i.subject_name, round(avg(100 * cast(i.failures as float) / nr.rounds), 1) as prob_failure_per_test
	from
	(
		select subject_name, test_name, sum(failures) as failures
		from flaky_test_failures
		where flaky_type = 'NO'
		group by subject_name, test_name
	) i
	inner join
	(
		select name as subject_name, sum(number) as rounds
		from num_rounds
		group by name
	) nr on i.subject_name = nr.subject_name
	group by i.subject_name
) nonorder on nonorder.subject_name = a.subject_name
inner join subject s on s.name = a.subject_name
group by s.slug