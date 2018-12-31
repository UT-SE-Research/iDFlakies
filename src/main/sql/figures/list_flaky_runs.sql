select trr.str_id, tr.order_index, tr.name, tr.result
from test_run_result trr
inner join detection_round_test_runs drtr on drtr.test_run_result_id = trr.str_id
inner join detection_round dr on dr.id = drtr.detection_round_id and dr.round_type = 'flaky'
inner join test_result tr on trr.str_id = tr.test_run_result_str_id
-- NOTE: If you change :name, then you will also need to change scripts/list_flaky_runs.sh
where trr.subject_name like '%:name%'
order by trr.str_id, order_index;
