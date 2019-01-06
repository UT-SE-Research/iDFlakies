select sum(case when det_a.test_name is null and det_b.test_name is null then 1 else 0 end) as neither,
       sum(case when det_a.test_name is not null and det_b.test_name is not null then 1 else 0 end) as both,
       sum(case when det_a.test_name is not null and det_b.test_name is null then 1 else 0 end) as just_a,
       sum(case when det_a.test_name is null and det_b.test_name is not null then 1 else 0 end) as just_b,
       count(*) as total
from flaky_test_classification ftc
left join
(
    select test_name
    from flaky_test_failures
    where round_type = 'random'
) det_a on det_a.test_name = ftc.test_name
left join
(
    select test_name
    from flaky_test_failures
    where round_type = 'reverse'
) det_b on det_b.test_name = ftc.test_name
where ftc.flaky_type = 'OD';

