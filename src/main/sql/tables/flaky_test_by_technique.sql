select *
from
(
    select i.slug,
           s.o_n,
           s.r_n,
           i.no_original,
           /* s.rc_n, */
           i.od_random_class,
           i.no_random_class,
           i.od_random,
           i.no_random,
           /* s.revc_n, */
           i.od_rev_class,
           i.no_rev_class,
           /* s.rev_n, */
           i.od_rev,
           i.no_rev,
           /* s.all_n, */
           ifnull(rcount.n, 0) as od_n,
           ifnull(fcount.n, 0) as no_n,
           ifnull(fcount.n, 0) + ifnull(rcount.n, 0) as all_n
    from
    (
        select s.slug,
               sum(ifnull(f.original_found, 0)) as no_original,
               sum(ifnull(f.rand_found, 0)) as no_random,
               sum(ifnull(r.rand_found, 0)) as od_random,
               sum(ifnull(f.rand_class_found, 0)) as no_random_class,
               sum(ifnull(r.rand_class_found, 0)) as od_random_class,
               sum(ifnull(f.rev_found, 0)) as no_rev,
               sum(ifnull(r.rev_found, 0)) as od_rev,
               sum(ifnull(f.rev_class_found, 0)) as no_rev_class,
               sum(ifnull(r.rev_class_found, 0)) as od_rev_class
        from flaky_test_classification ftc
        inner join subject s on ftc.subject_name = s.name
        left join
        (
          select ftc.subject_name, ftc.test_name, ftc.flaky_type,
               case when flaky.test_name is null then 0 else 1 end as original_found,
               case when rand.test_name is null then 0 else 1 end as rand_found,
               case when rand_class.test_name is null then 0 else 1 end as rand_class_found,
               case when rev.test_name is null then 0 else 1 end as rev_found,
               case when rev_class.test_name is null then 0 else 1 end as rev_class_found
          from flaky_test_classification ftc
          left join flaky_test_failures flaky on ftc.test_name = flaky.test_name and flaky.round_type = 'original'
          left join flaky_test_failures rand on ftc.test_name = rand.test_name and rand.round_type = 'random'
          left join flaky_test_failures rand_class on ftc.test_name = rand_class.test_name and rand_class.round_type = 'random-class'
          left join flaky_test_failures rev on ftc.test_name = rev.test_name and rev.round_type = 'reverse'
          left join flaky_test_failures rev_class on ftc.test_name = rev_class.test_name and rev_class.round_type = 'reverse-class'
        ) f on f.subject_name = ftc.subject_name and f.test_name = ftc.test_name and f.flaky_type = 'NO'
        left join
        (
          select ftc.subject_name, ftc.test_name, ftc.flaky_type,
               case when flaky.test_name is null then 0 else 1 end as original_found,
               case when rand.test_name is null then 0 else 1 end as rand_found,
               case when rand_class.test_name is null then 0 else 1 end as rand_class_found,
               case when rev.test_name is null then 0 else 1 end as rev_found,
               case when rev_class.test_name is null then 0 else 1 end as rev_class_found
          from flaky_test_classification ftc
          left join flaky_test_failures flaky on ftc.test_name = flaky.test_name and flaky.round_type = 'original'
          left join flaky_test_failures rand on ftc.test_name = rand.test_name and rand.round_type = 'random'
          left join flaky_test_failures rand_class on ftc.test_name = rand_class.test_name and rand_class.round_type = 'random-class'
          left join flaky_test_failures rev on ftc.test_name = rev.test_name and rev.round_type = 'reverse'
          left join flaky_test_failures rev_class on ftc.test_name = rev_class.test_name and rev_class.round_type = 'reverse-class'
        ) r on r.subject_name = ftc.subject_name and r.test_name = ftc.test_name and r.flaky_type = 'OD'
        group by s.slug
    ) i
    left join
    (
        select slug, flaky_type, sum(number) as n
        from flaky_test_counts ftc
        inner join subject s on s.name = ftc.subject_name
        group by slug, flaky_type
    ) fcount on lower(fcount.slug) = lower(i.slug) and fcount.flaky_type = 'NO'
    left join
    (
        select slug, flaky_type, sum(number) as n
        from flaky_test_counts ftc
        inner join subject s on s.name = ftc.subject_name
        group by slug, flaky_type
    ) rcount on lower(rcount.slug) = lower(i.slug) and rcount.flaky_type = 'OD'
    inner join
    (
        select s.slug, sum(o.number) as o_n, sum(r.number) as r_n, sum(rc.number) as rc_n, sum(rev.number) as rev_n, sum(revc.number) as revc_n
        from subject s
        inner join num_rounds o on o.name = s.name and o.round_type = 'original'
        inner join num_rounds r on r.name = s.name and r.round_type = 'random'
        inner join num_rounds rc on rc.name = s.name and rc.round_type = 'random-class'
        inner join num_rounds rev on rev.name = s.name and rev.round_type = 'reverse'
        inner join num_rounds revc on revc.name = s.name and revc.round_type = 'reverse-class'
        group by s.slug
        having sum(o.number) > 0 and sum(r.number) > 0 and sum(rc.number) > 0 and sum(rev.number) > 0 and sum(revc.number) > 0
    ) s on i.slug = s.slug
    order by i.slug
) t
union
select 'z-total',
       sum(s.o_n),
       sum(s.r_n),
       sum(i.no_original),
       /* s.rc_n, */
       sum(i.od_random_class),
       sum(i.no_random_class),
       sum(i.od_random),
       sum(i.no_random),
       /* s.revc_n, */
       sum(i.od_rev_class),
       sum(i.no_rev_class),
       /* s.rev_n, */
       sum(i.od_rev),
       sum(i.no_rev),
       /* s.all_n, */
	   sum(ifnull(rcount.n, 0)) as od_n,
	   sum(ifnull(fcount.n, 0)) as no_n,
	   sum(ifnull(fcount.n, 0) + ifnull(rcount.n, 0)) as all_n
from
(
	select s.slug,
		   sum(ifnull(f.original_found, 0)) as no_original,
		   sum(ifnull(f.rand_found, 0)) as no_random,
		   sum(ifnull(r.rand_found, 0)) as od_random,
		   sum(ifnull(f.rand_class_found, 0)) as no_random_class,
		   sum(ifnull(r.rand_class_found, 0)) as od_random_class,
		   sum(ifnull(f.rev_found, 0)) as no_rev,
		   sum(ifnull(r.rev_found, 0)) as od_rev,
		   sum(ifnull(f.rev_class_found, 0)) as no_rev_class,
		   sum(ifnull(r.rev_class_found, 0)) as od_rev_class
	from flaky_test_classification ftc
	inner join subject s on ftc.subject_name = s.name
	left join
	(
	  select ftc.subject_name, ftc.test_name, ftc.flaky_type,
		   case when flaky.test_name is null then 0 else 1 end as original_found,
		   case when rand.test_name is null then 0 else 1 end as rand_found,
		   case when rand_class.test_name is null then 0 else 1 end as rand_class_found,
		   case when rev.test_name is null then 0 else 1 end as rev_found,
		   case when rev_class.test_name is null then 0 else 1 end as rev_class_found
	  from flaky_test_classification ftc
	  left join flaky_test_failures flaky on ftc.test_name = flaky.test_name and flaky.round_type = 'original'
	  left join flaky_test_failures rand on ftc.test_name = rand.test_name and rand.round_type = 'random'
	  left join flaky_test_failures rand_class on ftc.test_name = rand_class.test_name and rand_class.round_type = 'random-class'
	  left join flaky_test_failures rev on ftc.test_name = rev.test_name and rev.round_type = 'reverse'
	  left join flaky_test_failures rev_class on ftc.test_name = rev_class.test_name and rev_class.round_type = 'reverse-class'
	) f on f.subject_name = ftc.subject_name and f.test_name = ftc.test_name and f.flaky_type = 'NO'
	left join
	(
	  select ftc.subject_name, ftc.test_name, ftc.flaky_type,
		   case when flaky.test_name is null then 0 else 1 end as original_found,
		   case when rand.test_name is null then 0 else 1 end as rand_found,
		   case when rand_class.test_name is null then 0 else 1 end as rand_class_found,
		   case when rev.test_name is null then 0 else 1 end as rev_found,
		   case when rev_class.test_name is null then 0 else 1 end as rev_class_found
	  from flaky_test_classification ftc
	  left join flaky_test_failures flaky on ftc.test_name = flaky.test_name and flaky.round_type = 'original'
	  left join flaky_test_failures rand on ftc.test_name = rand.test_name and rand.round_type = 'random'
	  left join flaky_test_failures rand_class on ftc.test_name = rand_class.test_name and rand_class.round_type = 'random-class'
	  left join flaky_test_failures rev on ftc.test_name = rev.test_name and rev.round_type = 'reverse'
	  left join flaky_test_failures rev_class on ftc.test_name = rev_class.test_name and rev_class.round_type = 'reverse-class'
	) r on r.subject_name = ftc.subject_name and r.test_name = ftc.test_name and r.flaky_type = 'OD'
	group by s.slug
) i
left join
(
	select slug, flaky_type, sum(number) as n
	from flaky_test_counts ftc
	inner join subject s on s.name = ftc.subject_name
	group by slug, flaky_type
) fcount on lower(fcount.slug) = lower(i.slug) and fcount.flaky_type = 'NO'
left join
(
	select slug, flaky_type, sum(number) as n
	from flaky_test_counts ftc
	inner join subject s on s.name = ftc.subject_name
	group by slug, flaky_type
) rcount on lower(rcount.slug) = lower(i.slug) and rcount.flaky_type = 'OD'
inner join
(
    select s.slug, sum(o.number) as o_n, sum(r.number) as r_n, sum(rc.number) as rc_n, sum(rev.number) as rev_n, sum(revc.number) as revc_n
    from subject s
    inner join num_rounds o on o.name = s.name and o.round_type = 'original'
    inner join num_rounds r on r.name = s.name and r.round_type = 'random'
    inner join num_rounds rc on rc.name = s.name and rc.round_type = 'random-class'
    inner join num_rounds rev on rev.name = s.name and rev.round_type = 'reverse'
    inner join num_rounds revc on revc.name = s.name and revc.round_type = 'reverse-class'
    group by s.slug
    having sum(o.number) > 0 and sum(r.number) > 0 and sum(rc.number) > 0 and sum(rev.number) > 0 and sum(revc.number) > 0
) s on i.slug = s.slug;

