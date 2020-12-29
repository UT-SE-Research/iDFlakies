select *
from
(
    select info.subject_name, sum(case when test_count = passes then 1 else 0 end) all_passing
    from
    (
        select trr.subject_name, trr.str_id, trr.test_count, sum(case when tr.result = 'PASS' or tr.result = 'SKIPPED' then 1 else 0 end) passes
        from test_run_result trr
        inner join subject_info si on trr.subject_name = si.name and trr.test_count = si.test_count
        inner join detection_round_test_runs drtr on drtr.test_run_result_id = trr.str_id
        inner join detection_round dr on dr.id = drtr.detection_round_id and dr.round_type = 'original'
        inner join test_result tr on trr.str_id = tr.test_run_result_str_id
        group by trr.subject_name, trr.str_id
    ) info
    group by info.subject_name
) info
left join flaky_test_counts ftc on info.subject_name = ftc.subject_name
where all_passing = 0

