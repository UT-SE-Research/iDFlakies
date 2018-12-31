select s.slug,
	   round(avg(ifnull(flaky.prob_failure_per_test, 0)), 1) as flaky_prob,
	   round(avg(ifnull(rand.prob_failure_per_test, 0)), 1) as rand_prob,
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
		where flaky_type = 'random'
		group by subject_name, test_name
	) i
	inner join
	(
		select name as subject_name, sum(number) as rounds
		from num_rounds
		group by name
	) nr on i.subject_name = nr.subject_name
	group by i.subject_name
) rand on rand.subject_name = a.subject_name
left join
(
	select i.subject_name, round(avg(100 * cast(i.failures as float) / nr.rounds), 1) as prob_failure_per_test
	from
	(
		select subject_name, test_name, sum(failures) as failures
		from flaky_test_failures
		where flaky_type = 'flaky'
		group by subject_name, test_name
	) i
	inner join
	(
		select name as subject_name, sum(number) as rounds
		from num_rounds
		group by name
	) nr on i.subject_name = nr.subject_name
	group by i.subject_name
) flaky on flaky.subject_name = a.subject_name
inner join subject s on s.name = a.subject_name
group by s.slug