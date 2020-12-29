select count(distinct test_name)
from flaky_test_failures ftf
inner join num_rounds o on o.name = ftf.subject_name and o.round_type = 'original'
inner join num_rounds r on r.name = ftf.subject_name and r.round_type = 'random'
inner join num_rounds rc on rc.name = ftf.subject_name and rc.round_type = 'random-class'
inner join num_rounds rev on rev.name = ftf.subject_name and rev.round_type = 'reverse'
inner join num_rounds revc on revc.name = ftf.subject_name and revc.round_type = 'reverse-class'
where ftf.flaky_type like ? and ftf.round_type = ? and o.number > 0 and r.number > 0 and rc.number > 0 and rev.number > 0 and revc.number > 0;

